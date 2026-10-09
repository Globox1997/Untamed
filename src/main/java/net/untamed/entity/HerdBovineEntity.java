package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.ai.herd.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class HerdBovineEntity extends Animal implements NeutralMob {

    private static final EntityDataAccessor<Byte> DATA_HERD_ROLE = SynchedEntityData.defineId(HerdBovineEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_HERD_POSE = SynchedEntityData.defineId(HerdBovineEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_REST_VARIANT = SynchedEntityData.defineId(HerdBovineEntity.class, EntityDataSerializers.BYTE);
    private static final int REST_VARIANTS = 2;
    private int restVariant;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);

    private static final int HERD_REFRESH_INTERVAL = 40;
    private static final double HERD_RANGE = 32.0D;
    private static final int MAX_HERD_SIZE = 24;
    private static final int MOTHER_MISSING_REFRESHES = 5;
    private static final float ROAMER_CHANCE = 0.3F;
    private static final float BABY_SPAWN_CHANCE = 0.25F;
    private static final int MAX_TOLERANCE_ENTRIES = 8;

    private int warningSoundTicks;
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    private List<HerdBovineEntity> herd = List.of();
    @Nullable
    private Vec3 herdCenter;
    private boolean herdInitialized;
    private long seniority;
    @Nullable
    private UUID motherUUID;
    @Nullable
    private HerdBovineEntity mother;
    private int motherMissingCount;
    private int alarmTicks;
    private final Map<UUID, Integer> playerTolerance = new HashMap<>();

    private float grazeAmount, grazeAmountO;
    private float restAmount, restAmountO;
    private float warnAmount, warnAmountO;
    private float chargeAmount, chargeAmountO;
    private float alertAmount, alertAmountO;

    protected HerdBovineEntity(EntityType<? extends HerdBovineEntity> entityType, Level level) {
        super(entityType, level);
    }

    public enum HerdPose {
        NONE, GRAZING, RESTING, WARNING, CHARGING, ALERT;

        private static final HerdPose[] VALUES = values();

        public static HerdPose byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    // HERD animals form the core of the group: they lead, vote and raise calves. ROAMERs are larger and more independent.
    public enum HerdRole {
        HERD, ROAMER;

        private static final HerdRole[] VALUES = values();

        public static HerdRole byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : HERD;
        }

        public static HerdRole byName(String name) {
            for (HerdRole role : VALUES) {
                if (role.getSerializedName().equals(name)) {
                    return role;
                }
            }
            return HERD;
        }

        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public record ThreatProfile(double watchDistance, double fleeDistance, double warnDistance, int warnTicksBeforeCharge) {
    }

    public abstract ThreatProfile getThreatProfile();

    public abstract boolean isRestTime();

    protected abstract SoundEvent getWarningSound();

    protected abstract void registerSpeciesGoals();

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0, pathfinderMob -> pathfinderMob.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new WarningMeleeAttackGoal(this, 1.25));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new ThreatResponseGoal(this));
        this.goalSelector.addGoal(5, new FollowMotherGoal(this));
        this.goalSelector.addGoal(5, new WaitForCalfGoal(this));
        this.goalSelector.addGoal(7, new RejoinHerdGoal(this, 1.1));
        this.goalSelector.addGoal(8, new HerdRestGoal(this));
        this.goalSelector.addGoal(8, new GrazeGoal(this));
        this.goalSelector.addGoal(9, new HerdStrollGoal(this, 0.8));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HerdHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
        this.registerSpeciesGoals();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HERD_ROLE, (byte) HerdRole.HERD.ordinal());
        builder.define(DATA_HERD_POSE, (byte) HerdPose.NONE.ordinal());
        builder.define(DATA_REST_VARIANT, (byte) 0);
    }

    @Override
    public boolean canMate(Animal animal) {
        if (animal == this || animal.getClass() != this.getClass()) {
            return false;
        }
        return this.isInLove() && animal.isInLove();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob partner) {
        if (!(this.getType().create(serverLevel) instanceof HerdBovineEntity calf)) {
            return null;
        }
        calf.setHerdRole(this.randomHerdRole());
        HerdBovineEntity calfMother = this.isRoamer() && partner instanceof HerdBovineEntity other && !other.isRoamer() ? other : this;
        calf.setMotherUUID(calfMother.getUUID());
        return calf;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            spawnGroupData = new AgeableMobGroupData(BABY_SPAWN_CHANCE);
        }
        this.setHerdRole(this.randomHerdRole());
        this.seniority = this.random.nextInt(120000);
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.isBaby()) {
            this.motherUUID = null;
            this.mother = null;
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
        if (!this.herdInitialized || (this.tickCount + this.getId()) % HERD_REFRESH_INTERVAL == 0) {
            this.herdInitialized = true;
            this.refreshHerd();
        }
    }

    private void refreshHerd() {
        List<HerdBovineEntity> found = new ArrayList<>(this.level().getEntitiesOfClass(HerdBovineEntity.class, this.getBoundingBox().inflate(HERD_RANGE, 8.0D, HERD_RANGE),
                other -> other != this && other.isAlive() && other.getType() == this.getType()));
        found.sort(Comparator.comparingDouble(this::distanceToSqr));
        this.herd = found.size() > MAX_HERD_SIZE ? List.copyOf(found.subList(0, MAX_HERD_SIZE)) : List.copyOf(found);

        double x = 0, y = 0, z = 0;
        int followers = 0;
        for (HerdBovineEntity member : this.herd) {
            if (member.followsHerd()) {
                x += member.getX();
                y += member.getY();
                z += member.getZ();
                followers++;
            }
        }
        this.herdCenter = followers == 0 ? null : new Vec3(x / followers, y / followers, z / followers);

        if (!this.isBaby()) {
            this.seniority += HERD_REFRESH_INTERVAL;
        } else {
            this.updateMother();
        }
        this.onHerdRefreshed();
    }

    private void updateMother() {
        if (this.motherUUID != null) {
            HerdBovineEntity found = this.findInHerd(this.motherUUID);
            if (found != null) {
                this.mother = found;
                this.motherMissingCount = 0;
                return;
            }
            this.mother = null;
            if (++this.motherMissingCount < MOTHER_MISSING_REFRESHES) {
                return;
            }
        }
        HerdBovineEntity adopted = null;
        int adoptedScore = -1;
        for (HerdBovineEntity member : this.herd) {
            if (member.isBaby()) {
                continue;
            }
            int score = (member.isRoamer() ? 0 : 2) + (member.hasOwnCalf() ? 0 : 1);
            if (score > adoptedScore) {
                adopted = member;
                adoptedScore = score;
            }
        }
        this.mother = adopted;
        this.motherUUID = adopted != null ? adopted.getUUID() : null;
        this.motherMissingCount = 0;
    }

    protected void onHerdRefreshed() {
    }

    @Nullable
    private HerdBovineEntity findInHerd(UUID uuid) {
        for (HerdBovineEntity member : this.herd) {
            if (member.isAlive() && member.getUUID().equals(uuid)) {
                return member;
            }
        }
        return null;
    }

    public List<HerdBovineEntity> getHerd() {
        return this.herd;
    }

    @Nullable
    public Vec3 getHerdCenter() {
        return this.herdCenter;
    }

    public long getSeniority() {
        return this.seniority;
    }

    @Nullable
    public HerdBovineEntity getHerdLeader() {
        return null;
    }

    public boolean isHerdLeader() {
        return this.getHerdLeader() == this;
    }

    @Nullable
    public Vec3 getHerdTravelTarget() {
        return null;
    }

    @Nullable
    public HerdBovineEntity getMother() {
        return this.mother != null && this.mother.isAlive() ? this.mother : null;
    }

    @Nullable
    public UUID getMotherUUID() {
        return this.motherUUID;
    }

    public void setMotherUUID(@Nullable UUID uuid) {
        this.motherUUID = uuid;
        this.mother = null;
    }

    public boolean hasCalfNearby(double distance) {
        for (HerdBovineEntity member : this.herd) {
            if (member.isAlive() && member.isBaby() && this.distanceToSqr(member) < distance * distance) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOwnCalf() {
        for (HerdBovineEntity member : this.herd) {
            if (member.isAlive() && member.isBaby() && this.getUUID().equals(member.getMotherUUID())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public HerdBovineEntity getStrayingCalf(double distance) {
        HerdBovineEntity farthest = null;
        double farthestSqr = distance * distance;
        for (HerdBovineEntity member : this.herd) {
            if (member.isAlive() && member.isBaby() && this.getUUID().equals(member.getMotherUUID())) {
                double distSqr = this.distanceToSqr(member);
                if (distSqr > farthestSqr) {
                    farthestSqr = distSqr;
                    farthest = member;
                }
            }
        }
        return farthest;
    }

    public int countRestingNearby(double distance) {
        int count = 0;
        for (HerdBovineEntity member : this.herd) {
            if (member.isAlive() && member.isResting() && this.distanceToSqr(member) < distance * distance) {
                count++;
            }
        }
        return count;
    }

    public boolean isAlarmed() {
        return this.alarmTicks > 0;
    }

    public void alarm(int ticks) {
        this.alarmTicks = Math.max(this.alarmTicks, ticks);
    }

    public void alarmHerd(int ticks, double distance) {
        this.alarm(ticks);
        for (HerdBovineEntity member : this.herd) {
            if (member.isAlive() && this.distanceToSqr(member) < distance * distance) {
                member.alarm(ticks);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource damageSource, float f) {
        boolean result = super.hurt(damageSource, f);
        if (result && !this.level().isClientSide()) {
            this.alarmHerd(200, 16.0D);
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

    public boolean defendsAsGroup() {
        return false;
    }

    public boolean prefersShade() {
        return false;
    }

    public void onRestEnded() {
    }

    public boolean followsHerd() {
        return true;
    }

    public boolean calfLeadsMother() {
        return false;
    }

    public boolean canDetect(Player player) {
        return true;
    }

    public void onThreatNoticed(Player player) {
    }

    public double scoreRestSpot(BlockPos pos) {
        return this.level().canSeeSky(pos.above()) ? -10.0D : 10.0D;
    }

    public void grazeAt(BlockPos pos) {
        if (this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && this.level().getBlockState(pos).is(Blocks.SHORT_GRASS) && this.random.nextInt(3) == 0) {
            this.level().destroyBlock(pos, false);
        }
    }

    public HerdRole getHerdRole() {
        return HerdRole.byId(this.entityData.get(DATA_HERD_ROLE));
    }

    public void setHerdRole(HerdRole role) {
        this.entityData.set(DATA_HERD_ROLE, (byte) role.ordinal());
    }

    public boolean isRoamer() {
        return this.getHerdRole() == HerdRole.ROAMER;
    }

    private HerdRole randomHerdRole() {
        return this.random.nextFloat() < ROAMER_CHANCE ? HerdRole.ROAMER : HerdRole.HERD;
    }

    public HerdPose getHerdPose() {
        return HerdPose.byId(this.entityData.get(DATA_HERD_POSE));
    }

    public void setHerdPose(HerdPose pose) {
        if (pose == HerdPose.RESTING && this.entityData.get(DATA_HERD_POSE) != (byte) HerdPose.RESTING.ordinal()) {
            this.entityData.set(DATA_REST_VARIANT, (byte) this.random.nextInt(REST_VARIANTS));
        }
        this.entityData.set(DATA_HERD_POSE, (byte) pose.ordinal());
    }

    public void clearHerdPose(HerdPose pose) {
        if (this.getHerdPose() == pose) {
            this.setHerdPose(HerdPose.NONE);
        }
    }

    public int getRestVariant() {
        return this.restVariant;
    }

    public boolean isResting() {
        return this.getHerdPose() == HerdPose.RESTING;
    }

    public float getGrazeAmount(float partialTick) {
        return Mth.lerp(partialTick, this.grazeAmountO, this.grazeAmount);
    }

    public float getRestAmount(float partialTick) {
        return Mth.lerp(partialTick, this.restAmountO, this.restAmount);
    }

    public float getWarnAmount(float partialTick) {
        return Mth.lerp(partialTick, this.warnAmountO, this.warnAmount);
    }

    public float getChargeAmount(float partialTick) {
        return Mth.lerp(partialTick, this.chargeAmountO, this.chargeAmount);
    }

    public float getAlertAmount(float partialTick) {
        return Mth.lerp(partialTick, this.alertAmountO, this.alertAmount);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.warningSoundTicks > 0) {
            this.warningSoundTicks--;
        }

        if (this.level().isClientSide()) {
            HerdPose pose = this.getHerdPose();
            this.grazeAmountO = this.grazeAmount;
            if (this.restAmount <= 0.0F) {
                this.restVariant = this.entityData.get(DATA_REST_VARIANT);
            }
            this.restAmountO = this.restAmount;
            this.warnAmountO = this.warnAmount;
            this.chargeAmountO = this.chargeAmount;
            this.alertAmountO = this.alertAmount;
            this.grazeAmount = Mth.approach(this.grazeAmount, pose == HerdPose.GRAZING ? 1.0F : 0.0F, 0.1F);
            this.restAmount = Mth.approach(this.restAmount, pose == HerdPose.RESTING ? 1.0F : 0.0F, 0.05F);
            this.warnAmount = Mth.approach(this.warnAmount, pose == HerdPose.WARNING ? 1.0F : 0.0F, 0.15F);
            this.chargeAmount = Mth.approach(this.chargeAmount, pose == HerdPose.CHARGING ? 1.0F : 0.0F, 0.2F);
            this.alertAmount = Mth.approach(this.alertAmount, pose == HerdPose.ALERT ? 1.0F : 0.0F, 0.15F);
        } else {
            this.updatePersistentAnger((ServerLevel) this.level(), true);
        }
    }

    public void playWarningSound() {
        if (this.warningSoundTicks <= 0) {
            this.makeSound(this.getWarningSound());
            this.warningSoundTicks = 40;
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
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        this.addPersistentAngerSaveData(compoundTag);
        compoundTag.putString("HerdRole", this.getHerdRole().getSerializedName());
        compoundTag.putLong("Seniority", this.seniority);
        if (this.motherUUID != null) {
            compoundTag.putUUID("Mother", this.motherUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.readPersistentAngerSaveData(this.level(), compoundTag);
        this.setHerdRole(HerdRole.byName(compoundTag.getString("HerdRole")));
        this.seniority = compoundTag.getLong("Seniority");
        this.motherUUID = compoundTag.hasUUID("Mother") ? compoundTag.getUUID("Mother") : null;
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.7F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }
}
