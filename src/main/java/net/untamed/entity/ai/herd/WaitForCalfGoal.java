package net.untamed.entity.ai.herd;

import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.HerdBovineEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class WaitForCalfGoal extends Goal {

    private static final double START_DISTANCE = 10.0D;
    private static final double STOP_DISTANCE = 5.0D;

    private final HerdBovineEntity mother;
    @Nullable
    private HerdBovineEntity calf;
    private int repathTicks;

    public WaitForCalfGoal(HerdBovineEntity mother) {
        this.mother = mother;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mother.isBaby() || this.mother.isMale() || this.mother.getRandom().nextInt(reducedTickDelay(10)) != 0) {
            return false;
        }
        this.calf = this.mother.getStrayingCalf(START_DISTANCE);
        return this.calf != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.calf != null && this.calf.isAlive() && this.calf.isBaby() && this.mother.distanceToSqr(this.calf) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void stop() {
        this.calf = null;
        this.mother.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.calf != null && --this.repathTicks <= 0) {
            this.repathTicks = this.adjustedTickDelay(10);
            this.mother.getNavigation().moveTo(this.calf, 1.0D);
        }
    }
}
