package net.untamed.entity.ai.lion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.AbstractLionEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class CubStayNearPrideGoal extends Goal {

    private static final double START_DISTANCE = 6.0D;
    private static final double STOP_DISTANCE = 3.0D;
    private static final double RUN_DISTANCE = 14.0D;
    private static final double HOME_START_DISTANCE = 8.0D;
    private static final double HOME_STOP_DISTANCE = 4.0D;

    private final AbstractLionEntity cub;
    @Nullable
    private AbstractLionEntity guardian;
    @Nullable
    private BlockPos home;
    private int repathTicks;

    public CubStayNearPrideGoal(AbstractLionEntity cub) {
        this.cub = cub;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.cub.isBaby() || this.cub.isSleeping() || this.cub.getRandom().nextInt(reducedTickDelay(10)) != 0) {
            return false;
        }
        this.guardian = this.findGuardian();
        this.home = null;
        if (this.guardian != null) {
            return this.cub.distanceToSqr(this.guardian) > START_DISTANCE * START_DISTANCE;
        }
        BlockPos homePos = this.cub.getHomePos();
        if (homePos != null && this.cub.blockPosition().distSqr(homePos) > HOME_START_DISTANCE * HOME_START_DISTANCE) {
            this.home = homePos;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.cub.isBaby()) {
            return false;
        }
        if (this.guardian != null) {
            return this.guardian.isAlive() && this.guardian.getHunt() == null && this.cub.distanceToSqr(this.guardian) > STOP_DISTANCE * STOP_DISTANCE;
        }
        return this.home != null && this.cub.blockPosition().distSqr(this.home) > HOME_STOP_DISTANCE * HOME_STOP_DISTANCE;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void stop() {
        this.guardian = null;
        this.home = null;
        this.cub.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (--this.repathTicks > 0) {
            return;
        }
        this.repathTicks = this.adjustedTickDelay(10);
        if (this.guardian != null) {
            double speed = this.cub.distanceToSqr(this.guardian) > RUN_DISTANCE * RUN_DISTANCE ? 1.5D : 1.15D;
            this.cub.getNavigation().moveTo(this.guardian, speed);
        } else if (this.home != null) {
            this.cub.getNavigation().moveTo(this.home.getX() + 0.5D, this.home.getY(), this.home.getZ() + 0.5D, 1.0D);
        }
    }

    @Nullable
    private AbstractLionEntity findGuardian() {
        AbstractLionEntity nearest = null;
        for (AbstractLionEntity member : this.cub.getPride()) {
            if (member.isAlive() && !member.isBaby() && member.getHunt() == null
                    && (nearest == null || this.cub.distanceToSqr(member) < this.cub.distanceToSqr(nearest))) {
                nearest = member;
            }
        }
        return nearest;
    }
}
