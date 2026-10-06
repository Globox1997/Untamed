package net.untamed.entity.ai.hyena;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.HyenaEntity;

import java.util.EnumSet;

// Clan members come back to the den at dawn and stay near it through the day
public class ReturnToDenGoal extends Goal {

    private static final double START_DISTANCE = 12.0D;
    private static final double STOP_DISTANCE = 6.0D;

    private final HyenaEntity hyena;
    private int repathTicks;

    public ReturnToDenGoal(HyenaEntity hyena) {
        this.hyena = hyena;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.hyena.getDenPos() != null && (this.hyena.isDawn() || this.hyena.isRestTime()) && !this.hyena.isNearDen(START_DISTANCE)
                && this.isFree() && !this.hyena.isResting();
    }

    @Override
    public boolean canContinueToUse() {
        return this.hyena.getDenPos() != null && !this.hyena.isNearDen(STOP_DISTANCE) && this.isFree();
    }

    private boolean isFree() {
        return this.hyena.getTarget() == null && this.hyena.getHunt() == null && this.hyena.getRallyPos() == null;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        BlockPos den = this.hyena.getDenPos();
        if (den != null && (--this.repathTicks <= 0 || this.hyena.getNavigation().isDone())) {
            this.repathTicks = this.adjustedTickDelay(40);
            this.hyena.getNavigation().moveTo(den.getX() + 0.5D, den.getY(), den.getZ() + 0.5D, 1.0D);
        }
    }

    @Override
    public void stop() {
        this.hyena.getNavigation().stop();
    }
}
