package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
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

import java.util.UUID;

public class HyenaEntity extends Animal implements NeutralMob {
    private int warningSoundTicks;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    public HyenaEntity(EntityType<? extends HyenaEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return EntityInit.HYENA.create(serverLevel);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.HYENA_FOOD);
    }

    @Override
    public boolean canMate(Animal animal) {
        if (!(animal instanceof HyenaEntity hyenaEntity)) {
            return false;
        }
        return this.isInLove() && hyenaEntity.isInLove();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HyenaEntity.HyenaEntityMeleeAttackGoal());
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0, pathfinderMob -> pathfinderMob.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HyenaEntity.HyenaEntityHurtByTargetGoal());
        this.targetSelector.addGoal(2, new HyenaEntity.HyenaEntityAttackPlayersGoal());
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 14, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.FOLLOW_RANGE, 20.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    public static boolean checkHyenaEntitySpawnRules(EntityType<HyenaEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.HYENAS_SPAWNABLE_ON);
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
    }

    @Override
    public void tick() {
        super.tick();

        if (this.warningSoundTicks > 0) {
            this.warningSoundTicks--;
        }

        if (!this.level().isClientSide()) {
            this.updatePersistentAnger((ServerLevel) this.level(), true);
        }
    }


    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            spawnGroupData = new AgeableMobGroupData(1.0F);
        }

        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
    }

    private class HyenaEntityAttackPlayersGoal extends NearestAttackableTargetGoal<Player> {

        public HyenaEntityAttackPlayersGoal() {
            super(HyenaEntity.this, Player.class, 20, true, true, null);
        }

        @Override
        public boolean canUse() {
            if (HyenaEntity.this.isBaby()) {
                return false;
            }

            if (super.canUse()) {
                for (HyenaEntity hyenaEntity : HyenaEntity.this.level().getEntitiesOfClass(HyenaEntity.class, HyenaEntity.this.getBoundingBox().inflate(8.0, 4.0, 8.0))) {
                    if (hyenaEntity.isBaby()) {
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

    private class HyenaEntityHurtByTargetGoal extends HurtByTargetGoal {

        public HyenaEntityHurtByTargetGoal() {
            super(HyenaEntity.this);
        }

        @Override
        public void start() {
            super.start();
            if (HyenaEntity.this.isBaby()) {
                this.alertOthers();
                this.stop();
            }
        }

        @Override
        protected void alertOther(Mob mob, LivingEntity livingEntity) {
            if (mob instanceof HyenaEntity && !mob.isBaby()) {
                super.alertOther(mob, livingEntity);
            }
        }
    }

    private class HyenaEntityMeleeAttackGoal extends MeleeAttackGoal {

        public HyenaEntityMeleeAttackGoal() {
            super(HyenaEntity.this, 1.25, true);
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
                    HyenaEntity.this.playWarningSound();
                }
            } else {
                this.resetAttackCooldown();
            }
        }
    }

}
