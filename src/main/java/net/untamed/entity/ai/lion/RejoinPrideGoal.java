package net.untamed.entity.ai.lion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractLionEntity;

import java.util.EnumSet;

public class RejoinPrideGoal extends Goal {

    private static final double START_DISTANCE = 16.0D;
    private static final double STOP_DISTANCE = 8.0D;
    private static final int RETRY_COOLDOWN = 100;

    private final AbstractLionEntity lion;
    private final double speedModifier;
    private long retryAfter;
    private int repathTicks;

    public RejoinPrideGoal(AbstractLionEntity lion, double speedModifier) {
        this.lion = lion;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.lion.isBaby() || this.lion.isSleeping() || this.lion.getHunt() != null || this.lion.level().getGameTime() < this.retryAfter) {
            return false;
        }
        Vec3 center = this.lion.getPrideCenter();
        if (center == null || this.lion.position().distanceToSqr(center) < START_DISTANCE * START_DISTANCE) {
            return false;
        }
        Path path = this.lion.getNavigation().createPath(BlockPos.containing(center), 4);
        if (path == null || !path.canReach()) {
            this.retryAfter = this.lion.level().getGameTime() + RETRY_COOLDOWN;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        Vec3 center = this.lion.getPrideCenter();
        return center != null && this.lion.getHunt() == null && this.lion.position().distanceToSqr(center) > STOP_DISTANCE * STOP_DISTANCE && !this.lion.getNavigation().isDone();
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
        this.lion.getNavigation().stop();
    }

    private void moveToCenter() {
        this.repathTicks = this.adjustedTickDelay(40);
        Vec3 center = this.lion.getPrideCenter();
        if (center != null) {
            this.lion.getNavigation().moveTo(center.x, center.y, center.z, this.speedModifier);
        }
    }
}
