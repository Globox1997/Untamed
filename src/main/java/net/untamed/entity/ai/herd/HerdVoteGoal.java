package net.untamed.entity.ai.herd;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.BuffaloEntity;

import java.util.EnumSet;

public class HerdVoteGoal extends Goal {

    private final BuffaloEntity mob;
    private int standTicks;

    public HerdVoteGoal(BuffaloEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !this.mob.isBaby() && !this.mob.isRoamer() && !this.mob.isAlarmed() && this.mob.getTarget() == null
                && this.mob.wantsToVote() && this.mob.onGround();
    }

    @Override
    public boolean canContinueToUse() {
        return this.standTicks > 0 && !this.mob.isAlarmed() && this.mob.getTarget() == null;
    }

    @Override
    public void start() {
        this.standTicks = this.adjustedTickDelay(100 + this.mob.getRandom().nextInt(60));
        this.mob.castVote(this.mob.getRandom().nextFloat() * 360.0F);
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.standTicks--;
        float yaw = this.mob.getVoteYaw() * Mth.DEG_TO_RAD;
        this.mob.getLookControl().setLookAt(this.mob.getX() - Mth.sin(yaw) * 5.0D, this.mob.getEyeY(), this.mob.getZ() + Mth.cos(yaw) * 5.0D);
    }
}
