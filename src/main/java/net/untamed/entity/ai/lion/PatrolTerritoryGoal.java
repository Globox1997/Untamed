package net.untamed.entity.ai.lion;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractLionEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PatrolTerritoryGoal extends Goal {

    private static final int PATROL_CHANCE = 400;
    private static final int MAX_PATROL_TICKS = 600;
    private static final double MIN_RADIUS = 20.0D;
    private static final double RADIUS_VARIATION = 12.0D;

    private final AbstractLionEntity lion;
    @Nullable
    private Vec3 patrolPoint;
    private int ticks;

    public PatrolTerritoryGoal(AbstractLionEntity lion) {
        this.lion = lion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos home = this.lion.getHomePos();
        if (!this.lion.isPrideMale() || home == null || this.lion.isRestTime() || this.lion.isAlarmed() || this.lion.getTarget() != null || this.lion.isSleeping()
                || this.lion.getRandom().nextInt(reducedTickDelay(PATROL_CHANCE)) != 0) {
            return false;
        }
        float angle = this.lion.getRandom().nextFloat() * Mth.TWO_PI;
        double radius = MIN_RADIUS + this.lion.getRandom().nextDouble() * RADIUS_VARIATION;
        Vec3 edge = Vec3.atBottomCenterOf(home).add(Mth.cos(angle) * radius, 0.0D, Mth.sin(angle) * radius);
        this.patrolPoint = LandRandomPos.getPosTowards(this.lion, 16, 7, edge);
        return this.patrolPoint != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.patrolPoint != null && this.ticks > 0 && !this.lion.getNavigation().isDone() && this.lion.getTarget() == null;
    }

    @Override
    public void start() {
        this.ticks = this.adjustedTickDelay(MAX_PATROL_TICKS);
        if (this.patrolPoint != null) {
            this.lion.getNavigation().moveTo(this.patrolPoint.x, this.patrolPoint.y, this.patrolPoint.z, 0.9D);
        }
    }

    @Override
    public void tick() {
        this.ticks--;
    }

    @Override
    public void stop() {
        if (this.patrolPoint != null && this.lion.position().distanceToSqr(this.patrolPoint) < 16.0D && this.lion.getRandom().nextInt(3) == 0) {
            this.lion.requestRoar(5);
        }
        this.lion.getNavigation().stop();
        this.patrolPoint = null;
    }
}
