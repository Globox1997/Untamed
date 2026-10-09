package net.untamed.entity;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.ai.CarcassTracker;
import net.untamed.entity.ai.LandUtils;
import net.untamed.entity.ai.vulture.VultureAi;
import net.untamed.entity.ai.vulture.VultureMoveControl;
import net.untamed.entity.ai.vulture.WaterStruggle;
import net.untamed.init.BrainInit;
import net.untamed.init.EntityInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VultureEntity extends Animal implements FlyingAnimal {

    public static final int FLIGHT_GROUND = 0;
    public static final int FLIGHT_GLIDE = 1;
    public static final int FLIGHT_THERMAL = 2;
    public static final int FLIGHT_TAKEOFF = 3;
    public static final int FLIGHT_DESCEND = 4;

    public static final int ACTION_NONE = 0;
    public static final int ACTION_FEEDING = 1;
    public static final int ACTION_SQUABBLING = 2;

    private enum LandingPurpose {
        NONE, REST, ROOST, CARCASS
    }

    private static final float TAKEOFF_HEADING_JITTER = 30.0F;
    private static final double TAKEOFF_IMPULSE = 0.35;
    private static final double TAKEOFF_ASCENT = 0.06;
    private static final double TAKEOFF_SPEED = 0.12;
    private static final int TAKEOFF_CHANCE = 100;
    private static final double TAKEOFF_THERMAL_GAIN = 6.0;
    private static final double FALL_RECOVER_ALTITUDE = 4.0;

    private static final double SOAR_ALTITUDE = 30.0;
    private static final double THERMAL_TRIGGER_ALTITUDE = 15.0;
    private static final double GLIDE_SPEED = 0.25;
    private static final double GLIDE_SINK = 0.01;
    private static final float GLIDE_TURN_RATE_MAX = 0.3F;
    private static final double THERMAL_SPEED = 0.18;
    private static final double THERMAL_ASCENT = 0.015;
    private static final double THERMAL_MIN_RADIUS = 2.0;
    private static final double THERMAL_MAX_RADIUS = 8.0;
    private static final double DESCEND_SINK = 0.04;

    private static final int FLAP_DURATION = 30;
    private static final int MIN_FLAP_INTERVAL = 300;
    private static final int FLAP_INTERVAL_VARIATION = 300;
    private static final double FLAP_LIFT = 0.02;
    private static final int FLAP_SOUND_INTERVAL = 10;

    private static final int LOOKAHEAD_INTERVAL = 5;
    private static final int[] LOOKAHEAD_DISTANCES = {6, 12};
    private static final int OBSTACLE_CLEARANCE = 3;
    private static final int AVOID_TICKS = 40;
    private static final float AVOID_TURN_RATE = 4.0F;
    private static final double AVOID_CLIMB = 0.03;

    private static final int AIR_STUCK_TICKS = 60;
    private static final double AIR_STUCK_MOVE_SQR = 0.0009;

    private static final double HOME_RADIUS = 48.0;
    private static final double HOME_ADOPT_DISTANCE = 160.0;
    private static final float HOME_TURN_RATE = 2.0F;
    private static final int BABY_HOME_RADIUS = 12;

    private static final int MIN_GROUND_TICKS = 600;
    private static final int GROUND_TICKS_VARIATION = 1200;
    private static final int MIN_WAKE_GROUND_TICKS = 200;
    private static final int WAKE_GROUND_TICKS_VARIATION = 400;
    private static final int MIN_FLIGHT_TICKS = 1800;
    private static final int FLIGHT_TICKS_VARIATION = 1800;

    private static final int PLAN_INTERVAL = 20;
    private static final double APPROACH_DISTANCE = 12.0;
    private static final double SPIRAL_RADIUS = 5.0;
    private static final double SPIRAL_PULL = 0.05;
    private static final double FINAL_APPROACH_HEIGHT = 3.0;
    private static final double FINAL_APPROACH_SPEED = 0.15;
    private static final double LANDING_ARRIVE_DISTANCE = 0.7;
    private static final double LANDING_SINK = 0.06;
    private static final double HOLD_ALTITUDE = 12.0;
    private static final int LANDING_TIMEOUT = 1200;
    private static final int ROOST_SEARCH_RADIUS = 16;
    private static final int FLOCK_ROOST_RADIUS = 6;
    private static final double FLOCK_ROOST_RANGE = 48.0;
    private static final int PERCH_SEARCH_TRIES = 48;
    private static final int LANDING_SEARCH_TRIES = 16;
    private static final int REST_LANDING_RADIUS = 12;
    private static final double PENDING_PERCH_DIST_SQR = 6.25;

    private static final double CARCASS_SPOT_RANGE = 48.0;
    private static final double CARCASS_SOCIAL_RANGE = 96.0;
    private static final double CARCASS_GROUND_RANGE = 16.0;
    private static final int CARCASS_LANDING_RADIUS = 2;
    private static final double RIVAL_RANGE = 8.0;
    private static final int RIVAL_CHECK_INTERVAL = 10;

    private static final double HURT_THREAT_RANGE = 16.0;
    private static final double PLAYER_THREAT_RANGE = 8.0;
    private static final double SNEAKING_THREAT_RANGE = 4.0;
    private static final double RIVAL_THREAT_RANGE = 6.0;
    private static final int SCARED_TICKS = 300;

    private static final long ACTIVE_START_TICK = 1000L;
    private static final long ACTIVE_END_TICK = 11000L;

    private static final float BANK_PER_DEGREE = 0.15F;
    private static final float MAX_BANK = 0.6F;
    private static final float BANK_SMOOTHING = 0.15F;

    private static final double AIR_DRAG = 0.98;
    private static final double WATER_DRAG = 0.85;

    private static final EntityDataAccessor<Integer> DATA_FLIGHT_STATE = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PERCHED = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FLAPPING = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(VultureEntity.class, EntityDataSerializers.INT);

    private boolean landNavigation;
    private double thermalCenterX, thermalCenterZ;
    private boolean thermalClockwise;
    private float glideTurnRate;
    private double takeoffStartY;
    private final WaterStruggle waterStruggle = new WaterStruggle(this);

    @Nullable
    private BlockPos homePos;
    @Nullable
    private BlockPos perchPos;
    @Nullable
    private BlockPos pendingPerch;
    private int groundTicks;
    private int flightTicks;
    private int flightDuration;
    private int flapTicks;
    private int nextFlapIn;
    private int avoidTicks;
    private float avoidTurn;
    private int airStuckTicks;
    private double lastAirX, lastAirY, lastAirZ;
    private long scaredUntil;

    private LandingPurpose landingPurpose = LandingPurpose.NONE;
    @Nullable
    private BlockPos landingTarget;
    private int landingTicks;
    private boolean rivalAtTarget;
    @Nullable
    private CarcassTracker.Carcass carcass;

    private float bank;
    private float bankO;

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
        if (this.isActiveTime()) this.brain.setMemory(BrainInit.IS_DAYTIME, Unit.INSTANCE);
        else this.brain.eraseMemory(BrainInit.IS_DAYTIME);
        if (this.homePos == null || this.homePos.distSqr(this.blockPosition()) > HOME_ADOPT_DISTANCE * HOME_ADOPT_DISTANCE) {
            this.homePos = this.blockPosition();
        }
        if (this.isBaby()) this.restrictTo(this.homePos, BABY_HOME_RADIUS);
        else if (this.hasRestriction()) this.clearRestriction();
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

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.bankO = this.bank;
            float target = this.getFlightState() == FLIGHT_GROUND ? 0.0F
                    : Mth.clamp(-Mth.wrapDegrees(this.getYRot() - this.yRotO) * BANK_PER_DEGREE, -MAX_BANK, MAX_BANK);
            this.bank += (target - this.bank) * BANK_SMOOTHING;
        }
    }

    public float getBank(float partialTick) { return Mth.lerp(partialTick, this.bankO, this.bank); }

    public int getFlightState() { return this.entityData.get(DATA_FLIGHT_STATE); }
    private void setFlightState(int state) { this.entityData.set(DATA_FLIGHT_STATE, state); }
    public boolean isPerched() { return this.entityData.get(DATA_PERCHED); }
    public boolean isFlapping() { return this.entityData.get(DATA_FLAPPING); }
    private void setFlapping(boolean flapping) { this.entityData.set(DATA_FLAPPING, flapping); }
    public int getAction() { return this.entityData.get(DATA_ACTION); }
    public void setAction(int action) { this.entityData.set(DATA_ACTION, action); }
    @Nullable
    public BlockPos getPerchPos() { return this.perchPos; }
    @Nullable
    public CarcassTracker.Carcass getCarcass() { return this.carcass; }

    @SuppressWarnings("resource")
    public boolean isActiveTime() {
        long dayTime = this.level().getDayTime() % 24000L;
        return dayTime >= ACTIVE_START_TICK && dayTime < ACTIVE_END_TICK;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FLIGHT_STATE, FLIGHT_GROUND);
        builder.define(DATA_PERCHED, false);
        builder.define(DATA_FLAPPING, false);
        builder.define(DATA_ACTION, ACTION_NONE);
    }

    public boolean canWander() {
        return this.getFlightState() == FLIGHT_GROUND && !this.isPerched() && this.getAction() == ACTION_NONE;
    }

    private boolean canFly() {
        return !this.isBaby() && !this.isLeashed() && !this.isPassenger();
    }

    public void perchAt(BlockPos pos) {
        this.entityData.set(DATA_PERCHED, true);
        this.perchPos = pos;
        this.homePos = pos;
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        this.brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    public void releasePerch() {
        if (this.isPerched()) {
            this.groundTicks = MIN_WAKE_GROUND_TICKS + this.random.nextInt(WAKE_GROUND_TICKS_VARIATION);
        }
        this.entityData.set(DATA_PERCHED, false);
        this.perchPos = null;
    }

    public void finishFeeding() {
        if (this.carcass == null || !this.carcass.isValid((ServerLevel) this.level())) {
            this.carcass = null;
            this.groundTicks = MIN_WAKE_GROUND_TICKS + this.random.nextInt(WAKE_GROUND_TICKS_VARIATION);
        }
    }

    public void abandonCarcass() {
        this.carcass = null;
        this.groundTicks = MIN_WAKE_GROUND_TICKS + this.random.nextInt(WAKE_GROUND_TICKS_VARIATION);
    }

    @Nullable
    public LivingEntity findThreat() {
        LivingEntity attacker = this.brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).orElse(null);
        if (attacker != null && attacker.isAlive() && this.distanceToSqr(attacker) < HURT_THREAT_RANGE * HURT_THREAT_RANGE) {
            return attacker;
        }
        return this.brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .flatMap(entities -> entities.findClosest(this::isThreat))
                .orElse(null);
    }

    private boolean isThreat(LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player) || this.isFood(player.getMainHandItem()) || this.isFood(player.getOffhandItem())) {
                return false;
            }
            double range = player.isDiscrete() ? SNEAKING_THREAT_RANGE : PLAYER_THREAT_RANGE;
            return this.distanceToSqr(player) < range * range;
        }
        return entity.getType().is(TagInit.VULTURE_RIVALS) && this.distanceToSqr(entity) < RIVAL_THREAT_RANGE * RIVAL_THREAT_RANGE;
    }

    public boolean takeOffAwayFrom(Vec3 threat) {
        if (!this.canFly() || !this.level().canSeeSky(this.blockPosition())) {
            return false;
        }
        this.scaredUntil = this.level().getGameTime() + SCARED_TICKS;
        this.takeOff((float) (Mth.atan2(threat.x - this.getX(), this.getZ() - threat.z) * (180F / Math.PI)));
        return true;
    }

    @SuppressWarnings("resource")
    private double altitudeAboveGround() {
        return this.getY() - this.level().getHeight(Heightmap.Types.WORLD_SURFACE, this.blockPosition().getX(), this.blockPosition().getZ());
    }

    @SuppressWarnings("resource")
    private void tickFlightAutopilot() {
        if (this.isPassenger()) return;
        if (this.isPerched()) {
            this.setFlightState(FLIGHT_GROUND);
            this.setNoGravity(false);
            this.setFlapping(false);
            return;
        }
        if (this.isInWater()) {
            this.setFlightState(FLIGHT_GROUND);
            this.setNoGravity(false);
            this.setFlapping(false);
            this.clearLanding();
            this.waterStruggle.tick();
            return;
        }
        if (this.onGround()) {
            this.tickGround();
            return;
        }
        int state = this.getFlightState();
        if (state == FLIGHT_GROUND) {
            this.setNoGravity(false);
            this.setFlapping(false);
            if (this.canFly() && this.isActiveTime() && this.altitudeAboveGround() > FALL_RECOVER_ALTITUDE && this.level().canSeeSky(this.blockPosition())) {
                this.takeOff(this.getYRot());
            }
            return;
        }
        if (!this.canFly()) {
            this.dropToGround();
            return;
        }
        this.setNoGravity(true);
        if (!this.getNavigation().isDone()) this.getNavigation().stop();
        this.flightTicks++;
        if (this.isAirStuck()) {
            this.dropToGround();
            return;
        }
        this.handleCollisions(state);
        if (this.flapTicks > 0) this.flapTicks--;
        if (this.avoidTicks > 0) this.avoidTicks--;
        if (this.landingPurpose == LandingPurpose.NONE && this.tickCount % PLAN_INTERVAL == 0) this.planLanding();
        if (this.landingPurpose != LandingPurpose.NONE) {
            this.tickLanding();
        } else {
            this.tickFreeFlight(this.getFlightState());
        }
        this.setFlapping(this.getFlightState() == FLIGHT_TAKEOFF || this.flapTicks > 0 || this.avoidTicks > 0);
        if (this.isFlapping() && this.tickCount % FLAP_SOUND_INTERVAL == 0) {
            this.playSound(SoundInit.VULTURE_FLAP_EVENT, 0.4F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    private void tickGround() {
        if (this.getFlightState() != FLIGHT_GROUND || this.landingPurpose != LandingPurpose.NONE) {
            this.onLanded();
        }
        this.setFlightState(FLIGHT_GROUND);
        this.setNoGravity(false);
        this.setFlapping(false);
        this.flightTicks = 0;
        if (this.pendingPerch != null) {
            if (!this.isActiveTime() && this.distanceToSqr(Vec3.atBottomCenterOf(this.pendingPerch)) < PENDING_PERCH_DIST_SQR) {
                this.perchAt(this.blockPosition());
            }
            this.pendingPerch = null;
        }
        if (this.groundTicks > 0) this.groundTicks--;
        if (this.carcass == null && this.isActiveTime() && !this.isBaby() && this.tickCount % PLAN_INTERVAL == 0) {
            this.carcass = CarcassTracker.findNearest((ServerLevel) this.level(), this.position(), CARCASS_GROUND_RANGE);
        }
        if (this.canFly() && this.isActiveTime() && !this.isInLove() && this.groundTicks <= 0 && this.carcass == null
                && this.getAction() == ACTION_NONE && this.getNavigation().isDone() && this.level().canSeeSky(this.blockPosition())
                && this.random.nextInt(TAKEOFF_CHANCE) == 0) {
            this.takeOff(this.getYRot() + (this.random.nextFloat() * 2.0F - 1.0F) * TAKEOFF_HEADING_JITTER);
        }
    }

    private void onLanded() {
        if (this.landingPurpose == LandingPurpose.ROOST && this.landingTarget != null) {
            this.pendingPerch = this.landingTarget;
        }
        if (this.landingPurpose != LandingPurpose.CARCASS) {
            this.groundTicks = MIN_GROUND_TICKS + this.random.nextInt(GROUND_TICKS_VARIATION);
        }
        this.clearLanding();
        this.avoidTicks = 0;
        this.flapTicks = 0;
    }

    private void dropToGround() {
        this.setFlightState(FLIGHT_GROUND);
        this.setNoGravity(false);
        this.setFlapping(false);
    }

    private boolean isAirStuck() {
        double dx = this.getX() - this.lastAirX;
        double dy = this.getY() - this.lastAirY;
        double dz = this.getZ() - this.lastAirZ;
        this.lastAirX = this.getX();
        this.lastAirY = this.getY();
        this.lastAirZ = this.getZ();
        this.airStuckTicks = dx * dx + dy * dy + dz * dz < AIR_STUCK_MOVE_SQR ? this.airStuckTicks + 1 : 0;
        return this.airStuckTicks > AIR_STUCK_TICKS;
    }

    @SuppressWarnings("resource")
    private void handleCollisions(int state) {
        if (this.horizontalCollision) {
            if (state == FLIGHT_GLIDE || state == FLIGHT_TAKEOFF) {
                this.setYRot(this.getYRot() + 180.0F + (this.random.nextFloat() * 2.0F - 1.0F) * TAKEOFF_HEADING_JITTER);
                this.glideTurnRate = (this.random.nextFloat() * 2.0F - 1.0F) * GLIDE_TURN_RATE_MAX;
            } else {
                this.thermalClockwise = !this.thermalClockwise;
                this.setupCircle();
            }
        }
        boolean blockedAbove = this.verticalCollision && this.getDeltaMovement().y >= 0.0D;
        if ((state == FLIGHT_TAKEOFF || state == FLIGHT_THERMAL) && (blockedAbove || !this.level().canSeeSky(this.blockPosition()))) {
            this.beginGlide();
        }
    }

    @SuppressWarnings("resource")
    private void tickFreeFlight(int state) {
        double altitude = this.altitudeAboveGround();
        boolean openSky = this.level().canSeeSky(this.blockPosition());
        if (state == FLIGHT_TAKEOFF) {
            if (this.getY() - this.takeoffStartY >= TAKEOFF_THERMAL_GAIN && openSky) this.beginThermal();
            else this.applyTakeoff();
        } else if (state == FLIGHT_GLIDE) {
            if (altitude <= THERMAL_TRIGGER_ALTITUDE && openSky && this.avoidTicks <= 0) this.beginThermal();
            else this.applyGlide();
        } else if (state == FLIGHT_THERMAL) {
            if (altitude >= SOAR_ALTITUDE) this.beginGlide();
            else this.applyCircle(THERMAL_ASCENT);
        } else {
            this.beginThermal();
        }
    }

    private void takeOff(float yaw) {
        this.releasePerch();
        this.clearLanding();
        this.getNavigation().stop();
        this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        this.setAction(ACTION_NONE);
        this.takeoffStartY = this.getY();
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.setFlightState(FLIGHT_TAKEOFF);
        this.setNoGravity(true);
        this.flightTicks = 0;
        this.flightDuration = MIN_FLIGHT_TICKS + this.random.nextInt(FLIGHT_TICKS_VARIATION);
        this.nextFlapIn = MIN_FLAP_INTERVAL + this.random.nextInt(FLAP_INTERVAL_VARIATION);
        this.airStuckTicks = 0;
        float yRotRad = yaw * ((float) Math.PI / 180F);
        this.setDeltaMovement(-Mth.sin(yRotRad) * TAKEOFF_SPEED, TAKEOFF_IMPULSE, Mth.cos(yRotRad) * TAKEOFF_SPEED);
        this.playSound(SoundInit.VULTURE_FLAP_EVENT, 0.8F, 0.9F + this.random.nextFloat() * 0.2F);
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
        this.faceMovement(3.0F);
    }

    private void applySpiral(double centerX, double centerZ, double verticalSpeed) {
        this.thermalCenterX = centerX;
        this.thermalCenterZ = centerZ;
        double rx = this.getX() - centerX;
        double rz = this.getZ() - centerZ;
        double radius = Math.sqrt(rx * rx + rz * rz);
        if (radius < 1.0E-4) {
            this.setupCircle();
            return;
        }
        double tx = this.thermalClockwise ? -rz : rz;
        double tz = this.thermalClockwise ? rx : -rx;
        double pull = radius > SPIRAL_RADIUS ? SPIRAL_PULL : 0.0D;
        double vx = tx / radius * THERMAL_SPEED - rx / radius * pull;
        double vz = tz / radius * THERMAL_SPEED - rz / radius * pull;
        this.setDeltaMovement(vx, verticalSpeed, vz);
        this.faceMovement(4.0F);
    }

    private void faceMovement(float maxTurn) {
        Vec3 movement = this.getDeltaMovement();
        if (movement.horizontalDistanceSqr() < 1.0E-6) return;
        float targetYaw = (float) (Mth.atan2(-movement.x, movement.z) * (180F / Math.PI));
        float delta = Mth.wrapDegrees(targetYaw - this.getYRot());
        this.setYRot(this.getYRot() + Mth.clamp(delta, -maxTurn, maxTurn));
    }

    private void setupCircle() {
        float yRotRad = this.getYRot() * ((float) Math.PI / 180F);
        double hx = -Mth.sin(yRotRad);
        double hz = Mth.cos(yRotRad);
        double radius = THERMAL_MIN_RADIUS + this.random.nextDouble() * (THERMAL_MAX_RADIUS - THERMAL_MIN_RADIUS);
        double rx = this.thermalClockwise ? hz : -hz;
        double rz = this.thermalClockwise ? -hx : hx;
        this.thermalCenterX = this.getX() - rx * radius;
        this.thermalCenterZ = this.getZ() - rz * radius;
    }

    private void beginThermal() {
        this.thermalClockwise = this.random.nextBoolean();
        this.setupCircle();
        this.setFlightState(FLIGHT_THERMAL);
    }

    private void beginGlide() {
        this.glideTurnRate = (this.random.nextFloat() * 2.0F - 1.0F) * GLIDE_TURN_RATE_MAX;
        this.setFlightState(FLIGHT_GLIDE);
    }

    private void applyGlide() {
        if (this.tickCount % LOOKAHEAD_INTERVAL == 0 && this.avoidTicks <= 0 && this.isObstacleAhead()) {
            this.avoidTicks = AVOID_TICKS;
            this.avoidTurn = this.lowerSide() * AVOID_TURN_RATE;
        }
        if (this.avoidTicks > 0) {
            this.setYRot(this.getYRot() + this.avoidTurn);
        } else if (!this.steerTowardsHome()) {
            this.setYRot(this.getYRot() + this.glideTurnRate);
        }
        double vertical = -GLIDE_SINK;
        if (this.avoidTicks > 0) {
            vertical = AVOID_CLIMB;
        } else if (--this.nextFlapIn <= 0) {
            this.nextFlapIn = MIN_FLAP_INTERVAL + this.random.nextInt(FLAP_INTERVAL_VARIATION);
            this.flapTicks = FLAP_DURATION;
        }
        if (this.flapTicks > 0 && this.avoidTicks <= 0) {
            vertical = FLAP_LIFT;
        }
        float yRotRad = this.getYRot() * ((float) Math.PI / 180F);
        this.setDeltaMovement(-Mth.sin(yRotRad) * GLIDE_SPEED, vertical, Mth.cos(yRotRad) * GLIDE_SPEED);
    }

    private boolean steerTowardsHome() {
        if (this.homePos == null) return false;
        double dx = this.homePos.getX() + 0.5D - this.getX();
        double dz = this.homePos.getZ() + 0.5D - this.getZ();
        if (dx * dx + dz * dz < HOME_RADIUS * HOME_RADIUS) return false;
        this.turnTowards(dx, dz, HOME_TURN_RATE);
        return true;
    }

    private void turnTowards(double dx, double dz, float maxTurn) {
        float targetYaw = (float) (Mth.atan2(-dx, dz) * (180F / Math.PI));
        float delta = Mth.wrapDegrees(targetYaw - this.getYRot());
        this.setYRot(this.getYRot() + Mth.clamp(delta, -maxTurn, maxTurn));
    }

    @SuppressWarnings("resource")
    private boolean isObstacleAhead() {
        float yRotRad = this.getYRot() * ((float) Math.PI / 180F);
        double hx = -Mth.sin(yRotRad);
        double hz = Mth.cos(yRotRad);
        for (int distance : LOOKAHEAD_DISTANCES) {
            int x = Mth.floor(this.getX() + hx * distance);
            int z = Mth.floor(this.getZ() + hz * distance);
            if (this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + OBSTACLE_CLEARANCE > this.getY()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("resource")
    private float lowerSide() {
        int distance = LOOKAHEAD_DISTANCES[LOOKAHEAD_DISTANCES.length - 1];
        float left = (this.getYRot() - 60.0F) * ((float) Math.PI / 180F);
        float right = (this.getYRot() + 60.0F) * ((float) Math.PI / 180F);
        int leftHeight = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                Mth.floor(this.getX() - Mth.sin(left) * distance), Mth.floor(this.getZ() + Mth.cos(left) * distance));
        int rightHeight = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                Mth.floor(this.getX() - Mth.sin(right) * distance), Mth.floor(this.getZ() + Mth.cos(right) * distance));
        return leftHeight < rightHeight ? -1.0F : 1.0F;
    }

    private void planLanding() {
        ServerLevel level = (ServerLevel) this.level();
        if (!this.isActiveTime()) {
            this.startLanding(LandingPurpose.ROOST, this.findRoostTarget(level));
            return;
        }
        if (this.isInLove()) {
            this.startLanding(LandingPurpose.REST, LandUtils.findOpenLanding(level, this.random, this.blockPosition(), REST_LANDING_RADIUS, LANDING_SEARCH_TRIES));
            return;
        }
        CarcassTracker.Carcass found = this.findCarcassFromAir(level);
        if (found != null) {
            this.carcass = found;
            BlockPos spot = LandUtils.findOpenLanding(level, this.random, found.getPos(), CARCASS_LANDING_RADIUS, LANDING_SEARCH_TRIES);
            this.startLanding(LandingPurpose.CARCASS, spot != null ? spot : found.getPos());
            return;
        }
        if (this.flightTicks > this.flightDuration) {
            BlockPos center = this.homePos != null ? this.homePos : this.blockPosition();
            this.startLanding(LandingPurpose.REST, LandUtils.findOpenLanding(level, this.random, center, REST_LANDING_RADIUS, LANDING_SEARCH_TRIES));
        }
    }

    @Nullable
    private CarcassTracker.Carcass findCarcassFromAir(ServerLevel level) {
        if (this.carcass != null && this.carcass.isValid(level)) {
            return this.carcass;
        }
        this.carcass = null;
        CarcassTracker.Carcass found = CarcassTracker.findNearest(level, this.position(), CARCASS_SPOT_RANGE);
        if (found != null) {
            return found;
        }
        List<VultureEntity> others = level.getEntitiesOfClass(VultureEntity.class, this.getBoundingBox().inflate(CARCASS_SOCIAL_RANGE, SOAR_ALTITUDE, CARCASS_SOCIAL_RANGE),
                other -> other != this && other.carcass != null && other.carcass.isValid(level));
        return others.isEmpty() ? null : others.get(0).carcass;
    }

    @Nullable
    private BlockPos findRoostTarget(ServerLevel level) {
        BlockPos center = this.homePos != null ? this.homePos : this.blockPosition();
        int radius = ROOST_SEARCH_RADIUS;
        for (VultureEntity other : level.getEntitiesOfClass(VultureEntity.class, this.getBoundingBox().inflate(FLOCK_ROOST_RANGE, SOAR_ALTITUDE, FLOCK_ROOST_RANGE),
                other -> other != this)) {
            BlockPos flockSpot = other.isPerched() ? other.perchPos : other.landingPurpose == LandingPurpose.ROOST ? other.landingTarget : null;
            if (flockSpot != null) {
                center = flockSpot;
                radius = FLOCK_ROOST_RADIUS;
                break;
            }
        }
        BlockPos perch = LandUtils.findAerialPerch(level, this.random, center, radius, PERCH_SEARCH_TRIES);
        return perch != null ? perch : LandUtils.findOpenLanding(level, this.random, center, radius, LANDING_SEARCH_TRIES);
    }

    private void startLanding(LandingPurpose purpose, @Nullable BlockPos target) {
        this.landingPurpose = purpose;
        this.landingTarget = target;
        this.landingTicks = 0;
        this.rivalAtTarget = false;
        this.thermalClockwise = this.random.nextBoolean();
        this.setFlightState(FLIGHT_DESCEND);
        if (target == null) this.setupCircle();
    }

    private void clearLanding() {
        this.landingPurpose = LandingPurpose.NONE;
        this.landingTarget = null;
        this.landingTicks = 0;
        this.rivalAtTarget = false;
    }

    @SuppressWarnings("resource")
    private void tickLanding() {
        ServerLevel level = (ServerLevel) this.level();
        this.setFlightState(FLIGHT_DESCEND);
        if (this.landingPurpose == LandingPurpose.CARCASS && (this.carcass == null || !this.carcass.isValid(level))) {
            this.carcass = null;
            this.clearLanding();
            this.beginThermal();
            return;
        }
        if (this.landingPurpose == LandingPurpose.REST && this.isActiveTime() && this.carcass == null && this.tickCount % PLAN_INTERVAL == 0) {
            CarcassTracker.Carcass found = this.findCarcassFromAir(level);
            if (found != null) {
                this.carcass = found;
                this.startLanding(LandingPurpose.CARCASS, found.getPos());
            }
        }
        if (++this.landingTicks > LANDING_TIMEOUT) {
            this.landingTarget = null;
        }
        if (this.landingTarget == null) {
            this.applyCircle(-DESCEND_SINK);
            return;
        }
        Vec3 target = Vec3.atBottomCenterOf(this.landingTarget);
        double dx = target.x - this.getX();
        double dz = target.z - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double height = this.getY() - target.y;

        if (horizontal > APPROACH_DISTANCE) {
            this.turnTowards(dx, dz, HOME_TURN_RATE * 2.0F);
            double vertical = this.isObstacleAhead() || this.altitudeAboveGround() < FINAL_APPROACH_HEIGHT * 2.0D ? AVOID_CLIMB : -GLIDE_SINK;
            this.avoidTicks = vertical > 0.0D ? 2 : 0;
            float yRotRad = this.getYRot() * ((float) Math.PI / 180F);
            this.setDeltaMovement(-Mth.sin(yRotRad) * GLIDE_SPEED, vertical, Mth.cos(yRotRad) * GLIDE_SPEED);
            return;
        }
        boolean hold = false;
        if (this.landingPurpose == LandingPurpose.CARCASS) {
            if (this.tickCount % RIVAL_CHECK_INTERVAL == 0) {
                this.rivalAtTarget = !level.getEntitiesOfClass(LivingEntity.class, new AABB(this.landingTarget).inflate(RIVAL_RANGE),
                        entity -> entity.isAlive() && entity.getType().is(TagInit.VULTURE_RIVALS)).isEmpty();
            }
            hold = this.rivalAtTarget || level.getGameTime() < this.scaredUntil;
        }
        if (hold) {
            this.applySpiral(target.x, target.z, height < HOLD_ALTITUDE ? THERMAL_ASCENT * 2.0D : 0.0D);
            return;
        }
        if (height > FINAL_APPROACH_HEIGHT) {
            this.applySpiral(target.x, target.z, -LANDING_SINK);
            return;
        }
        Vec3 toTarget = target.subtract(this.position());
        double distance = toTarget.length();
        if (distance < LANDING_ARRIVE_DISTANCE || (this.horizontalCollision && horizontal < FINAL_APPROACH_HEIGHT)) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.2D, 0.0D, 0.2D));
            this.dropToGround();
            return;
        }
        this.flapTicks = Math.max(this.flapTicks, 2);
        this.setDeltaMovement(toTarget.scale(FINAL_APPROACH_SPEED / distance));
        this.faceMovement(10.0F);
    }

    private void updateNavigationMode() {
        boolean grounded = this.getFlightState() == FLIGHT_GROUND;
        if (grounded == this.landNavigation) return;
        this.getNavigation().stop();
        this.landNavigation = grounded;
        this.navigation = grounded ? new GroundPathNavigation(this, this.level()) : this.createNavigation(this.level());
    }

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

    @Override
    public boolean causeFallDamage(float distance, float multiplier, @NotNull DamageSource source) { return false; }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull BlockState state, @NotNull BlockPos pos) {}

    @Override
    public boolean isFlying() { return this.getFlightState() != FLIGHT_GROUND; }

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
        VultureEntity baby = EntityInit.VULTURE.create(serverLevel);
        if (baby != null) {
            baby.homePos = this.homePos;
        }
        return baby;
    }

    @Override
    public boolean isFood(ItemStack itemStack) { return itemStack.is(TagInit.VULTURE_FOOD); }

    @Override
    public boolean canMate(Animal animal) {
        if (!(animal instanceof VultureEntity vultureEntity)) return false;
        return this.isInLove() && vultureEntity.isInLove();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean checkVultureEntitySpawnRules(EntityType<VultureEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA) && !holder.is(BiomeTags.IS_BADLANDS)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.VULTURES_SPAWNABLE_ON);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundInit.VULTURE_IDLE_EVENT; }

    @Override
    public int getAmbientSoundInterval() { return 240; }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) { return SoundInit.VULTURE_HURT_EVENT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundInit.VULTURE_DEATH_EVENT; }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        this.homePos = this.blockPosition();
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        if (this.homePos != null) {
            compoundTag.put("HomePos", NbtUtils.writeBlockPos(this.homePos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.homePos = NbtUtils.readBlockPos(compoundTag, "HomePos").orElse(null);
    }
}
