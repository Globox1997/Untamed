package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.untamed.entity.ai.panther.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractBigCatEntity extends Animal implements NeutralMob {

    private static final EntityDataAccessor<Byte> DATA_PANTHER_POSE = SynchedEntityData.defineId(AbstractBigCatEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_REST_VARIANT = SynchedEntityData.defineId(AbstractBigCatEntity.class, EntityDataSerializers.BYTE);
    private static final int REST_VARIANTS = 3;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private static final int MAX_HUNGER = 48000;
    public static final int HUNGRY = 18000;
    private static final int FAMILY_REFRESH_INTERVAL = 40;
    private static final double FAMILY_RANGE = 32.0D;
    private static final double MOTHER_DEFEND_RANGE = 24.0D;
    private static final int MAX_TOLERANCE_ENTRIES = 8;

    private int restVariant;
    private int warningSoundTicks;
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;
    private int hunger;
    private int alarmTicks;
    private int timedPoseTicks;
    private boolean hunting;
    private long huntCooldownUntil;
    @Nullable
    private UUID motherUUID;
    @Nullable
    private AbstractBigCatEntity mother;
    private List<AbstractBigCatEntity> cubs = List.of();
    private boolean familyInitialized;
    @Nullable
    private BlockPos denPos;
    private final Map<UUID, Integer> playerTolerance = new HashMap<>();

    private float stalkAmount, stalkAmountO;
    private float pounceAmount, pounceAmountO;
    private float restAmount, restAmountO;
    private float snarlAmount, snarlAmountO;
    private float fishAmount, fishAmountO;

    protected AbstractBigCatEntity(EntityType<? extends AbstractBigCatEntity> entityType, Level level) {
        super(entityType, level);
    }

    public enum PantherPose {
        NONE, STALKING, POUNCE, RESTING, SNARLING, FISHING;

        private static final PantherPose[] VALUES = values();

        public static PantherPose byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    public static class BigCatGroupData extends AgeableMob.AgeableMobGroupData {
        @Nullable
        private UUID motherId;

        public BigCatGroupData() {
            super(1.0F);
        }
    }

    public abstract boolean isRestTime();

    public abstract double scoreCoverSpot(BlockPos pos);

    public abstract boolean isPreyType(LivingEntity entity);

    protected abstract SoundEvent getWarningSound();

    protected void registerSpeciesGoals() {
    }

    protected int getPreyFeedValue(LivingEntity prey) {
        return HUNGRY;
    }

    public double getNoticeDistanceScale() {
        return 1.0D;
    }

    public boolean snarlsAtCloseRange() {
        return true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0, pathfinderMob -> pathfinderMob.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new PantherMeleeAttackGoal(this, 1.3));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new PantherThreatResponseGoal(this));
        this.goalSelector.addGoal(5, new PantherStalkGoal(this));
        this.goalSelector.addGoal(6, new CubStayHiddenGoal(this));
        this.goalSelector.addGoal(6, new CubFollowMotherGoal(this));
        this.goalSelector.addGoal(7, new PantherRestGoal(this));
        this.goalSelector.addGoal(8, new PantherStrollGoal(this, 0.8));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new PantherHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 14, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
        this.registerSpeciesGoals();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        if (!(this.getType().create(serverLevel) instanceof AbstractBigCatEntity cub)) {
            return null;
        }
        cub.motherUUID = this.getUUID();
        cub.denPos = this.blockPosition();
        return cub;
    }

    @Override
    public boolean canMate(Animal animal) {
        if (animal == this || animal.getClass() != this.getClass()) {
            return false;
        }
        return this.isInLove() && animal.isInLove();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PANTHER_POSE, (byte) PantherPose.NONE.ordinal());
        builder.define(DATA_REST_VARIANT, (byte) 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            spawnGroupData = new BigCatGroupData();
        }
        this.hunger = this.random.nextInt(HUNGRY);
        SpawnGroupData result = super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
        if (spawnGroupData instanceof BigCatGroupData groupData) {
            if (!this.isBaby() && groupData.motherId == null) {
                groupData.motherId = this.getUUID();
            } else if (this.isBaby()) {
                this.motherUUID = groupData.motherId;
            }
        }
        if (this.isBaby() && this.denPos == null) {
            this.denPos = this.blockPosition();
        }
        return result;
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.isBaby()) {
            this.motherUUID = null;
            this.mother = null;
            this.denPos = null;
        }
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
        if (this.timedPoseTicks > 0 && --this.timedPoseTicks == 0) {
            this.clearPantherPose(PantherPose.POUNCE);
        }
        if (this.isBaby() && this.denPos == null) {
            this.denPos = this.blockPosition();
        }
        if (!this.familyInitialized || (this.tickCount + this.getId()) % FAMILY_REFRESH_INTERVAL == 0) {
            this.familyInitialized = true;
            this.refreshFamily();
        }
    }

    private void refreshFamily() {
        List<AbstractBigCatEntity> nearby = this.level().getEntitiesOfClass(AbstractBigCatEntity.class, this.getBoundingBox().inflate(FAMILY_RANGE, 8.0D, FAMILY_RANGE),
                other -> other != this && other.isAlive() && other.getType() == this.getType());
        this.mother = null;
        if (this.motherUUID != null) {
            for (AbstractBigCatEntity other : nearby) {
                if (other.getUUID().equals(this.motherUUID)) {
                    this.mother = other;
                    break;
                }
            }
        }
        List<AbstractBigCatEntity> ownCubs = new ArrayList<>();
        if (!this.isBaby()) {
            for (AbstractBigCatEntity other : nearby) {
                if (other.isBaby() && this.getUUID().equals(other.motherUUID)) {
                    ownCubs.add(other);
                }
            }
        }
        this.cubs = List.copyOf(ownCubs);
    }

    @Nullable
    public AbstractBigCatEntity getMother() {
        return this.mother != null && this.mother.isAlive() ? this.mother : null;
    }

    public List<AbstractBigCatEntity> getCubs() {
        return this.cubs;
    }

    public boolean hasOwnCubNearby(double distance) {
        for (AbstractBigCatEntity cub : this.cubs) {
            if (cub.isAlive() && this.distanceToSqr(cub) < distance * distance) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public BlockPos getDenPos() {
        return this.denPos;
    }

    public void setDenPos(@Nullable BlockPos denPos) {
        this.denPos = denPos;
    }

    public boolean isYoungCub() {
        return this.isBaby() && this.getAge() < -12000;
    }

    public boolean isHungry() {
        return this.hunger >= HUNGRY;
    }

    public boolean isPeckish() {
        return this.hunger >= HUNGRY / 2;
    }

    public void feed(int amount) {
        this.hunger = Math.max(0, this.hunger - amount);
    }

    public boolean isHunting() {
        return this.hunting;
    }

    public void setHunting(boolean hunting) {
        this.hunting = hunting;
    }

    public boolean canStartHunt() {
        return this.level().getGameTime() >= this.huntCooldownUntil;
    }

    public void setHuntCooldown(int ticks) {
        this.huntCooldownUntil = this.level().getGameTime() + ticks;
    }

    public boolean isValidPrey(LivingEntity entity) {
        if (!(entity instanceof Animal animal) || !animal.isAlive() || animal.getType() == this.getType() || !this.isPreyType(animal)) {
            return false;
        }
        return !animal.isLeashed() && !animal.hasCustomName() && !(animal instanceof TamableAnimal tamable && tamable.isTame()) && !(animal instanceof AbstractHorse horse && horse.isTamed());
    }

    @Override
    public boolean killedEntity(ServerLevel serverLevel, LivingEntity livingEntity) {
        if (this.isPreyType(livingEntity)) {
            this.feed(this.getPreyFeedValue(livingEntity));
        }
        return super.killedEntity(serverLevel, livingEntity);
    }

    public boolean isAlarmed() {
        return this.alarmTicks > 0;
    }

    public void alarm(int ticks) {
        this.alarmTicks = Math.max(this.alarmTicks, ticks);
    }

    @Override
    public boolean hurt(DamageSource damageSource, float f) {
        boolean result = super.hurt(damageSource, f);
        if (result && !this.level().isClientSide()) {
            this.alarm(200);
            this.clearPantherPose(PantherPose.RESTING);
            if (damageSource.getEntity() instanceof Player player) {
                this.playerTolerance.remove(player.getUUID());
            }
            AbstractBigCatEntity cubMother = this.getMother();
            if (this.isBaby() && cubMother != null && damageSource.getEntity() instanceof LivingEntity attacker && attacker != cubMother
                    && cubMother.distanceToSqr(this) < MOTHER_DEFEND_RANGE * MOTHER_DEFEND_RANGE) {
                cubMother.setTarget(attacker);
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

    public PantherPose getPantherPose() {
        return PantherPose.byId(this.entityData.get(DATA_PANTHER_POSE));
    }

    public void setPantherPose(PantherPose pose) {
        if (pose == PantherPose.RESTING && this.entityData.get(DATA_PANTHER_POSE) != (byte) PantherPose.RESTING.ordinal()) {
            this.entityData.set(DATA_REST_VARIANT, (byte) this.random.nextInt(REST_VARIANTS));
        }
        this.entityData.set(DATA_PANTHER_POSE, (byte) pose.ordinal());
    }

    public void clearPantherPose(PantherPose pose) {
        if (this.getPantherPose() == pose) {
            this.setPantherPose(PantherPose.NONE);
        }
    }

    public void startPounce() {
        this.setPantherPose(PantherPose.POUNCE);
        this.timedPoseTicks = 15;
    }

    public int getRestVariant() {
        return this.restVariant;
    }

    public boolean isResting() {
        return this.getPantherPose() == PantherPose.RESTING;
    }

    public float getStalkAmount(float partialTick) {
        return Mth.lerp(partialTick, this.stalkAmountO, this.stalkAmount);
    }

    public float getPounceAmount(float partialTick) {
        return Mth.lerp(partialTick, this.pounceAmountO, this.pounceAmount);
    }

    public float getRestAmount(float partialTick) {
        return Mth.lerp(partialTick, this.restAmountO, this.restAmount);
    }

    public float getSnarlAmount(float partialTick) {
        return Mth.lerp(partialTick, this.snarlAmountO, this.snarlAmount);
    }

    public float getFishAmount(float partialTick) {
        return Mth.lerp(partialTick, this.fishAmountO, this.fishAmount);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.warningSoundTicks > 0) {
            this.warningSoundTicks--;
        }

        if (this.level().isClientSide()) {
            PantherPose pose = this.getPantherPose();
            this.stalkAmountO = this.stalkAmount;
            this.pounceAmountO = this.pounceAmount;
            if (this.restAmount <= 0.0F) {
                this.restVariant = this.entityData.get(DATA_REST_VARIANT);
            }
            this.restAmountO = this.restAmount;
            this.snarlAmountO = this.snarlAmount;
            this.fishAmountO = this.fishAmount;
            this.stalkAmount = Mth.approach(this.stalkAmount, pose == PantherPose.STALKING ? 1.0F : 0.0F, 0.1F);
            this.pounceAmount = Mth.approach(this.pounceAmount, pose == PantherPose.POUNCE ? 1.0F : 0.0F, 0.3F);
            this.restAmount = Mth.approach(this.restAmount, pose == PantherPose.RESTING ? 1.0F : 0.0F, 0.08F);
            this.snarlAmount = Mth.approach(this.snarlAmount, pose == PantherPose.SNARLING ? 1.0F : 0.0F, 0.2F);
            this.fishAmount = Mth.approach(this.fishAmount, pose == PantherPose.FISHING ? 1.0F : 0.0F, 0.2F);
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
        this.motherUUID = compoundTag.hasUUID("Mother") ? compoundTag.getUUID("Mother") : null;
        this.denPos = NbtUtils.readBlockPos(compoundTag, "Den").orElse(null);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        this.addPersistentAngerSaveData(compoundTag);
        compoundTag.putInt("Hunger", this.hunger);
        if (this.motherUUID != null) {
            compoundTag.putUUID("Mother", this.motherUUID);
        }
        if (this.denPos != null) {
            compoundTag.put("Den", NbtUtils.writeBlockPos(this.denPos));
        }
    }

    @Override
    public void playAmbientSound() {
        if (!this.isResting() && this.getPantherPose() != PantherPose.STALKING) {
            super.playAmbientSound();
        }
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.7F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    public void playWarningSound() {
        if (this.warningSoundTicks <= 0) {
            this.makeSound(this.getWarningSound());
            this.warningSoundTicks = 40;
        }
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }
}
