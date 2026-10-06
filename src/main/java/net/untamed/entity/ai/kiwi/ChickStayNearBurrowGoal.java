package net.untamed.entity.ai.kiwi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.KiwiEntity;

import java.util.EnumSet;

public class ChickStayNearBurrowGoal extends Goal {

    private static final int YOUNG_CHICK_AGE = -12000;
    private static final double START_DISTANCE = 6.0D;
    private static final double STOP_DISTANCE = 3.0D;

    private final KiwiEntity chick;
    private int repathTicks;

    public ChickStayNearBurrowGoal(KiwiEntity chick) {
        this.chick = chick;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        BlockPos burrow = this.chick.getBurrowPos();
        return this.isYoungChick() && burrow != null && !this.chick.isSleeping() && this.chick.getRandom().nextInt(reducedTickDelay(10)) == 0
                && !this.chick.blockPosition().closerThan(burrow, START_DISTANCE);
    }

    @Override
    public boolean canContinueToUse() {
        BlockPos burrow = this.chick.getBurrowPos();
        return this.isYoungChick() && burrow != null && !this.chick.blockPosition().closerThan(burrow, STOP_DISTANCE);
    }

    private boolean isYoungChick() {
        return this.chick.isBaby() && this.chick.getAge() < YOUNG_CHICK_AGE;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        BlockPos burrow = this.chick.getBurrowPos();
        if (burrow != null && (--this.repathTicks <= 0 || this.chick.getNavigation().isDone())) {
            this.repathTicks = this.adjustedTickDelay(20);
            this.chick.getNavigation().moveTo(burrow.getX() + 0.5D, burrow.getY(), burrow.getZ() + 0.5D, 1.0D);
        }
    }

    @Override
    public void stop() {
        this.chick.getNavigation().stop();
    }
}
