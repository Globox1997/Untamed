package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.untamed.entity.ai.raccoon.*;
import net.untamed.init.EntityInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class RaccoonEntity extends Animal {

    public enum RaccoonPose {
        NONE, RESTING, DOUSING, SITTING_UP, HISSING, FORAGING;

        private static final RaccoonPose[] VALUES = values();

        public static RaccoonPose byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    public static class RaccoonGroupData extends AgeableMob.AgeableMobGroupData {
        @Nullable
        private UUID motherId;

        public RaccoonGroupData() {
            super(1.0F);
        }
    }

    private static final EntityDataAccessor<Byte> DATA_RACCOON_POSE = SynchedEntityData.defineId(RaccoonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_REST_VARIANT = SynchedEntityData.defineId(RaccoonEntity.class, EntityDataSerializers.BYTE);
    private static final int REST_VARIANTS = 2;

    private static final int FAMILY_REFRESH_INTERVAL = 40;
    private static final double FAMILY_RANGE = 24.0D;
    private static final int HUNGER_INTERVAL = 2400;
    private static final int MIN_EXTRA_KITS = 1;
    private static final int EXTRA_KITS_VARIATION = 3;

    @Nullable
    private UUID motherUUID;
    @Nullable
    private RaccoonEntity mother;
    private List<RaccoonEntity> kits = List.of();
    private boolean familyInitialized;
    @Nullable
    private BlockPos denPos;
    private int alarmTicks;
    private long lastAteTime = -HUNGER_INTERVAL;

    private int restVariant;
    private float restAmount, restAmountO;
    private float douseAmount, douseAmountO;
    private float sitAmount, sitAmountO;
    private float hissAmount, hissAmountO;
    private float forageAmount, forageAmountO;

    public RaccoonEntity(EntityType<? extends RaccoonEntity> entityType, Level level) {
        super(entityType, level);
        this.setCanPickUpLoot(true);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RaccoonThreatGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 2.0, pathfinderMob -> DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new KitFollowMotherGoal(this));
        this.goalSelector.addGoal(5, new RaccoonDenGoal(this));
        this.goalSelector.addGoal(6, new DouseFoodGoal(this));
        this.goalSelector.addGoal(7, new ScavengeFoodGoal(this));
        this.goalSelector.addGoal(7, new RaidGoal(this));
        this.goalSelector.addGoal(8, new ShoreForageGoal(this));
        this.goalSelector.addGoal(9, new SitUpGoal(this));
        this.goalSelector.addGoal(10, new RaccoonStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    public static boolean checkRaccoonEntitySpawnRules(EntityType<RaccoonEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_FOREST) ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.RACCOONS_SPAWNABLE_ON);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        RaccoonEntity kit = EntityInit.RACCOON.create(serverLevel);
        if (kit != null) {
            kit.motherUUID = this.getUUID();
            kit.denPos = this.denPos;
        }
        return kit;
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel serverLevel, Animal partner) {
        super.spawnChildFromBreeding(serverLevel, partner);
        int extraKits = MIN_EXTRA_KITS + this.random.nextInt(EXTRA_KITS_VARIATION);
        for (int i = 0; i < extraKits; i++) {
            if (this.getBreedOffspring(serverLevel, partner) instanceof RaccoonEntity kit) {
                kit.setBaby(true);
                kit.moveTo(this.getX(), this.getY(), this.getZ(), this.random.nextFloat() * 360.0F, 0.0F);
                serverLevel.addFreshEntityWithPassengers(kit);
            }
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            spawnGroupData = new RaccoonGroupData();
        }
        SpawnGroupData result = super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
        if (spawnGroupData instanceof RaccoonGroupData groupData) {
            if (!this.isBaby() && groupData.motherId == null) {
                groupData.motherId = this.getUUID();
            } else if (this.isBaby()) {
                this.motherUUID = groupData.motherId;
            }
        }
        return result;
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
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.RACCOON_FOOD);
    }

    @Override
    public boolean canMate(Animal animal) {
        if (!(animal instanceof RaccoonEntity raccoonEntity)) {
            return false;
        }
        return this.isInLove() && raccoonEntity.isInLove();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RACCOON_POSE, (byte) RaccoonPose.NONE.ordinal());
        builder.define(DATA_REST_VARIANT, (byte) 0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.alarmTicks > 0) {
            this.alarmTicks--;
        }
        if (!this.familyInitialized || (this.tickCount + this.getId()) % FAMILY_REFRESH_INTERVAL == 0) {
            this.familyInitialized = true;
            this.refreshFamily();
        }
    }

    private void refreshFamily() {
        List<RaccoonEntity> nearby = this.level().getEntitiesOfClass(RaccoonEntity.class, this.getBoundingBox().inflate(FAMILY_RANGE, 8.0D, FAMILY_RANGE),
                other -> other != this && other.isAlive());
        this.mother = null;
        if (this.motherUUID != null) {
            for (RaccoonEntity other : nearby) {
                if (other.getUUID().equals(this.motherUUID)) {
                    this.mother = other;
                    break;
                }
            }
        }
        List<RaccoonEntity> ownKits = new ArrayList<>();
        if (!this.isBaby()) {
            for (RaccoonEntity other : nearby) {
                if (other.isBaby() && this.getUUID().equals(other.motherUUID)) {
                    ownKits.add(other);
                }
            }
            ownKits.sort(Comparator.comparing(Entity::getUUID));
        }
        this.kits = List.copyOf(ownKits);
    }

    @Nullable
    public RaccoonEntity getMother() {
        return this.mother != null && this.mother.isAlive() ? this.mother : null;
    }

    public List<RaccoonEntity> getKits() {
        return this.kits;
    }

    public boolean hasOwnKitNearby(double distance) {
        for (RaccoonEntity kit : this.kits) {
            if (kit.isAlive() && this.distanceToSqr(kit) < distance * distance) {
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

    public double scoreDenSpot(BlockPos pos) {
        Level level = this.level();
        double score = level.canSeeSky(pos.above()) ? -10.0D : 10.0D;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (level.getBlockState(side).is(BlockTags.LOGS)) {
                score += 6.0D;
            }
            if (level.getBlockState(side.above()).is(BlockTags.LEAVES)) {
                score += 2.0D;
            }
        }
        return score;
    }

    public boolean isRestTime() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 1000L && time <= 12500L;
    }

    public boolean isAlarmed() {
        return this.alarmTicks > 0;
    }

    public void alarm(int ticks) {
        this.alarmTicks = Math.max(this.alarmTicks, ticks);
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        boolean result = super.hurt(damageSource, amount);
        if (result && !this.level().isClientSide()) {
            this.alarm(200);
            this.clearRaccoonPose(RaccoonPose.RESTING);
        }
        return result;
    }

    public boolean isHungry() {
        return this.level().getGameTime() - this.lastAteTime > HUNGER_INTERVAL || this.getHealth() < this.getMaxHealth();
    }

    public void onFed(float healAmount) {
        this.heal(healAmount);
        this.lastAteTime = this.level().getGameTime();
        this.playSound(SoundEvents.GENERIC_EAT, 0.6F, 1.1F + this.random.nextFloat() * 0.2F);
    }

    public boolean isHoldingFood() {
        return this.isFood(this.getMainHandItem());
    }

    public void eatHeldFood() {
        ItemStack held = this.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, held), this.getX(), this.getY() + 0.4D, this.getZ(), 6, 0.15D, 0.1D, 0.15D, 0.05D);
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.onFed(2.0F);
    }

    public void holdItem(ItemStack stack) {
        this.setItemSlot(EquipmentSlot.MAINHAND, stack);
        this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack) {
        return !this.isBaby() && this.isFood(itemStack) && this.getMainHandItem().isEmpty() && !this.isResting();
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {
        return this.canHoldItem(itemStack);
    }

    @Override
    protected void pickUpItem(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (!this.canHoldItem(stack)) {
            return;
        }
        this.holdItem(stack.split(1));
        this.take(itemEntity, 1);
        if (stack.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(stack);
        }
    }

    public RaccoonPose getRaccoonPose() {
        return RaccoonPose.byId(this.entityData.get(DATA_RACCOON_POSE));
    }

    public void setRaccoonPose(RaccoonPose pose) {
        if (pose == RaccoonPose.RESTING && this.entityData.get(DATA_RACCOON_POSE) != (byte) RaccoonPose.RESTING.ordinal()) {
            this.entityData.set(DATA_REST_VARIANT, (byte) this.random.nextInt(REST_VARIANTS));
        }
        this.entityData.set(DATA_RACCOON_POSE, (byte) pose.ordinal());
    }

    public void clearRaccoonPose(RaccoonPose pose) {
        if (this.getRaccoonPose() == pose) {
            this.setRaccoonPose(RaccoonPose.NONE);
        }
    }

    public boolean isResting() {
        return this.getRaccoonPose() == RaccoonPose.RESTING;
    }

    public int getRestVariant() {
        return this.restVariant;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            RaccoonPose pose = this.getRaccoonPose();
            if (this.restAmount <= 0.0F) {
                this.restVariant = this.entityData.get(DATA_REST_VARIANT);
            }
            this.restAmountO = this.restAmount;
            this.douseAmountO = this.douseAmount;
            this.sitAmountO = this.sitAmount;
            this.hissAmountO = this.hissAmount;
            this.forageAmountO = this.forageAmount;
            this.restAmount = Mth.approach(this.restAmount, pose == RaccoonPose.RESTING ? 1.0F : 0.0F, 0.08F);
            this.douseAmount = Mth.approach(this.douseAmount, pose == RaccoonPose.DOUSING ? 1.0F : 0.0F, 0.15F);
            this.sitAmount = Mth.approach(this.sitAmount, pose == RaccoonPose.SITTING_UP ? 1.0F : 0.0F, 0.15F);
            this.hissAmount = Mth.approach(this.hissAmount, pose == RaccoonPose.HISSING ? 1.0F : 0.0F, 0.25F);
            this.forageAmount = Mth.approach(this.forageAmount, pose == RaccoonPose.FORAGING ? 1.0F : 0.0F, 0.15F);
        }
    }

    public float getRestAmount(float partialTick) {
        return Mth.lerp(partialTick, this.restAmountO, this.restAmount);
    }

    public float getDouseAmount(float partialTick) {
        return Mth.lerp(partialTick, this.douseAmountO, this.douseAmount);
    }

    public float getSitAmount(float partialTick) {
        return Mth.lerp(partialTick, this.sitAmountO, this.sitAmount);
    }

    public float getHissAmount(float partialTick) {
        return Mth.lerp(partialTick, this.hissAmountO, this.hissAmount);
    }

    public float getForageAmount(float partialTick) {
        return Mth.lerp(partialTick, this.forageAmountO, this.forageAmount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.motherUUID = compoundTag.hasUUID("Mother") ? compoundTag.getUUID("Mother") : null;
        this.denPos = NbtUtils.readBlockPos(compoundTag, "Den").orElse(null);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        if (this.motherUUID != null) {
            compoundTag.putUUID("Mother", this.motherUUID);
        }
        if (this.denPos != null) {
            compoundTag.put("Den", NbtUtils.writeBlockPos(this.denPos));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.RACCOON_IDLE_EVENT;
    }

    @Override
    public void playAmbientSound() {
        if (!this.isResting()) {
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.RACCOON_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.RACCOON_DEATH_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.RACCOON_STEP_EVENT, 0.15F, 1.0F);
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }
}
