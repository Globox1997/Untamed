package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.ai.bird.BirdFlyAroundGoal;
import net.untamed.entity.ai.bird.BirdPerchGoal;
import net.untamed.entity.ai.bird.BirdRoostGoal;
import net.untamed.entity.ai.bird.BirdThreatGoal;
import net.untamed.init.SoundInit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractBirdEntity extends Animal implements FlyingAnimal {

    public enum BirdPose {
        NONE, PERCHED, SLEEPING, HOVERING, FROZEN, BEATING;

        private static final BirdPose[] VALUES = values();

        public static BirdPose byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    private static final EntityDataAccessor<Byte> DATA_BIRD_POSE = SynchedEntityData.defineId(AbstractBirdEntity.class, EntityDataSerializers.BYTE);
    private static final int HUNGER_INTERVAL = 2400;
    private static final float FLAP_SOUND_STEP = 0.6F;
    private static final int SHARED_THREAT_TICKS = 100;
    private static final double MAX_SINK_SPEED = 0.12D;

    private int alarmTicks;
    private long lastAteTime = -HUNGER_INTERVAL;
    private float nextFlapSound = 1.0F;
    @Nullable
    private LivingEntity sharedThreat;
    private long sharedThreatUntil;

    private float flyAmount, flyAmountO;
    private float perchAmount, perchAmountO;
    private float sleepAmount, sleepAmountO;
    private float hoverAmount, hoverAmountO;
    private float frozenAmount, frozenAmountO;
    private float beatAmount, beatAmountO;

    protected AbstractBirdEntity(EntityType<? extends AbstractBirdEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
    }

    protected abstract TagKey<Item> getFoodTag();

    public abstract int getMinPerchTicks();

    public int getFlyRange() {
        return 8;
    }

    public double getFlySpeedModifier() {
        return 1.0D;
    }

    public abstract int getPerchTicksVariation();

    public abstract boolean freezesWhenThreatened();

    protected abstract float getSpeciesPitch();

    protected void registerSpeciesGoals() {
    }

    public void onThreatAlarm(LivingEntity threat) {
    }

    @Nullable
    public BlockPos getPreferredPerchCenter() {
        return null;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BirdThreatGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0D, Ingredient.of(this.getFoodTag()), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(6, new BirdRoostGoal(this));
        this.goalSelector.addGoal(7, new BirdPerchGoal(this));
        this.goalSelector.addGoal(8, new BirdFlyAroundGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.registerSpeciesGoals();
    }

    @Override
    protected @NotNull PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BIRD_POSE, (byte) BirdPose.NONE.ordinal());
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(this.getFoodTag());
    }

    @Override
    public boolean canMate(Animal animal) {
        if (animal == this || animal.getClass() != this.getClass()) {
            return false;
        }
        return this.isInLove() && animal.isInLove();
    }

    @Override
    public boolean isFlying() {
        return !this.onGround();
    }

    public boolean isRoostTime() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 13000L && time <= 23000L;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.alarmTicks > 0) {
            this.alarmTicks--;
        }
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
            this.setBirdPose(BirdPose.NONE);
        }
        return result;
    }

    public boolean isHungry() {
        return this.level().getGameTime() - this.lastAteTime > HUNGER_INTERVAL || this.getHealth() < this.getMaxHealth();
    }

    public void onFed(float healAmount) {
        this.heal(healAmount);
        this.lastAteTime = this.level().getGameTime();
        this.playSound(SoundEvents.GENERIC_EAT, 0.4F, 1.4F + this.random.nextFloat() * 0.2F);
    }

    public BirdPose getBirdPose() {
        return BirdPose.byId(this.entityData.get(DATA_BIRD_POSE));
    }

    public void setBirdPose(BirdPose pose) {
        this.entityData.set(DATA_BIRD_POSE, (byte) pose.ordinal());
    }

    public void clearBirdPose(BirdPose pose) {
        if (this.getBirdPose() == pose) {
            this.setBirdPose(BirdPose.NONE);
        }
    }

    public boolean isPerched() {
        return this.getBirdPose() == BirdPose.PERCHED;
    }

    public boolean isAsleep() {
        return this.getBirdPose() == BirdPose.SLEEPING;
    }

    public void warnOf(LivingEntity threat) {
        this.sharedThreat = threat;
        this.sharedThreatUntil = this.level().getGameTime() + SHARED_THREAT_TICKS;
        this.alarm(SHARED_THREAT_TICKS);
    }

    @Nullable
    public LivingEntity getSharedThreat() {
        if (this.sharedThreat != null && (!this.sharedThreat.isAlive() || this.level().getGameTime() > this.sharedThreatUntil)) {
            this.sharedThreat = null;
        }
        return this.sharedThreat;
    }

    public boolean isBusy() {
        return this.getBirdPose() != BirdPose.NONE && this.getBirdPose() != BirdPose.PERCHED;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            BirdPose pose = this.getBirdPose();
            this.flyAmountO = this.flyAmount;
            this.perchAmountO = this.perchAmount;
            this.sleepAmountO = this.sleepAmount;
            this.hoverAmountO = this.hoverAmount;
            this.frozenAmountO = this.frozenAmount;
            this.beatAmountO = this.beatAmount;
            boolean flying = !this.onGround() && !this.isInWater() && !this.isPassenger();
            this.flyAmount = Mth.approach(this.flyAmount, flying ? 1.0F : 0.0F, 0.2F);
            this.perchAmount = Mth.approach(this.perchAmount, pose == BirdPose.PERCHED || pose == BirdPose.FROZEN ? 1.0F : 0.0F, 0.15F);
            this.sleepAmount = Mth.approach(this.sleepAmount, pose == BirdPose.SLEEPING ? 1.0F : 0.0F, 0.08F);
            this.hoverAmount = Mth.approach(this.hoverAmount, pose == BirdPose.HOVERING ? 1.0F : 0.0F, 0.2F);
            this.frozenAmount = Mth.approach(this.frozenAmount, pose == BirdPose.FROZEN ? 1.0F : 0.0F, 0.2F);
            this.beatAmount = Mth.approach(this.beatAmount, pose == BirdPose.BEATING ? 1.0F : 0.0F, 0.25F);
        }
    }

    public float getFlyAmount(float partialTick) {
        return Mth.lerp(partialTick, this.flyAmountO, this.flyAmount);
    }

    public float getPerchAmount(float partialTick) {
        return Mth.lerp(partialTick, this.perchAmountO, this.perchAmount);
    }

    public float getSleepAmount(float partialTick) {
        return Mth.lerp(partialTick, this.sleepAmountO, this.sleepAmount);
    }

    public float getHoverAmount(float partialTick) {
        return Mth.lerp(partialTick, this.hoverAmountO, this.hoverAmount);
    }

    public float getFrozenAmount(float partialTick) {
        return Mth.lerp(partialTick, this.frozenAmountO, this.frozenAmount);
    }

    public float getBeatAmount(float partialTick) {
        return Mth.lerp(partialTick, this.beatAmountO, this.beatAmount);
    }

    @Override
    protected boolean isFlapping() {
        return this.isFlying() && this.flyDist > this.nextFlapSound;
    }

    @Override
    protected void onFlap() {
        this.playSound(SoundInit.BIRD_FLAP_EVENT, 0.15F, this.getSpeciesPitch() + (this.random.nextFloat() - 0.5F) * 0.2F);
        this.nextFlapSound = this.flyDist + FLAP_SOUND_STEP;
    }

    @Override
    public void travel(Vec3 travelVector) {
        super.travel(travelVector);
        if (!this.onGround() && !this.isInWater() && !this.isPassenger()) {
            Vec3 movement = this.getDeltaMovement();
            if (movement.y < -MAX_SINK_SPEED) {
                this.setDeltaMovement(movement.x, -MAX_SINK_SPEED, movement.z);
            }
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {
    }

    @Override
    public void playAmbientSound() {
        if (!this.isAsleep() && this.getBirdPose() != BirdPose.FROZEN) {
            super.playAmbientSound();
        }
    }

    public void playCall(float pitchOffset) {
        this.playSound(SoundInit.BIRD_IDLE_EVENT, 1.0F, this.getVoicePitch() + pitchOffset);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * this.getSpeciesPitch();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.BIRD_IDLE_EVENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.BIRD_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.BIRD_DEATH_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
    }
}
