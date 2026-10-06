package net.untamed.entity.ai.kiwi;

import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.KiwiEntity;
import org.jetbrains.annotations.Nullable;

public class KiwiStrollGoal extends RandomStrollGoal {

    private final KiwiEntity kiwi;

    public KiwiStrollGoal(KiwiEntity kiwi, double speedModifier) {
        super(kiwi, speedModifier);
        this.kiwi = kiwi;
    }

    @Override
    public boolean canUse() {
        return !this.kiwi.isSleeping() && super.canUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        return LandRandomPos.getPos(this.kiwi, 10, 7);
    }
}
