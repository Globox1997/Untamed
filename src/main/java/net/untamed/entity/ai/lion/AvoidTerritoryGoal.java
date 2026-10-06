package net.untamed.entity.ai.lion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractLionEntity;

import java.util.EnumSet;

public class AvoidTerritoryGoal extends Goal {

    private static final double SAFE_DISTANCE = AbstractLionEntity.TERRITORY_RADIUS * 2.0D;

    private final AbstractLionEntity lion;
    private int repathTicks;

    public AvoidTerritoryGoal(AbstractLionEntity lion) {
        this.lion = lion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.lion.getTarget() == null && this.isInsideAvoidedArea();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    private boolean isInsideAvoidedArea() {
        BlockPos avoidPos = this.lion.getAvoidPos();
        return avoidPos != null && this.lion.blockPosition().distSqr(avoidPos) < SAFE_DISTANCE * SAFE_DISTANCE;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
        this.lion.clearLionPose(AbstractLionEntity.LionPose.SLEEPING);
    }

    @Override
    public void tick() {
        BlockPos avoidPos = this.lion.getAvoidPos();
        if (avoidPos == null || (--this.repathTicks > 0 && !this.lion.getNavigation().isDone())) {
            return;
        }
        this.repathTicks = this.adjustedTickDelay(20);
        Vec3 away = DefaultRandomPos.getPosAway(this.lion, 16, 7, Vec3.atCenterOf(avoidPos));
        if (away != null) {
            this.lion.getNavigation().moveTo(away.x, away.y, away.z, 1.2D);
        }
    }

    @Override
    public void stop() {
        BlockPos avoidPos = this.lion.getAvoidPos();
        if (avoidPos != null && this.lion.blockPosition().distSqr(avoidPos) >= SAFE_DISTANCE * SAFE_DISTANCE) {
            this.lion.stopAvoiding();
        }
        this.lion.getNavigation().stop();
    }
}
