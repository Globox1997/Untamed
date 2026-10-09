package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.untamed.entity.ai.kiwi.*;
import net.untamed.init.EntityInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

public class KiwiEntity extends Animal {

    private static final EntityDataAccessor<Byte> DATA_KIWI_POSE = SynchedEntityData.defineId(KiwiEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_REST_VARIANT = SynchedEntityData.defineId(KiwiEntity.class, EntityDataSerializers.BYTE);
    private static final int REST_VARIANTS = 2;
    private int restVariant;

    private int alarmTicks;
    @Nullable
    private BlockPos burrowPos;

    private float sleepAmount, sleepAmountO;
    private float probeAmount, probeAmountO;
    private float defendAmount, defendAmountO;

    public KiwiEntity(EntityType<? extends KiwiEntity> entityType, Level level) {
        super(entityType, level);
    }

    public enum KiwiPose {
        NONE, SLEEPING, PROBING, DEFENSIVE;

        private static final KiwiPose[] VALUES = values();

        public static KiwiPose byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        KiwiEntity chick = EntityInit.KIWI.create(serverLevel);
        if (chick != null) {
            chick.setBurrowPos(this.burrowPos != null ? this.burrowPos : this.blockPosition());
        }
        return chick;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.KIWI_FOOD);
    }

    @Override
    public boolean canMate(Animal animal) {
        if (!(animal instanceof KiwiEntity kiwiEntity)) {
            return false;
        }
        return this.isInLove() && kiwiEntity.isInLove();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new KiwiThreatGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 2.0, pathfinderMob -> DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new ChickStayNearBurrowGoal(this));
        this.goalSelector.addGoal(5, new KiwiRestGoal(this));
        this.goalSelector.addGoal(6, new ProbeGoal(this));
        this.goalSelector.addGoal(7, new KiwiStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.ATTACK_DAMAGE, 1.5D);
    }

    public static boolean checkKiwiEntitySpawnRules(EntityType<KiwiEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_TAIGA) ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.KIWIS_SPAWNABLE_ON);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIWI_POSE, (byte) KiwiPose.NONE.ordinal());
        builder.define(DATA_REST_VARIANT, (byte) 0);
    }

    public boolean isRestTime() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 23500L || time < 12500L;
    }

    @Nullable
    public BlockPos getBurrowPos() {
        return this.burrowPos;
    }

    public void setBurrowPos(@Nullable BlockPos burrowPos) {
        this.burrowPos = burrowPos;
    }

    public double scoreHiddenSpot(BlockPos pos) {
        Level level = this.level();
        double score = level.canSeeSky(pos.above()) ? -10.0D : 10.0D;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (level.getBlockState(side).is(BlockTags.LOGS)) {
                score += 6.0D;
            }
            if (level.getBlockState(side).is(TagInit.KIWI_COVER) || level.getBlockState(side.above()).is(TagInit.KIWI_COVER)) {
                score += 4.0D;
            }
        }
        if (level.getBlockState(pos).is(TagInit.KIWI_COVER)) {
            score += 4.0D;
        }
        return score;
    }

    public boolean isHiddenSpot(BlockPos pos) {
        return this.scoreHiddenSpot(pos) > 0.0D;
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
    }

    @Override
    public boolean hurt(DamageSource damageSource, float f) {
        boolean result = super.hurt(damageSource, f);
        if (result && !this.level().isClientSide()) {
            this.alarm(200);
            this.clearKiwiPose(KiwiPose.SLEEPING);
        }
        return result;
    }

    public KiwiPose getKiwiPose() {
        return KiwiPose.byId(this.entityData.get(DATA_KIWI_POSE));
    }

    public void setKiwiPose(KiwiPose pose) {
        if (pose == KiwiPose.SLEEPING && this.entityData.get(DATA_KIWI_POSE) != (byte) KiwiPose.SLEEPING.ordinal()) {
            this.entityData.set(DATA_REST_VARIANT, (byte) this.random.nextInt(REST_VARIANTS));
        }
        this.entityData.set(DATA_KIWI_POSE, (byte) pose.ordinal());
    }

    public void clearKiwiPose(KiwiPose pose) {
        if (this.getKiwiPose() == pose) {
            this.setKiwiPose(KiwiPose.NONE);
        }
    }

    public int getRestVariant() {
        return this.restVariant;
    }

    public boolean isSleeping() {
        return this.getKiwiPose() == KiwiPose.SLEEPING;
    }

    public float getSleepAmount(float partialTick) {
        return Mth.lerp(partialTick, this.sleepAmountO, this.sleepAmount);
    }

    public float getProbeAmount(float partialTick) {
        return Mth.lerp(partialTick, this.probeAmountO, this.probeAmount);
    }

    public float getDefendAmount(float partialTick) {
        return Mth.lerp(partialTick, this.defendAmountO, this.defendAmount);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            KiwiPose pose = this.getKiwiPose();
            if (this.sleepAmount <= 0.0F) {
                this.restVariant = this.entityData.get(DATA_REST_VARIANT);
            }
            this.sleepAmountO = this.sleepAmount;
            this.probeAmountO = this.probeAmount;
            this.defendAmountO = this.defendAmount;
            this.sleepAmount = Mth.approach(this.sleepAmount, pose == KiwiPose.SLEEPING ? 1.0F : 0.0F, 0.1F);
            this.probeAmount = Mth.approach(this.probeAmount, pose == KiwiPose.PROBING ? 1.0F : 0.0F, 0.2F);
            this.defendAmount = Mth.approach(this.defendAmount, pose == KiwiPose.DEFENSIVE ? 1.0F : 0.0F, 0.25F);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.burrowPos = NbtUtils.readBlockPos(compoundTag, "Burrow").orElse(null);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        if (this.burrowPos != null) {
            compoundTag.put("Burrow", NbtUtils.writeBlockPos(this.burrowPos));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.KIWI_IDLE_EVENT;
    }

    @Override
    public void playAmbientSound() {
        if (!this.isSleeping()) {
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.KIWI_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.KIWI_DEATH_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.KIWI_STEP_EVENT, 0.15F, 1.0F);
    }

    public void playSnuffle() {
        this.playSound(SoundInit.KIWI_STEP_EVENT, 0.3F, 0.6F + this.random.nextFloat() * 0.1F);
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
        SpawnGroupData result = super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
        if (this.isBaby() && this.burrowPos == null) {
            this.burrowPos = this.blockPosition();
        }
        return result;
    }
}
