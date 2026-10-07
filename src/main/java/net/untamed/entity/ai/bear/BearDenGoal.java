package net.untamed.entity.ai.bear;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.BlackBearEntity;

import java.util.EnumSet;

public class BearDenGoal extends Goal {

    private static final int START_CHANCE = 80;
    private static final int MIN_REST_TICKS = 600;
    private static final int REST_TICKS_VARIATION = 900;
    private static final int COOLDOWN = 400;
    private static final int SEARCH_RANGE = 16;

    private final BlackBearEntity bear;
    private boolean walking;
    private int restTicks;
    private long nextRestTime;

    public BearDenGoal(BlackBearEntity bear) {
        this.bear = bear;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.bear.isSnowingHere() && !this.bear.isAlarmed() && this.bear.getTarget() == null && this.bear.onGround()
                && this.bear.level().getGameTime() >= this.nextRestTime && this.bear.getRandom().nextInt(reducedTickDelay(START_CHANCE)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.bear.isSnowingHere() && !this.bear.isAlarmed() && this.bear.getTarget() == null && (this.walking || (this.bear.isResting() && this.restTicks > 0));
    }

    @Override
    public void start() {
        this.walking = false;
        if (this.bear.scoreDenSpot(this.bear.blockPosition()) <= 0.0D) {
            Vec3 den = LandRandomPos.getPos(this.bear, SEARCH_RANGE, 6, this.bear::scoreDenSpot);
            if (den != null && this.bear.scoreDenSpot(BlockPos.containing(den)) > 0.0D) {
                this.walking = this.bear.getNavigation().moveTo(den.x, den.y, den.z, 1.0D);
            }
        }
        if (!this.walking) {
            this.lieDown();
        }
    }

    @Override
    public void tick() {
        if (this.walking) {
            if (this.bear.getNavigation().isDone()) {
                this.walking = false;
                this.lieDown();
            }
        } else {
            this.restTicks--;
            this.bear.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.walking = false;
        this.bear.clearBearPose(BlackBearEntity.BearPose.RESTING);
        this.nextRestTime = this.bear.level().getGameTime() + COOLDOWN;
    }

    private void lieDown() {
        this.restTicks = this.adjustedTickDelay(MIN_REST_TICKS + this.bear.getRandom().nextInt(REST_TICKS_VARIATION));
        this.bear.getNavigation().stop();
        this.bear.setBearPose(BlackBearEntity.BearPose.RESTING);
    }
}
