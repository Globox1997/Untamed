package net.untamed.entity.ai.panther;

import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.BlackPantherEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class CubFollowMotherGoal extends Goal {

    private static final double START_DISTANCE = 5.0D;
    private static final double STOP_DISTANCE = 2.5D;
    private static final double RUN_DISTANCE = 12.0D;

    private final BlackPantherEntity cub;
    @Nullable
    private BlackPantherEntity mother;
    private int repathTicks;

    public CubFollowMotherGoal(BlackPantherEntity cub) {
        this.cub = cub;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.cub.isBaby() || this.cub.isYoungCub() || this.cub.isResting()) {
            return false;
        }
        this.mother = this.cub.getMother();
        return this.mother != null && !this.mother.isHunting() && this.cub.distanceToSqr(this.mother) > START_DISTANCE * START_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        return this.cub.isBaby() && this.mother != null && this.mother.isAlive() && !this.mother.isHunting()
                && this.cub.distanceToSqr(this.mother) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        if (this.mother != null && --this.repathTicks <= 0) {
            this.repathTicks = this.adjustedTickDelay(10);
            this.cub.getNavigation().moveTo(this.mother, this.cub.distanceToSqr(this.mother) > RUN_DISTANCE * RUN_DISTANCE ? 1.5D : 1.2D);
        }
    }

    @Override
    public void stop() {
        this.mother = null;
        this.cub.getNavigation().stop();
    }
}
