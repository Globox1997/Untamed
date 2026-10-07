package net.untamed.entity.ai.panther;

import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.untamed.entity.BlackPantherEntity;

public class PantherHurtByTargetGoal extends HurtByTargetGoal {

    private final BlackPantherEntity panther;

    public PantherHurtByTargetGoal(BlackPantherEntity panther) {
        super(panther);
        this.panther = panther;
    }

    @Override
    public boolean canUse() {
        return !this.panther.isBaby() && super.canUse();
    }
}
