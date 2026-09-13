package net.untamed.entity;

import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
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
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.DifficultyInstance;
import net.untamed.entity.ai.WaterUtils;
import net.untamed.entity.ai.capybara.CapybaraAi;
import net.untamed.entity.ai.capybara.CapybaraMoveControl;
import net.untamed.init.BrainInit;
import net.untamed.init.EntityInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CapybaraEntity extends Animal {

    private boolean inWaterBody;
    private int waterExitHopCooldown;
    private int soakTicks;
    private float navVerticalIntent;
    @Nullable
    private BlockPos waterAnchor;

    // Aquatic physics tunables

    private static final float WATER_DRAG = 0.9F;              // per-tick velocity scale in water
    private static final float ASCENT_ACCEL = 0.08F;           // buoyancy push while eyes submerged
    private static final float ASCENT_MAX_SPEED = 0.06F;
    private static final float SURFACE_Y_DAMP = 0.6F;          // vertical damping while floating level
    private static final float NAV_DESCENT = 0.04F;            // gentle descent toward a path node below
    private static final float NAV_DESCENT_MAX = 0.08F;
    private static final float DIVE_DESCENT_SUBMERGED = 0.05F;
    private static final float DIVE_DESCENT_SURFACE = 0.03F;
    private static final float DIVE_MAX_DESCENT = 0.15F;
    private static final float SOAK_DESCENT = 0.02F;
    private static final float SOAK_MAX_DESCENT = 0.04F;
    private static final float CEILING_DRAG = 0.9F;            // drift under bridges/decks
    private static final float HOP_VERTICAL = 0.4F;
    private static final float HOP_HORIZONTAL = 0.25F;
    private static final float HOP_GRAVITY = 0.02F;
    private static final int HOP_COOLDOWN_TICKS = 12;
    private static final float JAMMED_VELOCITY_SQR = 0.004F;    // hop gate: collision-killed speed
    private static final float FORWARD_INPUT_EPSILON = 0.02F;  // hop gate: zza threshold

    private static final EntityDataAccessor<Boolean> DATA_DIVING =
            SynchedEntityData.defineId(CapybaraEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_RESTING =
            SynchedEntityData.defineId(CapybaraEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> DATA_RIVERBED =
            SynchedEntityData.defineId(CapybaraEntity.class, EntityDataSerializers.BOOLEAN);

    public CapybaraEntity(EntityType<? extends CapybaraEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
        this.moveControl = new CapybaraMoveControl(this);
        this.lookControl = new SmoothSwimmingLookControl(this, 20);
    }

    // Brain

    @Override
    protected Brain.@NotNull Provider<CapybaraEntity> brainProvider() {
        return CapybaraAi.brainProvider();
    }

    @Override
    protected @NotNull Brain<?> makeBrain(Dynamic<?> dynamic) {
        return CapybaraAi.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Brain<CapybaraEntity> getBrain() {
        return (Brain<CapybaraEntity>) super.getBrain();
    }

    @Override
    @SuppressWarnings("resource")
    protected void customServerAiStep() {
        if (this.isFloating()) this.inWaterBody = true;
        else if (!this.isInWater() || this.onGround()) this.inWaterBody = false;
        if (this.inWaterBody) this.brain.setMemory(MemoryModuleType.IS_IN_WATER, Unit.INSTANCE);
        else this.brain.eraseMemory(MemoryModuleType.IS_IN_WATER);
        if (this.isInWater()) this.waterAnchor = this.blockPosition();
        this.updateRiverbedState();
        this.level().getProfiler().push("capybaraBrain");
        this.getBrain().tick((ServerLevel) this.level(), this);
        this.setResting(this.brain.hasMemoryValue(BrainInit.IDLE_REST));
        this.level().getProfiler().pop();
        this.level().getProfiler().push("capybaraActivityUpdate");
        CapybaraAi.updateActivity(this);
        this.level().getProfiler().pop();
        super.customServerAiStep();
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    @Override
    @SuppressWarnings("resource")
    public boolean hurt(DamageSource damageSource, float amount) {
        if (!this.level().isClientSide && !this.isNoAi() && damageSource.getEntity() != null)
            this.brain.setMemoryWithExpiry(MemoryModuleType.IS_PANICKING, true, 200L);
        return super.hurt(damageSource, amount);
    }

    // Soaking (idle shallow dip)

    public void startSoak() {
        this.soakTicks = this.random.nextInt(10, 30);
        this.setDiving(true);
    }

    public void endSoak() {
        this.soakTicks = 0;
        this.setDiving(false);
    }

    // Diving state (synced for animations)

    public boolean isDiving() { return this.entityData.get(DATA_DIVING); }

    public void setDiving(boolean diving) { this.entityData.set(DATA_DIVING, diving); }

    public void setNavVerticalIntent(float intent) { this.navVerticalIntent = intent; }

    public @Nullable BlockPos getWaterAnchor() { return this.waterAnchor; }

    // Resting state

    public boolean isResting() { return this.entityData.get(DATA_RESTING); }

    private void setResting(boolean resting) { this.entityData.set(DATA_RESTING, resting); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DIVING, false);
        builder.define(DATA_RESTING, false);
        builder.define(DATA_RIVERBED, false);
    }

    // Aquatic physics: boat-like float, active dive, water-exit hop

    @SuppressWarnings("resource")
    public boolean isFloating() {
        if (!this.isInWater()) return false;
        if (!this.onGround()) return true;
        return this.level().getFluidState(this.blockPosition().above()).is(FluidTags.WATER);
    }

    public boolean isRiverbed() { return this.entityData.get(DATA_RIVERBED); }

    @Override
    public void travel(Vec3 vec3) {
        if (this.waterExitHopCooldown > 0) this.waterExitHopCooldown--;
        if (this.isControlledByLocalInstance() && this.isFloating()) {
            this.moveRelative(this.getSpeed(), vec3);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(WATER_DRAG));
            this.applyWaterPhysics();
        } else super.travel(vec3);
    }

    private void applyWaterPhysics() {
        Vec3 movement = this.getDeltaMovement();
        if (this.waterExitHopCooldown > 0)
            this.setDeltaMovement(movement.x, movement.y - HOP_GRAVITY, movement.z);
        else if (this.isDiving()) this.tickDive(movement);
        else if (this.tryWaterExitHop()); //Me da toc este warning pero ya ni modo :(
        else this.tickBuoyancy(movement);
    }

    private void tickDive(Vec3 movement) {
        if (this.isRiverbed()) {
            this.setDeltaMovement(movement.x, movement.y * 0.6, movement.z);
            return;
        }
        if (this.soakTicks > 0) {
            this.soakTicks--;
            if (this.soakTicks == 0) this.setDiving(false);
            this.setDeltaMovement(movement.x, Math.max(movement.y - SOAK_DESCENT, -SOAK_MAX_DESCENT), movement.z);
        } else {
            // Full dive: stronger once submerged, gentler while crossing the surface
            float descent = this.isEyesUnderwater() ? DIVE_DESCENT_SUBMERGED : DIVE_DESCENT_SURFACE;
            this.setDeltaMovement(movement.x, Math.max(movement.y - descent, -DIVE_MAX_DESCENT), movement.z);
        }
    }

    private void updateRiverbedState() {
        boolean riverbed = false;
        if (this.isInWater()) {
            double floorY = this.getY() - WaterUtils.oceanFloorBelow(this);
            riverbed = this.isRiverbed()
                    ? floorY <= 0.5
                    : floorY <= 0.3;
        }
        this.entityData.set(DATA_RIVERBED, riverbed);
    }

    @SuppressWarnings("resource")
    private void tickBuoyancy(Vec3 movement) {
        if (!this.isEyesUnderwater()) {
            if (this.navVerticalIntent < 0.0F)
                this.setDeltaMovement(movement.x, Math.max(movement.y - NAV_DESCENT, -NAV_DESCENT_MAX), movement.z);
            else this.setDeltaMovement(movement.x, movement.y * SURFACE_Y_DAMP, movement.z);
            return;
        }
        BlockPos headBlock = BlockPos.containing(this.getX(), this.getBoundingBox().maxY + 0.1, this.getZ());
        if (!this.level().getBlockState(headBlock).getCollisionShape(this.level(), headBlock).isEmpty()) {
            this.setDeltaMovement(movement.x * CEILING_DRAG, Math.min(movement.y, 0.0), movement.z * CEILING_DRAG);
            return;
        }
        if (this.navVerticalIntent < 0.0F)
            this.setDeltaMovement(movement.x, Math.max(movement.y - NAV_DESCENT, -NAV_DESCENT_MAX), movement.z);
        else this.setDeltaMovement(movement.x, Math.min(movement.y + ASCENT_ACCEL, ASCENT_MAX_SPEED), movement.z);
    }

    @SuppressWarnings("resource")
    private boolean tryWaterExitHop() {
        if (Math.abs(this.zza) < FORWARD_INPUT_EPSILON) return false;
        Vec3 velocity = this.getDeltaMovement();
        if (velocity.x * velocity.x + velocity.z * velocity.z > JAMMED_VELOCITY_SQR) return false;
        Path path = this.getNavigation().getPath();
        if (path == null || path.isDone()) return false;
        Vec3i node = path.getNextNodePos();
        BlockPos nodePos = BlockPos.containing(node.getX(), node.getY(), node.getZ());
        if (!this.level().getFluidState(nodePos).isEmpty()) return false;
        float yRotRad = this.getYRot() * ((float) Math.PI / 180F);
        Vec3 dir = new Vec3(-Mth.sin(yRotRad), 0.0, Mth.cos(yRotRad));
        BlockPos ahead = BlockPos.containing(
                this.getX() + dir.x * 0.9,
                this.getBoundingBox().minY + 0.1,
                this.getZ() + dir.z * 0.9);
        if (this.level().getBlockState(ahead).getCollisionShape(this.level(), ahead).isEmpty())
            return false;
        if (!this.level().getBlockState(ahead.above()).getCollisionShape(this.level(), ahead.above()).isEmpty())
            return false;
        this.setDeltaMovement(dir.x * HOP_HORIZONTAL, HOP_VERTICAL, dir.z * HOP_HORIZONTAL);
        this.waterExitHopCooldown = HOP_COOLDOWN_TICKS;
        return true;
    }

    @SuppressWarnings("resource")
    private boolean isEyesUnderwater() {
        return this.level().getFluidState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(FluidTags.WATER);
    }

    // Air supply: can actually drown

    @Override
    public int getMaxAirSupply() {
        return 6000;
    }

    @Override
    public void baseTick() {
        int currentAir = this.getAirSupply();
        super.baseTick();
        if (!this.isNoAi()) this.handleAirSupply(currentAir);
    }

    protected void handleAirSupply(int currentAir) {
        if (this.isAlive() && this.isEyesUnderwater()) {
            this.setAirSupply(currentAir - 1);
            if (this.getAirSupply() == -20) {
                this.setAirSupply(0);
                this.hurt(this.damageSources().drown(), 2.0F);
            }
        } else this.setAirSupply(this.getMaxAirSupply());
    }

    // Vanilla

    @Override
    public int getMaxHeadXRot() { return 30; }

    @Override
    public int getMaxHeadYRot() { return this.isUnderWater() ? 0 : 30; }

    @Override
    protected @NotNull PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public boolean isPushedByFluid() { return false; }

    @Override
    protected float getWaterSlowDown() { return 0.98F; }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return EntityInit.CAPYBARA.create(serverLevel);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.CAPYBARA_FOOD);
    }

    @Override
    public boolean canMate(Animal animal) {
        if (!(animal instanceof CapybaraEntity capybaraEntity)) return false;
        return this.isInLove() && capybaraEntity.isInLove();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean checkCapybaraEntitySpawnRules(EntityType<CapybaraEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA) ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.CAPYBARAS_SPAWNABLE_ON);
    }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.AXOLOTL_IDLE_AIR; }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) { return SoundEvents.AXOLOTL_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.AXOLOTL_DEATH; }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundEvents.AXOLOTL_ATTACK, 0.1F, 1.0F);
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null)
            spawnGroupData = new AgeableMob.AgeableMobGroupData(1.0F);
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
    }

    @SuppressWarnings("resource")
    public boolean hasSurfaceAccess() {
        BlockPos pos = this.blockPosition();
        for (int i = 0; i < 4; i++) {
            if (!this.level().getFluidState(pos).is(FluidTags.WATER)) break;
            pos = pos.above();
        }
        return this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty();
    }
}