package net.untamed.entity.ai.hyena;

import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.HyenaEntity;

import java.util.EnumSet;

// Contact whoops at night; clan members may answer. Recruiting whoops are triggered by HyenaEntity itself.
public class WhoopGoal extends Goal {

    private static final int WHOOP_CHANCE = 800;
    private static final int MIN_TICKS_BETWEEN_WHOOPS = 2400;
    private static final int STAND_TICKS = 40;

    private final HyenaEntity hyena;
    private int ticks;

    public WhoopGoal(HyenaEntity hyena) {
        this.hyena = hyena;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !this.hyena.isBaby() && !this.hyena.isRestTime() && !this.hyena.isResting() && !this.hyena.isAlarmed() && this.hyena.getTarget() == null
                && this.hyena.getHunt() == null && this.hyena.level().getGameTime() - this.hyena.getLastHeardWhoopTime() > MIN_TICKS_BETWEEN_WHOOPS
                && this.hyena.getRandom().nextInt(reducedTickDelay(WHOOP_CHANCE)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.ticks > 0 && this.hyena.getTarget() == null;
    }

    @Override
    public void start() {
        this.ticks = this.adjustedTickDelay(STAND_TICKS);
        this.hyena.getNavigation().stop();
        this.hyena.whoop(false, true);
    }

    @Override
    public void tick() {
        this.ticks--;
    }
}
