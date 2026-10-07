package net.untamed.entity.ai.panther;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.BlackPantherEntity;

import java.util.EnumSet;

public class CubStayHiddenGoal extends Goal {

    private static final double START_DISTANCE = 4.0D;
    private static final double STOP_DISTANCE = 2.0D;
    private static final int COVER_SEARCH_RANGE = 8;

    private final BlackPantherEntity cub;
    private boolean denChecked;
    private int repathTicks;

    public CubStayHiddenGoal(BlackPantherEntity cub) {
        this.cub = cub;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.cub.isYoungCub() || this.cub.isResting() || this.cub.getRandom().nextInt(reducedTickDelay(10)) != 0) {
            return false;
        }
        this.moveDenIntoCover();
        BlockPos den = this.cub.getDenPos();
        return den != null && !this.cub.blockPosition().closerThan(den, START_DISTANCE);
    }

    private void moveDenIntoCover() {
        BlockPos den = this.cub.getDenPos();
        if (this.denChecked || den == null) {
            return;
        }
        this.denChecked = true;
        if (this.cub.scoreCoverSpot(den) > 0.0D) {
            return;
        }
        Vec3 cover = LandRandomPos.getPos(this.cub, COVER_SEARCH_RANGE, 4, this.cub::scoreCoverSpot);
        if (cover != null && this.cub.scoreCoverSpot(BlockPos.containing(cover)) > 0.0D) {
            this.cub.setDenPos(BlockPos.containing(cover));
        }
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos den = this.cub.getDenPos();
        return this.cub.isYoungCub() && den != null && !this.cub.blockPosition().closerThan(den, STOP_DISTANCE);
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        BlockPos den = this.cub.getDenPos();
        if (den != null && (--this.repathTicks <= 0 || this.cub.getNavigation().isDone())) {
            this.repathTicks = this.adjustedTickDelay(20);
            this.cub.getNavigation().moveTo(den.getX() + 0.5D, den.getY(), den.getZ() + 0.5D, 1.0D);
        }
    }

    @Override
    public void stop() {
        this.cub.getNavigation().stop();
    }
}
