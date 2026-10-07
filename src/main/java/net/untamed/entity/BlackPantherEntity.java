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
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.untamed.entity.ai.panther.*;
import net.untamed.init.ConfigInit;
import net.untamed.init.EntityInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BlackPantherEntity extends Animal implements NeutralMob {

    private static final EntityDataAccessor<Byte> DATA_PANTHER_POSE = SynchedEntityData.defineId(BlackPantherEntity.class, EntityDataSerializers.BYTE);
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private static final int MAX_HUNGER = 48000;
    public static final int HUNGRY = 18000;
    private static final int FAMILY_REFRESH_INTERVAL = 40;
    private static final double FAMILY_RANGE = 32.0D;
    private static final double MOTHER_DEFEND_RANGE = 24.0D;
    private static final int MAX_TOLERANCE_ENTRIES = 8;

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
    private BlackPantherEntity mother;
    private List<BlackPantherEntity> cubs = List.of();
    private boolean familyInitialized;
    @Nullable
    private BlockPos denPos;
    private final Map<UUID, Integer> playerTolerance = new HashMap<>();

    private float stalkAmount, stalkAmountO;
    private float pounceAmount, pounceAmountO;
    private float restAmount, restAmountO;
    private float snarlAmount, snarlAmountO;
    private float fishAmount, fishAmountO;

    public BlackPantherEntity(EntityType<? extends BlackPantherEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
    }

    public enum PantherPose {
        NONE, STALKING, POUNCE, RESTING, SNARLING, FISHING;

        private static final PantherPose[] VALUES = values();

        public static PantherPose byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    public static class PantherGroupData extends AgeableMob.AgeableMobGroupData {
        @Nullable
        private UUID motherId;

        public PantherGroupData() {
            super(1.0F);
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        BlackPantherEntity cub = EntityInit.BLACK_PANTHER.create(serverLevel);
        if (cub != null) {
            cub.motherUUID = this.getUUID();
            cub.denPos = this.blockPosition();
        }
        return cub;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.BLACK_PANTHER_FOOD);
    }

    @Override
    public boolean canMate(Animal animal) {
        if (!(animal instanceof BlackPantherEntity blackPantherEntity)) {
            return false;
        }
        return this.isInLove() && blackPantherEntity.isInLove();
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
        this.goalSelector.addGoal(5, new PantherFishGoal(this));
        this.goalSelector.addGoal(6, new CubStayHiddenGoal(this));
        this.goalSelector.addGoal(6, new CubFollowMotherGoal(this));
        this.goalSelector.addGoal(7, new PantherRestGoal(this));
        this.goalSelector.addGoal(8, new PantherStrollGoal(this, 0.8));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new PantherHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 14, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 28.0D).add(Attributes.FOLLOW_RANGE, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.28D).add(Attributes.ATTACK_DAMAGE, 6.0D);
    }

    public static boolean checkBlackPantherEntitySpawnRules(EntityType<BlackPantherEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_JUNGLE)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.BLACK_PANTHERS_SPAWNABLE_ON);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PANTHER_POSE, (byte) PantherPose.NONE.ordinal());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            spawnGroupData = new PantherGroupData();
        }
        this.hunger = this.random.nextInt(HUNGRY);
        SpawnGroupData result = super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
        if (spawnGroupData instanceof PantherGroupData groupData) {
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

    public boolean isRestTime() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 1000L && time <= 11500L;
    }

    public double scoreCoverSpot(BlockPos pos) {
        Level level = this.level();
        double score = level.canSeeSky(pos.above()) ? -10.0D : 10.0D;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState side = level.getBlockState(pos.relative(direction));
            if (side.is(BlockTags.LOGS) || side.is(BlockTags.LEAVES)) {
                score += 3.0D;
            }
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                score += 3.0D;
            }
        }
        return score;
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
        List<BlackPantherEntity> nearby = this.level().getEntitiesOfClass(BlackPantherEntity.class, this.getBoundingBox().inflate(FAMILY_RANGE, 8.0D, FAMILY_RANGE),
                other -> other != this && other.isAlive());
        this.mother = null;
        if (this.motherUUID != null) {
            for (BlackPantherEntity other : nearby) {
                if (other.getUUID().equals(this.motherUUID)) {
                    this.mother = other;
                    break;
                }
            }
        }
        List<BlackPantherEntity> ownCubs = new ArrayList<>();
        if (!this.isBaby()) {
            for (BlackPantherEntity other : nearby) {
                if (other.isBaby() && this.getUUID().equals(other.motherUUID)) {
                    ownCubs.add(other);
                }
            }
        }
        this.cubs = List.copyOf(ownCubs);
    }

    @Nullable
    public BlackPantherEntity getMother() {
        return this.mother != null && this.mother.isAlive() ? this.mother : null;
    }

    public List<BlackPantherEntity> getCubs() {
        return this.cubs;
    }

    public boolean hasOwnCubNearby(double distance) {
        for (BlackPantherEntity cub : this.cubs) {
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
        if (!(entity instanceof Animal animal) || !animal.isAlive() || animal instanceof BlackPantherEntity || !isPreyType(animal)) {
            return false;
        }
        return !animal.isLeashed() && !animal.hasCustomName() && !(animal instanceof TamableAnimal tamable && tamable.isTame()) && !(animal instanceof AbstractHorse horse && horse.isTamed());
    }

    public static boolean isPreyType(LivingEntity entity) {
        return entity.getType().is(TagInit.BLACK_PANTHER_PREY) || entity.getType().is(TagInit.BLACK_PANTHER_FISH)
                || (ConfigInit.CONFIG.panthersHuntLivestock && entity.getType().is(TagInit.BLACK_PANTHER_LIVESTOCK_PREY));
    }

    @Override
    public boolean killedEntity(ServerLevel serverLevel, LivingEntity livingEntity) {
        if (isPreyType(livingEntity)) {
            this.feed(livingEntity.getType().is(TagInit.BLACK_PANTHER_FISH) ? HUNGRY / 3 : HUNGRY);
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
            BlackPantherEntity cubMother = this.getMother();
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
    protected SoundEvent getAmbientSound() {
        return SoundInit.BLACK_PANTHER_IDLE_EVENT;
    }

    @Override
    public void playAmbientSound() {
        if (!this.isResting() && this.getPantherPose() != PantherPose.STALKING) {
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.BLACK_PANTHER_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.BLACK_PANTHER_DEATH_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.BLACK_PANTHER_STEP_EVENT, 0.15F, 1.0F);
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.7F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    public void playWarningSound() {
        if (this.warningSoundTicks <= 0) {
            this.makeSound(SoundInit.BLACK_PANTHER_ATTACK_EVENT);
            this.warningSoundTicks = 40;
        }
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }
}
