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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.ai.lion.*;
import net.untamed.init.ConfigInit;
import net.untamed.init.EntityInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractLionEntity extends Animal implements NeutralMob {

    private static final EntityDataAccessor<Byte> DATA_LION_POSE = SynchedEntityData.defineId(AbstractLionEntity.class, EntityDataSerializers.BYTE);
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);

    public static final double TERRITORY_RADIUS = 32.0D;
    private static final int PRIDE_REFRESH_INTERVAL = 40;
    private static final double PRIDE_RANGE = 48.0D;
    private static final double JOIN_RANGE = 16.0D;
    private static final int MAX_PRIDE_SIZE = 16;
    private static final float PRIDE_CUB_CHANCE = 0.3F;
    private static final int MAX_HUNGER = 48000;
    public static final int HUNGRY = 24000;
    private static final int MAX_TOLERANCE_ENTRIES = 8;
    private static final int ROAR_REQUEST_TIMEOUT = 100;

    private int warningSoundTicks;
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    @Nullable
    private UUID prideId;
    @Nullable
    private BlockPos homePos;
    private List<AbstractLionEntity> pride = List.of();
    @Nullable
    private Vec3 prideCenter;
    private boolean prideInitialized;

    private int alarmTicks;
    private int hunger;
    private int stamina;
    private boolean drainedStamina;
    @Nullable
    private LionHunt hunt;
    private long huntCooldownUntil;
    private long roarRequestTime;
    private long lastHeardRoarTime = -24000L;
    @Nullable
    private BlockPos avoidPos;
    private int avoidTicks;
    private final Map<UUID, Integer> playerTolerance = new HashMap<>();

    private float sleepAmount, sleepAmountO;
    private float stalkAmount, stalkAmountO;
    private float roarAmount, roarAmountO;
    private float warnAmount, warnAmountO;
    private float playAmount, playAmountO;

    protected AbstractLionEntity(EntityType<? extends AbstractLionEntity> entityType, Level level) {
        super(entityType, level);
        this.stamina = this.getMaxStamina();
    }

    public enum LionPose {
        NONE, SLEEPING, STALKING, ROARING, WARNING, PLAYING;

        private static final LionPose[] VALUES = values();

        public static LionPose byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    public record ThreatProfile(double watchDistance, double warnDistance, double mockChargeDistance, int warnTicksBeforeMockCharge, int mockChargesBeforeAttack) {
    }

    public static class PrideGroupData extends AgeableMob.AgeableMobGroupData {
        private final UUID prideId;
        private final BlockPos home;
        private boolean membersAdded;

        public PrideGroupData(UUID prideId, BlockPos home) {
            super(PRIDE_CUB_CHANCE);
            this.prideId = prideId;
            this.home = home;
        }
    }

    public abstract boolean isMale();

    public abstract ThreatProfile getThreatProfile();

    public abstract int getMaxStamina();

    protected abstract void registerSpeciesGoals();

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0, pathfinderMob -> pathfinderMob.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new LionMeleeAttackGoal(this, 1.3));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0, AbstractLionEntity.class));
        this.goalSelector.addGoal(4, new LionThreatResponseGoal(this));
        this.goalSelector.addGoal(5, new AvoidTerritoryGoal(this));
        this.goalSelector.addGoal(6, new CubStayNearPrideGoal(this));
        this.goalSelector.addGoal(6, new RejoinPrideGoal(this, 1.1));
        this.goalSelector.addGoal(7, new LionRoarGoal(this));
        this.goalSelector.addGoal(8, new LionRestGoal(this));
        this.goalSelector.addGoal(8, new CubNapGoal(this));
        this.goalSelector.addGoal(9, new CubPlayGoal(this));
        this.goalSelector.addGoal(10, new PrideStrollGoal(this, 0.8));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new PrideHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
        this.registerSpeciesGoals();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LION_POSE, (byte) LionPose.NONE.ordinal());
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(this.isMale() ? TagInit.LION_FOOD : TagInit.LIONESS_FOOD);
    }

    @Override
    public boolean canMate(Animal animal) {
        return animal instanceof AbstractLionEntity other && other != this && this.isMale() != other.isMale() && this.isInLove() && other.isInLove();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob partner) {
        AbstractLionEntity cub = this.random.nextBoolean() ? EntityInit.LION.create(serverLevel) : EntityInit.LIONESS.create(serverLevel);
        if (cub != null) {
            AbstractLionEntity mother = this.isMale() && partner instanceof AbstractLionEntity other ? other : this;
            cub.joinPride(mother.prideId != null ? mother.prideId : this.prideId, mother.homePos != null ? mother.homePos : this.homePos);
        }
        return cub;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            boolean natural = mobSpawnType == MobSpawnType.NATURAL || mobSpawnType == MobSpawnType.CHUNK_GENERATION;
            spawnGroupData = natural && !this.isMale() ? new PrideGroupData(UUID.randomUUID(), this.blockPosition()) : new AgeableMobGroupData(false);
        }
        if (spawnGroupData instanceof PrideGroupData prideData) {
            this.joinPride(prideData.prideId, prideData.home);
        }
        this.hunger = this.random.nextInt(HUNGRY);
        SpawnGroupData result = super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
        if (spawnGroupData instanceof PrideGroupData prideData && !prideData.membersAdded && !this.isBaby()) {
            prideData.membersAdded = true;
            this.spawnPrideMembers(serverLevelAccessor, difficultyInstance, mobSpawnType, prideData);
        }
        return result;
    }

    private void spawnPrideMembers(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, PrideGroupData prideData) {
        int males = this.random.nextFloat() < 0.3F ? 2 : 1;
        for (int i = 0; i < males; i++) {
            LionEntity lion = EntityInit.LION.create(level.getLevel());
            if (lion != null) {
                lion.moveTo(this.getX(), this.getY(), this.getZ(), this.random.nextFloat() * 360.0F, 0.0F);
                lion.finalizeSpawn(level, difficulty, spawnType, prideData);
                lion.setAge(0);
                level.addFreshEntityWithPassengers(lion);
            }
        }
        int cubs = this.random.nextInt(3);
        for (int i = 0; i < cubs; i++) {
            AbstractLionEntity cub = this.random.nextBoolean() ? EntityInit.LION.create(level.getLevel()) : EntityInit.LIONESS.create(level.getLevel());
            if (cub != null) {
                cub.moveTo(this.getX(), this.getY(), this.getZ(), this.random.nextFloat() * 360.0F, 0.0F);
                cub.finalizeSpawn(level, difficulty, spawnType, prideData);
                cub.setBaby(true);
                level.addFreshEntityWithPassengers(cub);
            }
        }
    }

    public void joinPride(@Nullable UUID prideId, @Nullable BlockPos home) {
        this.prideId = prideId;
        this.homePos = home;
        this.prideInitialized = false;
    }

    @Nullable
    public UUID getPrideId() {
        return this.prideId;
    }

    @Nullable
    public BlockPos getHomePos() {
        return this.homePos;
    }

    public boolean isNomad() {
        return this.prideId == null && this.isMale() && !this.isBaby();
    }

    public boolean isPrideMale() {
        return this.prideId != null && this.isMale() && !this.isBaby();
    }

    public boolean isInPrideWith(AbstractLionEntity other) {
        return this.prideId != null && this.prideId.equals(other.prideId);
    }

    public List<AbstractLionEntity> getPride() {
        return this.pride;
    }

    @Nullable
    public Vec3 getPrideCenter() {
        return this.prideCenter;
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
        if (!this.drainedStamina && this.stamina < this.getMaxStamina()) {
            this.stamina++;
        }
        this.drainedStamina = false;
        if (this.avoidTicks > 0 && --this.avoidTicks == 0) {
            this.avoidPos = null;
        }
        if (this.hunt != null && this.hunt.isFinished()) {
            this.hunt = null;
        }
        if (!this.prideInitialized || (this.tickCount + this.getId()) % PRIDE_REFRESH_INTERVAL == 0) {
            this.prideInitialized = true;
            this.refreshPride();
        }
    }

    private void refreshPride() {
        List<AbstractLionEntity> nearby = new ArrayList<>(this.level().getEntitiesOfClass(AbstractLionEntity.class, this.getBoundingBox().inflate(PRIDE_RANGE, 12.0D, PRIDE_RANGE),
                other -> other != this && other.isAlive()));
        nearby.sort(Comparator.comparingDouble(this::distanceToSqr));

        if (this.prideId == null) {
            this.tryJoinPride(nearby);
        }

        List<AbstractLionEntity> members = new ArrayList<>();
        if (this.prideId != null) {
            for (AbstractLionEntity other : nearby) {
                if (this.isInPrideWith(other) && members.size() < MAX_PRIDE_SIZE) {
                    members.add(other);
                }
            }
        }
        this.pride = List.copyOf(members);

        if (this.pride.isEmpty()) {
            this.prideCenter = null;
        } else {
            double x = 0, y = 0, z = 0;
            for (AbstractLionEntity member : this.pride) {
                x += member.getX();
                y += member.getY();
                z += member.getZ();
            }
            this.prideCenter = new Vec3(x / this.pride.size(), y / this.pride.size(), z / this.pride.size());
        }
    }

    private void tryJoinPride(List<AbstractLionEntity> nearby) {
        for (AbstractLionEntity other : nearby) {
            if (other.prideId == null || this.distanceTo(other) > JOIN_RANGE) {
                continue;
            }
            if (this.isMale() && !this.isBaby() && this.prideHasMale(other.prideId, nearby)) {
                continue;
            }
            this.joinPride(other.prideId, other.homePos != null ? other.homePos : other.blockPosition());
            this.prideInitialized = true;
            return;
        }
        if (!this.isMale()) {
            this.joinPride(UUID.randomUUID(), this.blockPosition());
            this.prideInitialized = true;
        }
    }

    private boolean prideHasMale(UUID id, List<AbstractLionEntity> nearby) {
        for (AbstractLionEntity other : nearby) {
            if (other.isPrideMale() && id.equals(other.prideId)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCubNearby(double distance) {
        for (AbstractLionEntity member : this.pride) {
            if (member.isAlive() && member.isBaby() && this.distanceToSqr(member) < distance * distance) {
                return true;
            }
        }
        return false;
    }

    public int countAdultsNearby(double distance) {
        int count = 0;
        for (AbstractLionEntity member : this.pride) {
            if (member.isAlive() && !member.isBaby() && this.distanceToSqr(member) < distance * distance) {
                count++;
            }
        }
        return count;
    }

    public int countSleepingNearby(double distance) {
        int count = 0;
        for (AbstractLionEntity member : this.pride) {
            if (member.isAlive() && member.isSleeping() && this.distanceToSqr(member) < distance * distance) {
                count++;
            }
        }
        return count;
    }

    public boolean isRestTime() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 1000L && time <= 11500L;
    }

    public boolean isAlarmed() {
        return this.alarmTicks > 0;
    }

    public void alarm(int ticks) {
        this.alarmTicks = Math.max(this.alarmTicks, ticks);
    }

    public void alarmPride(int ticks, double distance) {
        this.alarm(ticks);
        for (AbstractLionEntity member : this.pride) {
            if (member.isAlive() && this.distanceToSqr(member) < distance * distance) {
                member.alarm(ticks);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource damageSource, float f) {
        boolean result = super.hurt(damageSource, f);
        if (result && !this.level().isClientSide()) {
            this.alarmPride(200, 24.0D);
            this.clearLionPose(LionPose.SLEEPING);
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

    public boolean isHungry() {
        return this.hunger >= HUNGRY;
    }

    public void feed(int amount) {
        this.hunger = Math.max(0, this.hunger - amount);
    }

    public int getStamina() {
        return this.stamina;
    }

    public void drainStamina() {
        this.drainedStamina = true;
        if (this.stamina > 0) {
            this.stamina--;
        }
    }

    @Nullable
    public LionHunt getHunt() {
        return this.hunt;
    }

    public void setHunt(@Nullable LionHunt hunt) {
        this.hunt = hunt;
    }

    @Nullable
    public LivingEntity getHuntPrey() {
        return this.hunt != null && !this.hunt.isFinished() ? this.hunt.getPrey() : null;
    }

    public boolean canStartHunt() {
        return this.level().getGameTime() >= this.huntCooldownUntil;
    }

    public void setHuntCooldown(int ticks) {
        this.huntCooldownUntil = this.level().getGameTime() + ticks;
    }

    public boolean isValidPrey(LivingEntity entity, int hunters) {
        if (!(entity instanceof Animal animal) || !animal.isAlive() || !isPreyType(animal)) {
            return false;
        }
        if (animal.isLeashed() || animal.hasCustomName() || (animal instanceof TamableAnimal tamable && tamable.isTame()) || (animal instanceof AbstractHorse horse && horse.isTamed())) {
            return false;
        }
        return !animal.getType().is(TagInit.LION_LARGE_PREY) || hunters >= 3 || animal.isBaby();
    }

    public static boolean isPreyType(LivingEntity entity) {
        return entity.getType().is(TagInit.LION_PREY) || entity.getType().is(TagInit.LION_LARGE_PREY)
                || (ConfigInit.CONFIG.lionsHuntLivestock && entity.getType().is(TagInit.LION_LIVESTOCK_PREY));
    }

    @Override
    public boolean killedEntity(ServerLevel serverLevel, LivingEntity livingEntity) {
        if (isPreyType(livingEntity)) {
            this.feed(HUNGRY);
            for (AbstractLionEntity member : this.pride) {
                if (member.isAlive() && this.distanceToSqr(member) < 32.0D * 32.0D) {
                    member.feed(HUNGRY);
                }
            }
        }
        return super.killedEntity(serverLevel, livingEntity);
    }

    public void requestRoar(int delay) {
        this.roarRequestTime = this.level().getGameTime() + delay;
    }

    public boolean hasRoarRequest() {
        long time = this.level().getGameTime();
        return this.roarRequestTime != 0L && time >= this.roarRequestTime && time < this.roarRequestTime + ROAR_REQUEST_TIMEOUT;
    }

    public void clearRoarRequest() {
        this.roarRequestTime = 0L;
    }

    public long getLastHeardRoarTime() {
        return this.lastHeardRoarTime;
    }

    public void hearRoar(AbstractLionEntity roarer) {
        this.lastHeardRoarTime = this.level().getGameTime();
        if (this.isNomad() && !this.isInPrideWith(roarer) && roarer.prideId != null) {
            this.avoid(roarer.homePos != null ? roarer.homePos : roarer.blockPosition(), 1200);
        }
    }

    public void avoid(BlockPos pos, int ticks) {
        this.avoidPos = pos;
        this.avoidTicks = Math.max(this.avoidTicks, ticks);
    }

    @Nullable
    public BlockPos getAvoidPos() {
        return this.avoidPos;
    }

    public void stopAvoiding() {
        this.avoidPos = null;
        this.avoidTicks = 0;
    }

    public LionPose getLionPose() {
        return LionPose.byId(this.entityData.get(DATA_LION_POSE));
    }

    public void setLionPose(LionPose pose) {
        this.entityData.set(DATA_LION_POSE, (byte) pose.ordinal());
    }

    public void clearLionPose(LionPose pose) {
        if (this.getLionPose() == pose) {
            this.setLionPose(LionPose.NONE);
        }
    }

    public boolean isSleeping() {
        return this.getLionPose() == LionPose.SLEEPING;
    }

    public float getSleepAmount(float partialTick) {
        return Mth.lerp(partialTick, this.sleepAmountO, this.sleepAmount);
    }

    public float getStalkAmount(float partialTick) {
        return Mth.lerp(partialTick, this.stalkAmountO, this.stalkAmount);
    }

    public float getRoarAmount(float partialTick) {
        return Mth.lerp(partialTick, this.roarAmountO, this.roarAmount);
    }

    public float getWarnAmount(float partialTick) {
        return Mth.lerp(partialTick, this.warnAmountO, this.warnAmount);
    }

    public float getPlayAmount(float partialTick) {
        return Mth.lerp(partialTick, this.playAmountO, this.playAmount);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.warningSoundTicks > 0) {
            this.warningSoundTicks--;
        }

        if (this.level().isClientSide()) {
            LionPose pose = this.getLionPose();
            this.sleepAmountO = this.sleepAmount;
            this.stalkAmountO = this.stalkAmount;
            this.roarAmountO = this.roarAmount;
            this.warnAmountO = this.warnAmount;
            this.playAmountO = this.playAmount;
            this.sleepAmount = Mth.approach(this.sleepAmount, pose == LionPose.SLEEPING ? 1.0F : 0.0F, 0.1F);
            this.stalkAmount = Mth.approach(this.stalkAmount, pose == LionPose.STALKING ? 1.0F : 0.0F, 0.1F);
            this.roarAmount = Mth.approach(this.roarAmount, pose == LionPose.ROARING ? 1.0F : 0.0F, 0.15F);
            this.warnAmount = Mth.approach(this.warnAmount, pose == LionPose.WARNING ? 1.0F : 0.0F, 0.2F);
            this.playAmount = Mth.approach(this.playAmount, pose == LionPose.PLAYING ? 1.0F : 0.0F, 0.2F);
        } else {
            this.updatePersistentAnger((ServerLevel) this.level(), true);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.LION_IDLE_EVENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.LION_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.LION_DEATH_EVENT;
    }

    @Override
    public void playAmbientSound() {
        if (this.isSleeping()) {
            this.makeSound(SoundInit.LION_SLEEPING_EVENT);
        } else {
            super.playAmbientSound();
        }
    }

    public void playWarningSound() {
        if (this.warningSoundTicks <= 0) {
            this.makeSound(SoundInit.LION_ROARING_EVENT);
            this.warningSoundTicks = 40;
        }
    }

    public void playTerritoryRoar() {
        this.playSound(SoundInit.LION_ROARING_EVENT, 4.0F, this.getVoicePitch());
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.LION_STEP_EVENT, 0.15F, this.isMale() ? 1.0F : 1.2F);
    }

    @Override
    public float getVoicePitch() {
        float base = this.isBaby() ? 1.7F : this.isMale() ? 1.0F : 1.2F;
        return (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + base;
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
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        this.addPersistentAngerSaveData(compoundTag);
        if (this.prideId != null) {
            compoundTag.putUUID("Pride", this.prideId);
        }
        if (this.homePos != null) {
            compoundTag.put("Home", NbtUtils.writeBlockPos(this.homePos));
        }
        compoundTag.putInt("Hunger", this.hunger);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.readPersistentAngerSaveData(this.level(), compoundTag);
        this.prideId = compoundTag.hasUUID("Pride") ? compoundTag.getUUID("Pride") : null;
        this.homePos = NbtUtils.readBlockPos(compoundTag, "Home").orElse(null);
        this.hunger = compoundTag.getInt("Hunger");
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }
}
