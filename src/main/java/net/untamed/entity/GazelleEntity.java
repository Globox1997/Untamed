package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.untamed.entity.ai.gazelle.GazelleFleeGoal;
import net.untamed.entity.ai.gazelle.StottingGoal;
import net.untamed.init.ConfigInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GazelleEntity extends HerdBovineEntity {

    private static final ThreatProfile THREAT_PROFILE = new ThreatProfile(16.0D, 9.0D, 0.0D, 0);

    private static final EntityDataAccessor<Boolean> DATA_STOTTING = SynchedEntityData.defineId(GazelleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SCANNING = SynchedEntityData.defineId(GazelleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ALARMED = SynchedEntityData.defineId(GazelleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FLEEING = SynchedEntityData.defineId(GazelleEntity.class, EntityDataSerializers.BOOLEAN);

    public static final double FLEE_DISTANCE = 10.0D;
    public static final double STOT_DISTANCE = 20.0D;

    private static final int PREDATOR_SCAN_INTERVAL = 10;
    private static final double ALERT_DETECTION_RANGE = 24.0D;
    private static final double DEFAULT_DETECTION_RANGE = 16.0D;
    private static final double RESTING_DETECTION_RANGE = 12.0D;
    private static final double GRAZING_DETECTION_RANGE = 8.0D;
    private static final double STALKING_DETECTION_FACTOR = 0.5D;
    private static final double FORGET_DISTANCE = 32.0D;
    private static final double SHARE_RANGE = 20.0D;
    private static final int NOTICE_ALARM_TICKS = 60;

    private static final int MIN_SCAN_INTERVAL = 60;
    private static final int SCAN_INTERVAL_PER_MEMBER = 15;
    private static final int MAX_SCAN_INTERVAL = 240;
    private static final int SCAN_INTERVAL_VARIATION = 40;
    private static final int MIN_SCAN_TICKS = 30;
    private static final int SCAN_TICKS_VARIATION = 20;

    private static final double LEAP_MIN_HEIGHT = 1.0D;
    private static final double LEAP_MAX_HEIGHT = 1.6D;
    private static final double LEAP_REACH = 0.9D;
    private static final double LEAP_VELOCITY = 0.55D;
    private static final double LEAP_FORWARD = 0.3D;
    private static final int LEAP_COOLDOWN = 20;
    private static final double LEAP_MIN_AWAY_DOT = 0.3D;

    @Nullable
    private LivingEntity nearestPredator;
    private int predatorScanCooldown;
    private long stotCooldownUntil;
    private int scanTicks;
    private int nextScanIn = MIN_SCAN_INTERVAL;
    @Nullable
    private Vec3 scanLookTarget;
    private int leapCooldown;

    private float scanAmount, scanAmountO;
    private float stotAmount, stotAmountO;
    private float alarmAmount, alarmAmountO;
    private float fleeAmount, fleeAmountO;

    public GazelleEntity(EntityType<? extends GazelleEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.GAZELLE_FOOD);
    }

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(1, new GazelleFleeGoal(this));
        this.goalSelector.addGoal(2, new StottingGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1D, Ingredient.of(TagInit.GAZELLE_FOOD), false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MOVEMENT_SPEED, 0.26D);
    }

    public static boolean checkGazelleEntitySpawnRules(EntityType<GazelleEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA) ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.GAZELLES_SPAWNABLE_ON);
    }

    @Override
    public ThreatProfile getThreatProfile() {
        return THREAT_PROFILE;
    }

    @Override
    public boolean fightsBack() {
        return false;
    }

    @Override
    public boolean isRestTime() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 4000L && time <= 9000L;
    }

    @Override
    public boolean prefersShade() {
        return true;
    }

    @Override
    public boolean canDetect(Player player) {
        if (this.getHerdPose() != HerdPose.GRAZING || this.isScanning()) {
            return true;
        }
        return this.distanceTo(player) < THREAT_PROFILE.watchDistance() * 0.5D;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STOTTING, false);
        builder.define(DATA_SCANNING, false);
        builder.define(DATA_ALARMED, false);
        builder.define(DATA_FLEEING, false);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.updateScanning();
        if (--this.predatorScanCooldown <= 0) {
            this.predatorScanCooldown = PREDATOR_SCAN_INTERVAL;
            this.scanForPredators();
        }
        if (this.nearestPredator != null && (!this.nearestPredator.isAlive() || this.distanceToSqr(this.nearestPredator) > FORGET_DISTANCE * FORGET_DISTANCE)) {
            this.nearestPredator = null;
        }
        this.entityData.set(DATA_ALARMED, this.isAlarmed());
        this.entityData.set(DATA_FLEEING, this.isAlarmed() && !this.getNavigation().isDone() && !this.isResting());
        this.tryLeap();
    }

    private double getDetectionRange() {
        if (this.isScanning() || this.isAlarmed() || this.getHerdPose() == HerdPose.ALERT) {
            return ALERT_DETECTION_RANGE;
        }
        if (this.getHerdPose() == HerdPose.GRAZING) {
            return GRAZING_DETECTION_RANGE;
        }
        return this.isResting() ? RESTING_DETECTION_RANGE : DEFAULT_DETECTION_RANGE;
    }

    private void scanForPredators() {
        double range = this.getDetectionRange();
        List<LivingEntity> predators = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range, 6.0D, range),
                entity -> entity.isAlive() && entity.getType().is(TagInit.GAZELLE_PREDATORS));
        LivingEntity closest = null;
        double closestSqr = Double.MAX_VALUE;
        for (LivingEntity predator : predators) {
            double effectiveRange = isResting(predator) ? Math.min(range, FLEE_DISTANCE) : isStalking(predator) ? range * STALKING_DETECTION_FACTOR : range;
            double distSqr = this.distanceToSqr(predator);
            if (distSqr < effectiveRange * effectiveRange && distSqr < closestSqr) {
                closestSqr = distSqr;
                closest = predator;
            }
        }
        if (closest != null) {
            this.noticePredator(closest, true);
        }
    }

    private static boolean isResting(LivingEntity predator) {
        if (predator instanceof AbstractLionEntity lion) {
            return lion.isSleeping();
        }
        if (predator instanceof HyenaEntity hyena) {
            return hyena.isResting();
        }
        return predator instanceof AbstractBigCatEntity cat && cat.isResting();
    }

    private static boolean isStalking(LivingEntity predator) {
        if (predator instanceof AbstractLionEntity lion) {
            return lion.getLionPose() == AbstractLionEntity.LionPose.STALKING;
        }
        if (predator instanceof AbstractBigCatEntity cat) {
            return cat.getPantherPose() == AbstractBigCatEntity.PantherPose.STALKING;
        }
        return predator.isDiscrete();
    }

    public void noticePredator(LivingEntity predator, boolean shareWithHerd) {
        boolean wasUnaware = this.nearestPredator == null;
        this.nearestPredator = predator;
        this.alarm(NOTICE_ALARM_TICKS);
        if (!wasUnaware) {
            return;
        }
        this.playWarningSound();
        if (shareWithHerd) {
            for (HerdBovineEntity member : this.getHerd()) {
                if (member instanceof GazelleEntity gazelle && gazelle.isAlive() && this.distanceToSqr(gazelle) < SHARE_RANGE * SHARE_RANGE) {
                    gazelle.noticePredator(predator, false);
                }
            }
        }
    }

    @Nullable
    public LivingEntity getNearestPredator() {
        return this.nearestPredator;
    }

    private void updateScanning() {
        if (this.getHerdPose() != HerdPose.GRAZING) {
            this.scanTicks = 0;
            this.entityData.set(DATA_SCANNING, false);
            return;
        }
        if (this.scanTicks > 0) {
            if (this.scanLookTarget != null) {
                this.getLookControl().setLookAt(this.scanLookTarget.x, this.scanLookTarget.y, this.scanLookTarget.z);
            }
            if (--this.scanTicks == 0) {
                this.entityData.set(DATA_SCANNING, false);
            }
            return;
        }
        if (--this.nextScanIn <= 0) {
            this.scanTicks = MIN_SCAN_TICKS + this.random.nextInt(SCAN_TICKS_VARIATION);
            this.nextScanIn = Math.min(MAX_SCAN_INTERVAL, MIN_SCAN_INTERVAL + this.getHerd().size() * SCAN_INTERVAL_PER_MEMBER) + this.random.nextInt(SCAN_INTERVAL_VARIATION);
            float yaw = this.random.nextFloat() * Mth.TWO_PI;
            this.scanLookTarget = new Vec3(this.getX() - Mth.sin(yaw) * 8.0D, this.getEyeY(), this.getZ() + Mth.cos(yaw) * 8.0D);
            this.entityData.set(DATA_SCANNING, true);
        }
    }

    private void tryLeap() {
        if (this.leapCooldown > 0) {
            this.leapCooldown--;
            return;
        }
        if (!ConfigInit.CONFIG.gazellesLeapObstacles || !this.isAlarmed() || this.isStotting() || !this.onGround() || this.isInWater()
                || !(this.horizontalCollision || this.getNavigation().isDone())) {
            return;
        }
        Vec3 threat = this.getThreatPosition();
        if (threat == null) {
            return;
        }
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double dirX = -Mth.sin(yaw);
        double dirZ = Mth.cos(yaw);
        Vec3 away = this.position().subtract(threat).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 1.0E-4D || (dirX * away.x + dirZ * away.z) / away.length() < LEAP_MIN_AWAY_DOT) {
            return;
        }
        BlockPos ahead = BlockPos.containing(this.getX() + dirX * LEAP_REACH, this.getY(), this.getZ() + dirZ * LEAP_REACH);
        VoxelShape shape = this.level().getBlockState(ahead).getCollisionShape(this.level(), ahead);
        if (shape.isEmpty()) {
            return;
        }
        double height = ahead.getY() + shape.max(Direction.Axis.Y) - this.getY();
        if (height <= LEAP_MIN_HEIGHT || height > LEAP_MAX_HEIGHT
                || !this.level().getBlockState(ahead.above()).getCollisionShape(this.level(), ahead.above()).isEmpty()
                || !this.level().getBlockState(ahead.above(2)).getCollisionShape(this.level(), ahead.above(2)).isEmpty()) {
            return;
        }
        this.setDeltaMovement(dirX * LEAP_FORWARD, LEAP_VELOCITY, dirZ * LEAP_FORWARD);
        this.hasImpulse = true;
        this.leapCooldown = LEAP_COOLDOWN;
    }

    @Nullable
    private Vec3 getThreatPosition() {
        if (this.nearestPredator != null) {
            return this.nearestPredator.position();
        }
        if (this.getLastHurtByMob() != null && this.getLastHurtByMob().isAlive()) {
            return this.getLastHurtByMob().position();
        }
        Player player = this.level().getNearestPlayer(this, THREAT_PROFILE.watchDistance());
        return player != null && !player.isSpectator() ? player.position() : null;
    }

    public boolean canStot() {
        return this.level().getGameTime() >= this.stotCooldownUntil;
    }

    public void setStotCooldown(int ticks) {
        this.stotCooldownUntil = this.level().getGameTime() + ticks;
    }

    public boolean isStotting() {
        return this.entityData.get(DATA_STOTTING);
    }

    public void setStotting(boolean stotting) {
        this.entityData.set(DATA_STOTTING, stotting);
    }

    public boolean isScanning() {
        return this.entityData.get(DATA_SCANNING);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.scanAmountO = this.scanAmount;
            this.stotAmountO = this.stotAmount;
            this.alarmAmountO = this.alarmAmount;
            this.fleeAmountO = this.fleeAmount;
            this.scanAmount = Mth.approach(this.scanAmount, this.isScanning() ? 1.0F : 0.0F, 0.15F);
            this.stotAmount = Mth.approach(this.stotAmount, this.isStotting() ? 1.0F : 0.0F, 0.25F);
            this.alarmAmount = Mth.approach(this.alarmAmount, this.entityData.get(DATA_ALARMED) ? 1.0F : 0.0F, 0.2F);
            this.fleeAmount = Mth.approach(this.fleeAmount, this.entityData.get(DATA_FLEEING) ? 1.0F : 0.0F, 0.1F);
        }
    }

    public float getScanAmount(float partialTick) {
        return Mth.lerp(partialTick, this.scanAmountO, this.scanAmount);
    }

    public float getStotAmount(float partialTick) {
        return Mth.lerp(partialTick, this.stotAmountO, this.stotAmount);
    }

    public float getAlarmAmount(float partialTick) {
        return Mth.lerp(partialTick, this.alarmAmountO, this.alarmAmount);
    }

    public float getFleeAmount(float partialTick) {
        return Mth.lerp(partialTick, this.fleeAmountO, this.fleeAmount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.GAZELLE_IDLE_EVENT;
    }

    @Override
    public void playAmbientSound() {
        if (!this.isResting()) {
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.GAZELLE_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.GAZELLE_DEATH_EVENT;
    }

    @Override
    protected SoundEvent getWarningSound() {
        return SoundInit.GAZELLE_IDLE_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.GAZELLE_STEP_EVENT, 0.15F, 1.0F);
    }
}
