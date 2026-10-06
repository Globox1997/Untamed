package net.untamed.entity.ai.lion;

import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.AbstractLionEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class CubNapGoal extends Goal {

    private static final double SEARCH_RANGE = 10.0D;
    private static final double KEEP_RANGE = 12.0D;
    private static final double CUDDLE_DISTANCE = 2.0D;

    private final AbstractLionEntity cub;
    @Nullable
    private AbstractLionEntity adult;
    private int repathTicks;

    public CubNapGoal(AbstractLionEntity cub) {
        this.cub = cub;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!this.cub.isBaby() || this.cub.isAlarmed() || this.cub.getTarget() != null || this.cub.getRandom().nextInt(reducedTickDelay(20)) != 0) {
            return false;
        }
        this.adult = null;
        for (AbstractLionEntity member : this.cub.getPride()) {
            if (member.isAlive() && !member.isBaby() && member.isSleeping() && this.cub.distanceToSqr(member) < SEARCH_RANGE * SEARCH_RANGE
                    && (this.adult == null || this.cub.distanceToSqr(member) < this.cub.distanceToSqr(this.adult))) {
                this.adult = member;
            }
        }
        return this.adult != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.adult != null && this.adult.isAlive() && this.adult.isSleeping() && !this.cub.isAlarmed() && this.cub.isBaby()
                && this.cub.distanceToSqr(this.adult) < KEEP_RANGE * KEEP_RANGE;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        if (this.adult == null) {
            return;
        }
        if (this.cub.distanceToSqr(this.adult) > CUDDLE_DISTANCE * CUDDLE_DISTANCE) {
            this.cub.clearLionPose(AbstractLionEntity.LionPose.SLEEPING);
            if (--this.repathTicks <= 0 || this.cub.getNavigation().isDone()) {
                this.repathTicks = this.adjustedTickDelay(20);
                this.cub.getNavigation().moveTo(this.adult, 1.0D);
            }
        } else {
            this.cub.getNavigation().stop();
            this.cub.setLionPose(AbstractLionEntity.LionPose.SLEEPING);
        }
    }

    @Override
    public void stop() {
        this.cub.clearLionPose(AbstractLionEntity.LionPose.SLEEPING);
        this.adult = null;
    }
}
