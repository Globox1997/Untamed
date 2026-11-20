package net.untamed.entity.ai.raccoon;

import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.untamed.entity.RaccoonEntity;

public class RaccoonStrollGoal extends RandomStrollGoal {

    private static final int STROLL_INTERVAL = 60;

    private final RaccoonEntity raccoon;

    public RaccoonStrollGoal(RaccoonEntity raccoon, double speedModifier) {
        super(raccoon, speedModifier, STROLL_INTERVAL);
        this.raccoon = raccoon;
    }

    @Override
    public boolean canUse() {
        return this.raccoon.getRaccoonPose() == RaccoonEntity.RaccoonPose.NONE && super.canUse();
    }
}
