package net.untamed.entity.ai.lion;

import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.AbstractLionEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class CubPlayGoal extends Goal {

    private static final int PLAY_CHANCE = 300;
    private static final double SEARCH_RANGE = 8.0D;
    private static final double KEEP_RANGE = 12.0D;
    private static final double PLAY_DISTANCE = 2.0D;

    private final AbstractLionEntity cub;
    @Nullable
    private AbstractLionEntity partner;
    private int ticks;
    private int repathTicks;

    public CubPlayGoal(AbstractLionEntity cub) {
        this.cub = cub;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!this.cub.isBaby() || this.cub.isSleeping() || this.cub.isAlarmed() || this.cub.getTarget() != null
                || this.cub.getRandom().nextInt(reducedTickDelay(PLAY_CHANCE)) != 0) {
            return false;
        }
        AbstractLionEntity otherCub = null;
        AbstractLionEntity adult = null;
        for (AbstractLionEntity member : this.cub.getPride()) {
            if (!member.isAlive() || this.cub.distanceToSqr(member) > SEARCH_RANGE * SEARCH_RANGE) {
                continue;
            }
            if (member.isBaby() && !member.isSleeping() && (otherCub == null || this.cub.distanceToSqr(member) < this.cub.distanceToSqr(otherCub))) {
                otherCub = member;
            } else if (!member.isBaby() && member.getHunt() == null && (adult == null || this.cub.distanceToSqr(member) < this.cub.distanceToSqr(adult))) {
                adult = member;
            }
        }
        this.partner = otherCub != null ? otherCub : adult;
        return this.partner != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.ticks > 0 && this.partner != null && this.partner.isAlive() && !this.cub.isAlarmed() && this.cub.getTarget() == null
                && this.cub.distanceToSqr(this.partner) < KEEP_RANGE * KEEP_RANGE;
    }

    @Override
    public void start() {
        this.ticks = this.adjustedTickDelay(100 + this.cub.getRandom().nextInt(100));
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        if (this.partner == null) {
            return;
        }
        this.ticks--;
        this.cub.getLookControl().setLookAt(this.partner, 30.0F, 30.0F);
        if (this.cub.distanceToSqr(this.partner) > PLAY_DISTANCE * PLAY_DISTANCE) {
            this.cub.clearLionPose(AbstractLionEntity.LionPose.PLAYING);
            if (--this.repathTicks <= 0) {
                this.repathTicks = this.adjustedTickDelay(10);
                this.cub.getNavigation().moveTo(this.partner, 1.1D);
            }
        } else {
            this.cub.getNavigation().stop();
            this.cub.setLionPose(AbstractLionEntity.LionPose.PLAYING);
            if (this.cub.onGround() && this.cub.getRandom().nextInt(10) == 0) {
                this.cub.getJumpControl().jump();
            }
        }
    }

    @Override
    public void stop() {
        this.cub.clearLionPose(AbstractLionEntity.LionPose.PLAYING);
        this.cub.getNavigation().stop();
        this.partner = null;
    }
}
