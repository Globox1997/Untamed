package net.untamed.entity.ai.herd;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HerdBovineEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BisonChargeGoal extends Goal {

    private static final int WINDUP_TICKS = 25;
    private static final int MAX_RUN_TICKS = 40;
    private static final double MIN_DISTANCE = 4.0D;
    private static final double MAX_DISTANCE = 16.0D;
    private static final double OVERSHOOT = 4.0D;
    private static final double CHARGE_SPEED = 2.6D;
    private static final double TOSS_HORIZONTAL = 1.2D;
    private static final double TOSS_VERTICAL = 0.5D;

    private final HerdBovineEntity mob;
    @Nullable
    private LivingEntity target;
    private Vec3 direction = Vec3.ZERO;
    private Vec3 startPos = Vec3.ZERO;
    private double maxTravel;
    private int ticks;
    private boolean running;
    private boolean finished;
    private long nextChargeTime;

    public BisonChargeGoal(HerdBovineEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isBaby() || this.mob.level().getGameTime() < this.nextChargeTime || !this.mob.onGround()) {
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distanceSqr = this.mob.distanceToSqr(target);
        if (distanceSqr < MIN_DISTANCE * MIN_DISTANCE || distanceSqr > MAX_DISTANCE * MAX_DISTANCE || !this.mob.hasLineOfSight(target)) {
            return false;
        }
        this.target = target;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.finished && this.target != null && this.target.isAlive() && this.mob.getTarget() == this.target;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.running = false;
        this.finished = false;
        this.mob.getNavigation().stop();
        this.mob.setHerdPose(HerdBovineEntity.HerdPose.WARNING);
        this.mob.playWarningSound();
    }

    @Override
    public void stop() {
        this.mob.clearHerdPose(HerdBovineEntity.HerdPose.WARNING);
        this.mob.clearHerdPose(HerdBovineEntity.HerdPose.CHARGING);
        this.mob.getNavigation().stop();
        this.mob.getMoveControl().setWantedPosition(this.mob.getX(), this.mob.getY(), this.mob.getZ(), 0.0D);
        this.nextChargeTime = this.mob.level().getGameTime() + 80 + this.mob.getRandom().nextInt(60);
        this.target = null;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.ticks++;
        if (!this.running) {
            this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            if (this.ticks >= WINDUP_TICKS) {
                this.beginRun();
            }
            return;
        }

        if (this.ticks > MAX_RUN_TICKS || this.mob.horizontalCollision || !this.isSafeAhead()
                || this.mob.position().distanceToSqr(this.startPos) > this.maxTravel * this.maxTravel) {
            this.finished = true;
            return;
        }

        float yaw = (float) (Mth.atan2(this.direction.z, this.direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.mob.setYRot(yaw);
        this.mob.setYBodyRot(yaw);
        this.mob.setYHeadRot(yaw);
        Vec3 ahead = this.mob.position().add(this.direction.scale(3.0D));
        this.mob.getMoveControl().setWantedPosition(ahead.x, ahead.y, ahead.z, CHARGE_SPEED);

        if (this.mob.getBoundingBox().inflate(0.4D).intersects(this.target.getBoundingBox())) {
            this.mob.doHurtTarget(this.target);
            this.target.push(this.direction.x * TOSS_HORIZONTAL, TOSS_VERTICAL, this.direction.z * TOSS_HORIZONTAL);
            this.target.hurtMarked = true;
            this.finished = true;
        }
    }

    private void beginRun() {
        Vec3 toTarget = this.target.position().subtract(this.mob.position());
        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() < 1.0E-4D) {
            this.finished = true;
            return;
        }
        this.direction = horizontal.normalize();
        this.startPos = this.mob.position();
        this.maxTravel = horizontal.length() + OVERSHOOT;
        this.running = true;
        this.ticks = 0;
        this.mob.setHerdPose(HerdBovineEntity.HerdPose.CHARGING);
    }

    private boolean isSafeAhead() {
        BlockPos ahead = BlockPos.containing(this.mob.position().add(this.direction.scale(1.5D)));
        for (int i = 0; i <= 3; i++) {
            BlockPos pos = ahead.below(i);
            if (!this.mob.level().getBlockState(pos).getCollisionShape(this.mob.level(), pos).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
