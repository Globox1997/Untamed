package net.untamed.entity.ai.panther;

import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.untamed.entity.BlackPantherEntity;

public class PantherStrollGoal extends RandomStrollGoal {

    private final BlackPantherEntity panther;

    public PantherStrollGoal(BlackPantherEntity panther, double speedModifier) {
        super(panther, speedModifier);
        this.panther = panther;
    }

    @Override
    public boolean canUse() {
        return !this.panther.isResting() && !this.panther.isYoungCub() && super.canUse();
    }
}
