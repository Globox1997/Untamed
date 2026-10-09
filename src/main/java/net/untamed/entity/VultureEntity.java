package net.untamed.entity;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.ai.vulture.VultureAi;
import net.untamed.entity.ai.vulture.VultureMoveControl;
import net.untamed.entity.ai.vulture.WaterStruggle;
import net.untamed.init.BrainInit;
import net.untamed.init.EntityInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VultureEntity extends Animal implements FlyingAnimal {

    // Flight states
    public static final int FLIGHT_GROUND = 0;
    public static final int FLIGHT_GLIDE = 1;
    public static final int FLIGHT_THERMAL = 2;
    public static final int FLIGHT_TAKEOFF = 3;
    public static final int FLIGHT_DESCEND = 4;

    // Flight
    private static final float TAKEOFF_HEADING_JITTER = 30.0F;
    private static final double TAKEOFF_IMPULSE = 0.35;
    private static final double TAKEOFF_ASCENT = 0.02;
    private static final double TAKEOFF_SPEED = 0.12;
    private static final int TAKEOFF_CHANCE = 100;
    private static final double TAKEOFF_THERMAL_GAIN = 6.0;
    private static final double FALL_RECOVER_ALTITUDE = 4.0;

    // Thermal / glide
    private static final double SOAR_ALTITUDE = 30.0;
    private static final double THERMAL_TRIGGER_ALTITUDE = 15.0;
    private static final double GLIDE_SPEED = 0.25;
    private static final double GLIDE_SINK = 0.01;
    private static final float GLIDE_TURN_RATE_MAX = 0.3F;
    private static final double THERMAL_SPEED = 0.18;
    private static final double THERMAL_ASCENT = 0.01;
    private static final double THERMAL_MIN_RADIUS = 2.0;
    private static final double THERMAL_MAX_RADIUS = 8.0;
    private static final double DESCEND_SINK = 0.025;

    // Drag
    private static final double AIR_DRAG = 0.98;
    private static final double WATER_DRAG = 0.85;

    // Day / night
    private static final long DAY_START_TICK = 22200L;
    private static final long DAY_END_TICK = 12000L;

    // Synced data
    private static final EntityDataAccessor<Integer> DATA_FLIGHT_STATE = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PERCHED = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.BOOLEAN);

    // Session state (server only)
    private boolean landNavigation;
    private double thermalCenterX, thermalCenterZ;
    private boolean thermalClockwise;
    private float glideTurnRate;
    private double takeoffStartY;
    private final WaterStruggle waterStruggle = new WaterStruggle(this);

    public VultureEntity(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new VultureMoveControl(this);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
    }

    @Override
    protected Brain.@NotNull Provider<VultureEntity> brainProvider() {
        return VultureAi.brainProvider();
    }

    @Override
    protected @NotNull Brain<?> makeBrain(Dynamic<?> dynamic) {
        return VultureAi.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Brain<VultureEntity> getBrain() {
        return (Brain<VultureEntity>) super.getBrain();
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    @Override
    @SuppressWarnings("resource")
    protected void customServerAiStep() {
        long dayTime = this.level().getDayTime() % 24000L;
        boolean day = dayTime < DAY_END_TICK || dayTime >= DAY_START_TICK;
        if (day) this.brain.setMemory(BrainInit.IS_DAYTIME, Unit.INSTANCE);
        else this.brain.eraseMemory(BrainInit.IS_DAYTIME);
        this.tickFlightAutopilot();
        this.updateNavigationMode();
        this.level().getProfiler().push("vultureBrain");
        this.getBrain().tick((ServerLevel) this.level(), this);
        this.level().getProfiler().pop();
        this.level().getProfiler().push("vultureActivityUpdate");
        VultureAi.updateActivity(this);
        this.level().getProfiler().pop();
        super.customServerAiStep();
    }

    public int getFlightState() { return this.entityData.get(DATA_FLIGHT_STATE); }
    private void setFlightState(int state) { this.entityData.set(DATA_FLIGHT_STATE, state); }
    public boolean isPerched() { return this.entityData.get(DATA_PERCHED); }
    public void setPerched(boolean perched) { this.entityData.set(DATA_PERCHED, perched); }
    public boolean isDaytime() { return this.brain.hasMemoryValue(BrainInit.IS_DAYTIME); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLIGHT_STATE, FLIGHT_GROUND);
        builder.define(DATA_PERCHED, false);
    }

    @SuppressWarnings("resource")
    private double altitudeAboveGround() {
        return this.getY() - this.level().getHeight(Heightmap.Types.WORLD_SURFACE, this.blockPosition().getX(), this.blockPosition().getZ());
    }

    private void tickFlightAutopilot() {
        if (this.isPassenger()) return;
        if (this.isPerched()) {
            this.setFlightState(FLIGHT_GROUND);
            this.setNoGravity(false);
            return;
        }
        boolean navActive = !this.getNavigation().isDone();
        if (this.onGround() && !this.isInWater()) {
            this.setFlightState(FLIGHT_GROUND);
            this.setNoGravity(false);
            if (!navActive && this.isDaytime() && this.random.nextInt(TAKEOFF_CHANCE) == 0) this.takeOff();
            return;
        }
        if (this.isInWater()) {
            this.setFlightState(FLIGHT_GROUND);
            this.setNoGravity(false);
            this.waterStruggle.tick();
            return;
        }
        int state = this.getFlightState();
        if (state == FLIGHT_GROUND) {
            this.setNoGravity(false);
            if (this.isDaytime() && this.altitudeAboveGround() > FALL_RECOVER_ALTITUDE) this.takeOff();
            return;
        }
        this.setNoGravity(true);
        if (!this.isDaytime()) {
            if (state != FLIGHT_DESCEND) this.beginDescend();
            this.applyCircle(-DESCEND_SINK);
            return;
        }
        if (state == FLIGHT_DESCEND) {
            this.beginThermal();
            return;
        }
        if (navActive) {
            this.setFlightState(FLIGHT_GLIDE);
            return;
        }
        double altitude = this.altitudeAboveGround();
        if (state == FLIGHT_TAKEOFF) {
            if (this.getY() - this.takeoffStartY >= TAKEOFF_THERMAL_GAIN) this.beginThermal();
            else this.applyTakeoff();
        } else if (state == FLIGHT_GLIDE) {
            if (altitude <= THERMAL_TRIGGER_ALTITUDE) this.beginThermal();
            else this.applyGlide();
        } else if (state == FLIGHT_THERMAL) {
            if (altitude >= SOAR_ALTITUDE) this.beginGlide();
            else this.applyCircle(THERMAL_ASCENT);
        }
    }

    private void takeOff() {
        this.takeoffStartY = this.getY();
        this.setYRot(this.getYRot() + (this.random.nextFloat() * 2.0F - 1.0F) * TAKEOFF_HEADING_JITTER);
        this.setFlightState(FLIGHT_TAKEOFF);
        this.setNoGravity(true);
        float yRotRad = this.getYRot() * ((float) Math.PI / 180F);
        this.setDeltaMovement(-Mth.sin(yRotRad) * TAKEOFF_SPEED, TAKEOFF_IMPULSE, Mth.cos(yRotRad) * TAKEOFF_SPEED);
    }

    private void applyTakeoff() {
        float yRotRad = this.getYRot() * ((float) Math.PI / 180F);
        this.setDeltaMovement(-Mth.sin(yRotRad) * TAKEOFF_SPEED, TAKEOFF_ASCENT, Mth.cos(yRotRad) * TAKEOFF_SPEED);
    }

    private void applyCircle(double verticalSpeed) {
        double rx = this.getX() - this.thermalCenterX;
        double rz = this.getZ() - this.thermalCenterZ;
        double tx = this.thermalClockwise ? -rz : rz;
        double tz = this.thermalClockwise ? rx : -rx;
        double len = Math.sqrt(tx * tx + tz * tz);
        if (len < 1.0E-4) {
            this.setupCircle();
            return;
        }
        this.setDeltaMovement(tx / len * THERMAL_SPEED, verticalSpeed, tz / len * THERMAL_SPEED);
        float targetYaw = (float) (Mth.atan2(-tx, tz) * (180F / Math.PI));
        float delta = Mth.wrapDegrees(targetYaw - this.getYRot());
        this.setYRot(this.getYRot() + Mth.clamp(delta, -3.0F, 3.0F));
    }

    private void setupCircle() {
        float yRotRad = this.getYRot() * ((float) Math.PI / 180F);
        double hx = -Mth.sin(yRotRad);
        double hz = Mth.cos(yRotRad);
        double radius = THERMAL_MIN_RADIUS + this.random.nextDouble() * (THERMAL_MAX_RADIUS - THERMAL_MIN_RADIUS);
        this.thermalClockwise = this.random.nextBoolean();
        double rx = this.thermalClockwise ? hz : -hz;
        double rz = this.thermalClockwise ? -hx : hx;
        this.thermalCenterX = this.getX() - rx * radius;
        this.thermalCenterZ = this.getZ() - rz * radius;
    }

    private void beginThermal() {
        this.setupCircle();
        this.setFlightState(FLIGHT_THERMAL);
    }

    private void beginDescend() {
        this.setupCircle();
        this.setFlightState(FLIGHT_DESCEND);
    }

    private void beginGlide() {
        this.glideTurnRate = (this.random.nextFloat() * 2.0F - 1.0F) * GLIDE_TURN_RATE_MAX;
        this.setFlightState(FLIGHT_GLIDE);
    }

    private void applyGlide() {
        this.setYRot(this.getYRot() + this.glideTurnRate);
        float yRotRad = this.getYRot() * ((float) Math.PI / 180F);
        this.setDeltaMovement(-Mth.sin(yRotRad) * GLIDE_SPEED, -GLIDE_SINK, Mth.cos(yRotRad) * GLIDE_SPEED);
    }

    private void updateNavigationMode() {
        boolean grounded = this.getFlightState() == FLIGHT_GROUND;
        if (grounded == this.landNavigation) return;
        this.getNavigation().stop();
        this.landNavigation = grounded;
        this.navigation = grounded ? new GroundPathNavigation(this, this.level()) : this.createNavigation(this.level());
    }

    // Flight physics
    @Override
    public void travel(Vec3 vec3) {
        if (this.isControlledByLocalInstance() && this.getFlightState() != FLIGHT_GROUND) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(AIR_DRAG));
        } else if (this.isControlledByLocalInstance() && this.isInWater()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(WATER_DRAG));
        } else super.travel(vec3);
    }

    // I should have added something to make it flap a little, while still making it feel heavy (so it doesn't just drift down like a chicken).
    @Override
    public boolean causeFallDamage(float distance, float multiplier, @NotNull DamageSource source) { return false; }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {}

    @Override
    public boolean isFlying() { return !this.onGround(); }

    @Override
    protected @NotNull PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return EntityInit.VULTURE.create(serverLevel);
    }

    @Override
    public boolean isFood(ItemStack itemStack) { return itemStack.is(TagInit.VULTURE_FOOD); }

    @Override
    public boolean canMate(Animal animal) {
        if (!(animal instanceof VultureEntity vultureEntity)) return false;
        return this.isInLove() && vultureEntity.isInLove();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ATTACK_DAMAGE, 8.0).add(Attributes.FLYING_SPEED, 0.6F)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean checkVultureEntitySpawnRules(EntityType<VultureEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.VULTURES_SPAWNABLE_ON);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isBaby() ? SoundEvents.POLAR_BEAR_AMBIENT_BABY : SoundEvents.POLAR_BEAR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) { return SoundEvents.POLAR_BEAR_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.POLAR_BEAR_DEATH; }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
    }
}