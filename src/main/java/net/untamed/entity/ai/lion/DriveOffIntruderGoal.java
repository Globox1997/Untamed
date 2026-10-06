package net.untamed.entity.ai.lion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.AbstractLionEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class DriveOffIntruderGoal extends Goal {

    private static final double DETECT_RANGE = 24.0D;
    private static final double RELEASE_DISTANCE = AbstractLionEntity.TERRITORY_RADIUS + 8.0D;
    private static final int MAX_CHASE_TICKS = 400;
    private static final int AVOID_TICKS = 1200;

    private final AbstractLionEntity lion;
    @Nullable
    private AbstractLionEntity intruder;
    private int ticks;
    private int repathTicks;

    public DriveOffIntruderGoal(AbstractLionEntity lion) {
        this.lion = lion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        BlockPos home = this.lion.getHomePos();
        if (!this.lion.isPrideMale() || home == null || this.lion.getTarget() != null || this.lion.getRandom().nextInt(reducedTickDelay(20)) != 0) {
            return false;
        }
        this.intruder = null;
        for (AbstractLionEntity other : this.lion.level().getEntitiesOfClass(AbstractLionEntity.class, this.lion.getBoundingBox().inflate(DETECT_RANGE, 8.0D, DETECT_RANGE),
                other -> other.isAlive() && other.isMale() && !other.isBaby() && !other.isInPrideWith(this.lion)
                        && other.blockPosition().distSqr(home) < AbstractLionEntity.TERRITORY_RADIUS * AbstractLionEntity.TERRITORY_RADIUS)) {
            if (this.intruder == null || this.lion.distanceToSqr(other) < this.lion.distanceToSqr(this.intruder)) {
                this.intruder = other;
            }
        }
        return this.intruder != null;
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos home = this.lion.getHomePos();
        return this.intruder != null && this.intruder.isAlive() && home != null && this.ticks > 0 && this.lion.getTarget() == null
                && this.intruder.blockPosition().distSqr(home) < RELEASE_DISTANCE * RELEASE_DISTANCE;
    }

    @Override
    public void start() {
        this.ticks = this.adjustedTickDelay(MAX_CHASE_TICKS);
        this.repathTicks = 0;
        this.lion.clearLionPose(AbstractLionEntity.LionPose.SLEEPING);
        this.lion.setLionPose(AbstractLionEntity.LionPose.WARNING);
    }

    @Override
    public void tick() {
        if (this.intruder == null || this.lion.getHomePos() == null) {
            return;
        }
        this.ticks--;
        this.intruder.avoid(this.lion.getHomePos(), AVOID_TICKS);
        this.lion.getLookControl().setLookAt(this.intruder, 30.0F, 30.0F);
        this.lion.playWarningSound();
        if (--this.repathTicks <= 0) {
            this.repathTicks = this.adjustedTickDelay(10);
            this.lion.getNavigation().moveTo(this.intruder, 1.4D);
        }
    }

    @Override
    public void stop() {
        this.lion.clearLionPose(AbstractLionEntity.LionPose.WARNING);
        this.lion.getNavigation().stop();
        this.intruder = null;
    }
}
