package net.untamed.entity.ai.herd;

import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.HerdBovineEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class FollowLeaderGoal extends Goal {

    private static final double START_DISTANCE = 8.0D;
    private static final double STOP_DISTANCE = 4.0D;

    private final HerdBovineEntity mob;
    @Nullable
    private HerdBovineEntity leader;
    private int checkCooldown;
    private int repathTicks;

    public FollowLeaderGoal(HerdBovineEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isBaby() || this.mob.isHerdLeader() || --this.checkCooldown > 0) {
            return false;
        }
        this.checkCooldown = this.adjustedTickDelay(20 + this.mob.getRandom().nextInt(20));
        this.leader = this.mob.getHerdLeader();
        return this.leader != null && this.leader.isAlive() && this.mob.distanceToSqr(this.leader) > START_DISTANCE * START_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        return this.leader != null && this.leader.isAlive() && this.mob.distanceToSqr(this.leader) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void stop() {
        this.leader = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.leader != null && --this.repathTicks <= 0) {
            this.repathTicks = this.adjustedTickDelay(10);
            double speed = this.leader.getDeltaMovement().horizontalDistanceSqr() > 0.04D ? 1.3D : 1.0D;
            this.mob.getNavigation().moveTo(this.leader, speed);
        }
    }
}
