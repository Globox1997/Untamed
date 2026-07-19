package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.untamed.init.EntityInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.UUID;

public class LionessEntity extends Animal implements NeutralMob {
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING_ID = SynchedEntityData.defineId(LionessEntity.class, EntityDataSerializers.BOOLEAN);
    private int warningSoundTicks;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    private int ticksUntilNextSleepCheck;
    private int sleepTicksRemaining;

    public LionessEntity(EntityType<? extends LionessEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return EntityInit.LIONESS.create(serverLevel);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.LIONESS_FOOD);
    }

    @Override
    public boolean canMate(Animal animal) {
        if (!(animal instanceof LionEntity lion)) {
            return false;
        }

        return this.isInLove() && lion.isInLove();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LionessEntity.LionessEntityMeleeAttackGoal());
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0, pathfinderMob -> pathfinderMob.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new LionessBreedGoal(this, 1.0, LionEntity.class));
        this.goalSelector.addGoal(2, new LionessEntity.LionSleepGoal());
        this.goalSelector.addGoal(3, new LionessEntity.BabySleepNearParentGoal());
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.85));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new LionessEntity.LionessEntityHurtByTargetGoal());
        this.targetSelector.addGoal(2, new LionessEntity.LionessEntityAttackPlayersGoal());
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
//        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Fox.class, 10, true, true, null));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 26.0).add(Attributes.FOLLOW_RANGE, 25.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    public static boolean checkLionessEntitySpawnRules(EntityType<LionessEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.LIONS_SPAWNABLE_ON);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.readPersistentAngerSaveData(this.level(), compoundTag);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        this.addPersistentAngerSaveData(compoundTag);
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public void setRemainingPersistentAngerTime(int i) {
        this.remainingPersistentAngerTime = i;
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID uUID) {
        this.persistentAngerTarget = uUID;
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? SoundEvents.POLAR_BEAR_AMBIENT_BABY : SoundEvents.POLAR_BEAR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.POLAR_BEAR_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundEvents.POLAR_BEAR_STEP, 0.15F, 1.0F);
    }

    protected void playWarningSound() {
        if (this.warningSoundTicks <= 0) {
            this.makeSound(SoundEvents.POLAR_BEAR_WARNING);
            this.warningSoundTicks = 40;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLEEPING_ID, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.warningSoundTicks > 0) {
            this.warningSoundTicks--;
        }

        if (!this.level().isClientSide()) {
            this.updatePersistentAnger((ServerLevel) this.level(), true);

            if (this.isSleeping()) {
                if (this.sleepTicksRemaining > 0) {
                    this.sleepTicksRemaining--;
                } else {
                    this.setSleeping(false);
                }
            }

            if (this.ticksUntilNextSleepCheck > 0) {
                this.ticksUntilNextSleepCheck--;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource damageSource, float f) {
        boolean result = super.hurt(damageSource, f);
        if (this.isSleeping()) {
            this.setSleeping(false);
        }
        return result;
    }

    public boolean isSleeping() {
        return this.entityData.get(DATA_SLEEPING_ID);
    }

    public void setSleeping(boolean bl) {
        this.entityData.set(DATA_SLEEPING_ID, bl);
        if (bl) {
            this.sleepTicksRemaining = 300 + this.random.nextInt(600);
        } else {
            this.sleepTicksRemaining = 0;
        }
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            spawnGroupData = new AgeableMob.AgeableMobGroupData(1.0F);
        }

        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
    }

    private class LionSleepGoal extends Goal {
        public LionSleepGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (LionessEntity.this.isBaby()) {
                return false;
            }
            if (LionessEntity.this.getTarget() != null || LionessEntity.this.isAngry()) {
                return false;
            }
            if (LionessEntity.this.isSleeping()) {
                return true;
            }
            if (LionessEntity.this.ticksUntilNextSleepCheck > 0) {
                return false;
            }

            LionessEntity.this.ticksUntilNextSleepCheck = 100;

            boolean isNight = LionessEntity.this.level().isNight();
            int chance = isNight ? 60 : 400;

            if (LionessEntity.this.random.nextInt(chance) == 0) {
                LionessEntity.this.setSleeping(true);
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return LionessEntity.this.isSleeping()
                    && LionessEntity.this.getTarget() == null
                    && !LionessEntity.this.isAngry();
        }

        @Override
        public void start() {
            LionessEntity.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            LionessEntity.this.setSleeping(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LionessEntity.this.getNavigation().stop();
            LionessEntity.this.setXxa(0);
            LionessEntity.this.setZza(0);
            LionessEntity.this.getMoveControl().setWantedPosition(LionessEntity.this.getX(), LionessEntity.this.getY(), LionessEntity.this.getZ(), 0);
        }
    }

    private class BabySleepNearParentGoal extends Goal {
        private LionessEntity sleepingParent;

        public BabySleepNearParentGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!LionessEntity.this.isBaby()) {
                return false;
            }
            if (LionessEntity.this.isSleeping()) {
                return true;
            }

            LionessEntity nearestSleeping = LionessEntity.this.level()
                    .getEntitiesOfClass(LionessEntity.class, LionessEntity.this.getBoundingBox().inflate(10.0, 4.0, 10.0))
                    .stream()
                    .filter(lion -> !lion.isBaby() && lion.isSleeping())
                    .min(Comparator.comparingDouble(LionessEntity.this::distanceToSqr))
                    .orElse(null);

            if (nearestSleeping != null) {
                this.sleepingParent = nearestSleeping;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.sleepingParent != null
                    && this.sleepingParent.isSleeping()
                    && LionessEntity.this.distanceToSqr(this.sleepingParent) < 144.0;
        }

        @Override
        public void start() {
            if (LionessEntity.this.distanceToSqr(this.sleepingParent) > 4.0) {
                LionessEntity.this.getNavigation().moveTo(this.sleepingParent, 1.0);
            } else {
                LionessEntity.this.setSleeping(true);
            }
        }

        @Override
        public void tick() {
            if (LionessEntity.this.distanceToSqr(this.sleepingParent) > 4.0) {
                if (LionessEntity.this.getNavigation().isDone()) {
                    LionessEntity.this.getNavigation().moveTo(this.sleepingParent, 1.0);
                }
            } else {
                LionessEntity.this.getNavigation().stop();
                LionessEntity.this.setSleeping(true);
            }
        }

        @Override
        public void stop() {
            LionessEntity.this.setSleeping(false);
            this.sleepingParent = null;
        }
    }

    private class LionessEntityAttackPlayersGoal extends NearestAttackableTargetGoal<Player> {

        public LionessEntityAttackPlayersGoal() {
            super(LionessEntity.this, Player.class, 20, true, true, null);
        }

        @Override
        public boolean canUse() {
            if (LionessEntity.this.isBaby()) {
                return false;
            }

            if (super.canUse()) {
                for (LionessEntity lionEntity : LionessEntity.this.level().getEntitiesOfClass(LionessEntity.class, LionessEntity.this.getBoundingBox().inflate(8.0, 4.0, 8.0))) {
                    if (lionEntity.isBaby()) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        protected double getFollowDistance() {
            return super.getFollowDistance() * 0.5;
        }
    }

    private class LionessEntityHurtByTargetGoal extends HurtByTargetGoal {

        public LionessEntityHurtByTargetGoal() {
            super(LionessEntity.this);
        }

        @Override
        public void start() {
            super.start();
            if (LionessEntity.this.isBaby()) {
                this.alertOthers();
                this.stop();
            }
        }

        @Override
        protected void alertOther(Mob mob, LivingEntity livingEntity) {
            if (mob instanceof LionessEntity && !mob.isBaby()) {
                super.alertOther(mob, livingEntity);
            }
        }
    }

    private class LionessEntityMeleeAttackGoal extends MeleeAttackGoal {

        public LionessEntityMeleeAttackGoal() {
            super(LionessEntity.this, 1.25, true);
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity livingEntity) {
            if (this.canPerformAttack(livingEntity)) {
                this.resetAttackCooldown();
                this.mob.doHurtTarget(livingEntity);
            } else if (this.mob.distanceToSqr(livingEntity) < (livingEntity.getBbWidth() + 3.0F) * (livingEntity.getBbWidth() + 3.0F)) {
                if (this.isTimeToAttack()) {
                    this.resetAttackCooldown();
                }

                if (this.getTicksUntilNextAttack() <= 10) {
                    LionessEntity.this.playWarningSound();
                }
            } else {
                this.resetAttackCooldown();
            }
        }
    }

    private class LionessBreedGoal extends BreedGoal {

        public LionessBreedGoal(Animal animal, double d, Class<? extends Animal> class_) {
            super(animal, d, class_);
        }

    }
}