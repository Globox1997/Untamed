package net.untamed.entity.ai.bear;

import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.untamed.entity.BlackBearEntity;

public class BearStrollGoal extends RandomStrollGoal {

    private final BlackBearEntity bear;

    public BearStrollGoal(BlackBearEntity bear, double speedModifier) {
        super(bear, speedModifier);
        this.bear = bear;
    }

    @Override
    public boolean canUse() {
        return !this.bear.isResting() && super.canUse();
    }
}
