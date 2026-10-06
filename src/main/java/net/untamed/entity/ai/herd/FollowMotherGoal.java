package net.untamed.entity.ai.herd;

import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.HerdBovineEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class FollowMotherGoal extends Goal {

    private static final double START_DISTANCE = 5.0D;
    private static final double STOP_DISTANCE = 2.5D;
    private static final double RUN_DISTANCE = 12.0D;

    private final HerdBovineEntity calf;
    @Nullable
    private HerdBovineEntity mother;
    private int repathTicks;

    public FollowMotherGoal(HerdBovineEntity calf) {
        this.calf = calf;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.calf.isBaby()) {
            return false;
        }
        this.mother = this.calf.getMother();
        return this.mother != null && this.calf.distanceToSqr(this.mother) > START_DISTANCE * START_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        return this.calf.isBaby() && this.mother != null && this.mother.isAlive() && this.calf.distanceToSqr(this.mother) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void stop() {
        this.mother = null;
        this.calf.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.mother != null && --this.repathTicks <= 0) {
            this.repathTicks = this.adjustedTickDelay(10);
            double speed = this.calf.distanceToSqr(this.mother) > RUN_DISTANCE * RUN_DISTANCE ? 1.5D : 1.15D;
            this.calf.getNavigation().moveTo(this.mother, speed);
        }
    }
}
