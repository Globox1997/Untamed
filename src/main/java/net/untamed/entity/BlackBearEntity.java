package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.untamed.entity.ai.bear.*;
import net.untamed.init.EntityInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlackBearEntity extends Animal implements NeutralMob {

    private static final EntityDataAccessor<Byte> DATA_BEAR_POSE = SynchedEntityData.defineId(BlackBearEntity.class, EntityDataSerializers.BYTE);
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private static final int MAX_HUNGER = 48000;
    public static final int HUNGRY = 18000;
    private static final int MAX_TOLERANCE_ENTRIES = 8;

    private int warningSoundTicks;
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;
    private int alarmTicks;
    private int hunger;
    private final Map<UUID, Integer> playerTolerance = new HashMap<>();

    private float standAmount, standAmountO;
    private float restAmount, restAmountO;
    private float forageAmount, forageAmountO;
    private float eatAmount, eatAmountO;
    private float huffAmount, huffAmountO;
    private float rubAmount, rubAmountO;

    public BlackBearEntity(EntityType<? extends BlackBearEntity> entityType, Level level) {
        super(entityType, level);
    }

    public enum BearPose {
        NONE, STANDING, RESTING, FORAGING, EATING, HUFFING, RUBBING;

        private static final BearPose[] VALUES = values();

        public static BearPose byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return EntityInit.BLACK_BEAR.create(serverLevel);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.BLACK_BEAR_FOOD);
    }

    @Override
    public boolean canMate(Animal animal) {
        if (!(animal instanceof BlackBearEntity blackBearEntity)) {
            return false;
        }
        return this.isInLove() && blackBearEntity.isInLove();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BlackBearEntity.BlackBearEntityMeleeAttackGoal());
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0, pathfinderMob -> pathfinderMob.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new BearThreatResponseGoal(this));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(5, new BearDenGoal(this));
        this.goalSelector.addGoal(6, new RaidBeehiveGoal(this));
        this.goalSelector.addGoal(6, new EatBerriesGoal(this));
        this.goalSelector.addGoal(6, new BearScavengeGoal(this));
        this.goalSelector.addGoal(7, new TearLogGoal(this));
        this.goalSelector.addGoal(7, new RubTreeGoal(this));
        this.goalSelector.addGoal(8, new BearStrollGoal(this, 0.8));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new BlackBearEntity.BlackBearEntityHurtByTargetGoal());
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 14, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 34.0D).add(Attributes.FOLLOW_RANGE, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.ATTACK_DAMAGE, 7.0D);
    }

    public static boolean checkBlackBearEntitySpawnRules(EntityType<BlackBearEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_FOREST)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.BLACK_BEARS_SPAWNABLE_ON);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BEAR_POSE, (byte) BearPose.NONE.ordinal());
    }

    @Override
    public boolean canBeAffected(MobEffectInstance mobEffectInstance) {
        return !mobEffectInstance.is(MobEffects.POISON) && super.canBeAffected(mobEffectInstance);
    }

    public boolean isPeckish() {
        return this.hunger >= HUNGRY / 2;
    }

    public void feed(int amount) {
        this.hunger = Math.max(0, this.hunger - amount);
    }

    public boolean isSnowingHere() {
        BlockPos pos = this.blockPosition();
        return this.level().isRaining() && this.level().getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW;
    }

    public double scoreDenSpot(BlockPos pos) {
        Level level = this.level();
        double score = level.canSeeSky(pos.above()) ? -10.0D : 10.0D;
        for (BlockPos side : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
            if (level.getBlockState(side).is(BlockTags.LOGS) || level.getBlockState(side).is(BlockTags.LEAVES)) {
                score += 3.0D;
            }
        }
        return score;
    }

    public boolean isAlarmed() {
        return this.alarmTicks > 0;
    }

    public void alarm(int ticks) {
        this.alarmTicks = Math.max(this.alarmTicks, ticks);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.alarmTicks > 0) {
            this.alarmTicks--;
        }
        if (this.getTarget() != null) {
            this.alarm(20);
        }
        if (this.hunger < MAX_HUNGER) {
            this.hunger++;
        }
    }

    @Override
    public boolean hurt(DamageSource damageSource, float f) {
        boolean result = super.hurt(damageSource, f);
        if (result && !this.level().isClientSide() && !(damageSource.getEntity() instanceof Bee)) {
            this.alarm(200);
            this.clearBearPose(BearPose.RESTING);
            if (damageSource.getEntity() instanceof Player player) {
                this.playerTolerance.remove(player.getUUID());
            }
        }
        return result;
    }

    public int getTolerance(Player player) {
        return this.playerTolerance.getOrDefault(player.getUUID(), 0);
    }

    public void addTolerance(Player player, int amount, int max) {
        if (!this.playerTolerance.containsKey(player.getUUID()) && this.playerTolerance.size() >= MAX_TOLERANCE_ENTRIES) {
            this.playerTolerance.clear();
        }
        this.playerTolerance.merge(player.getUUID(), amount, (a, b) -> Math.min(max, a + b));
    }

    public void provokeBy(Player player) {
        this.setTarget(player);
        this.setPersistentAngerTarget(player.getUUID());
        this.startPersistentAngerTimer();
        this.playerTolerance.remove(player.getUUID());
    }

    public BearPose getBearPose() {
        return BearPose.byId(this.entityData.get(DATA_BEAR_POSE));
    }

    public void setBearPose(BearPose pose) {
        this.entityData.set(DATA_BEAR_POSE, (byte) pose.ordinal());
    }

    public void clearBearPose(BearPose pose) {
        if (this.getBearPose() == pose) {
            this.setBearPose(BearPose.NONE);
        }
    }

    public boolean isResting() {
        return this.getBearPose() == BearPose.RESTING;
    }

    public float getStandAmount(float partialTick) {
        return Mth.lerp(partialTick, this.standAmountO, this.standAmount);
    }

    public float getRestAmount(float partialTick) {
        return Mth.lerp(partialTick, this.restAmountO, this.restAmount);
    }

    public float getForageAmount(float partialTick) {
        return Mth.lerp(partialTick, this.forageAmountO, this.forageAmount);
    }

    public float getEatAmount(float partialTick) {
        return Mth.lerp(partialTick, this.eatAmountO, this.eatAmount);
    }

    public float getHuffAmount(float partialTick) {
        return Mth.lerp(partialTick, this.huffAmountO, this.huffAmount);
    }

    public float getRubAmount(float partialTick) {
        return Mth.lerp(partialTick, this.rubAmountO, this.rubAmount);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.warningSoundTicks > 0) {
            this.warningSoundTicks--;
        }

        if (this.level().isClientSide()) {
            BearPose pose = this.getBearPose();
            this.standAmountO = this.standAmount;
            this.restAmountO = this.restAmount;
            this.forageAmountO = this.forageAmount;
            this.eatAmountO = this.eatAmount;
            this.huffAmountO = this.huffAmount;
            this.rubAmountO = this.rubAmount;
            boolean standing = pose == BearPose.STANDING || pose == BearPose.RUBBING;
            this.standAmount = Mth.approach(this.standAmount, standing ? 1.0F : 0.0F, 0.1F);
            this.restAmount = Mth.approach(this.restAmount, pose == BearPose.RESTING ? 1.0F : 0.0F, 0.08F);
            this.forageAmount = Mth.approach(this.forageAmount, pose == BearPose.FORAGING ? 1.0F : 0.0F, 0.15F);
            this.eatAmount = Mth.approach(this.eatAmount, pose == BearPose.EATING ? 1.0F : 0.0F, 0.15F);
            this.huffAmount = Mth.approach(this.huffAmount, pose == BearPose.HUFFING ? 1.0F : 0.0F, 0.2F);
            this.rubAmount = Mth.approach(this.rubAmount, pose == BearPose.RUBBING ? 1.0F : 0.0F, 0.1F);
        } else {
            this.updatePersistentAnger((ServerLevel) this.level(), true);
        }
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
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.readPersistentAngerSaveData(this.level(), compoundTag);
        this.hunger = compoundTag.getInt("Hunger");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        this.addPersistentAngerSaveData(compoundTag);
        compoundTag.putInt("Hunger", this.hunger);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.BLACK_BEAR_IDLE_EVENT;
    }

    @Override
    public void playAmbientSound() {
        if (!this.isResting()) {
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.BLACK_BEAR_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.BLACK_BEAR_DEATH_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.BLACK_BEAR_STEP_EVENT, 0.15F, 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.7F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    public void playWarningSound() {
        if (this.warningSoundTicks <= 0) {
            this.makeSound(SoundInit.BLACK_BEAR_ATTACK_EVENT);
            this.warningSoundTicks = 40;
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
        this.hunger = this.random.nextInt(HUNGRY);
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
    }

    private class BlackBearEntityHurtByTargetGoal extends HurtByTargetGoal {

        public BlackBearEntityHurtByTargetGoal() {
            super(BlackBearEntity.this, Bee.class);
        }

        @Override
        public void start() {
            super.start();
            if (BlackBearEntity.this.isBaby()) {
                this.alertOthers();
                this.stop();
            }
        }

        @Override
        protected void alertOther(Mob mob, LivingEntity livingEntity) {
            if (mob instanceof BlackBearEntity && !mob.isBaby()) {
                super.alertOther(mob, livingEntity);
            }
        }
    }

    private class BlackBearEntityMeleeAttackGoal extends MeleeAttackGoal {

        public BlackBearEntityMeleeAttackGoal() {
            super(BlackBearEntity.this, 1.25, true);
        }

        @Override
        public void start() {
            super.start();
            BlackBearEntity.this.clearBearPose(BearPose.RESTING);
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
                    BlackBearEntity.this.playWarningSound();
                }
            } else {
                this.resetAttackCooldown();
            }
        }
    }
}
