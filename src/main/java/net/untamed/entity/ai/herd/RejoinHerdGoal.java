package net.untamed.entity.ai.herd;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HerdBovineEntity;

import java.util.EnumSet;

public class RejoinHerdGoal extends Goal {

    private static final double START_DISTANCE = 12.0D;
    private static final double STOP_DISTANCE = 6.0D;
    private static final int RETRY_COOLDOWN = 100;

    private final HerdBovineEntity mob;
    private final double speedModifier;
    private long retryAfter;
    private int repathTicks;

    public RejoinHerdGoal(HerdBovineEntity mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isBaby() || !this.mob.followsHerd() || this.mob.isHerdLeader() || this.mob.level().getGameTime() < this.retryAfter) {
            return false;
        }
        Vec3 center = this.mob.getHerdCenter();
        if (center == null || this.mob.position().distanceToSqr(center) < START_DISTANCE * START_DISTANCE) {
            return false;
        }
        Path path = this.mob.getNavigation().createPath(BlockPos.containing(center), 4);
        if (path == null || !path.canReach()) {
            this.retryAfter = this.mob.level().getGameTime() + RETRY_COOLDOWN;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        Vec3 center = this.mob.getHerdCenter();
        return center != null && this.mob.position().distanceToSqr(center) > STOP_DISTANCE * STOP_DISTANCE && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.repathTicks = 0;
        this.moveToCenter();
    }

    @Override
    public void tick() {
        if (--this.repathTicks <= 0) {
            this.moveToCenter();
        }
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
    }

    private void moveToCenter() {
        this.repathTicks = this.adjustedTickDelay(40);
        Vec3 center = this.mob.getHerdCenter();
        if (center != null) {
            this.mob.getNavigation().moveTo(center.x, center.y, center.z, this.speedModifier);
        }
    }
}
