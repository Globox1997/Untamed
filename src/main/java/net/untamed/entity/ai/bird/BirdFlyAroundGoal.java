package net.untamed.entity.ai.bird;

import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractBirdEntity;
import org.jetbrains.annotations.Nullable;

public class BirdFlyAroundGoal extends WaterAvoidingRandomFlyingGoal {

    private static final int FLY_INTERVAL = 30;

    private final AbstractBirdEntity bird;

    public BirdFlyAroundGoal(AbstractBirdEntity bird) {
        super(bird, bird.getFlySpeedModifier());
        this.bird = bird;
        this.setInterval(FLY_INTERVAL);
    }

    @Override
    public boolean canUse() {
        return !this.bird.isBusy() && !this.bird.isPerched() && super.canUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        int range = this.bird.getFlyRange();
        Vec3 view = this.bird.getViewVector(0.0F);
        Vec3 target = HoverRandomPos.getPos(this.bird, range, 7, view.x, view.z, (float) (Math.PI / 2.0), 3, 1);
        return target != null ? target : AirAndWaterRandomPos.getPos(this.bird, range, 4, -2, view.x, view.z, (float) (Math.PI / 2.0));
    }

    @Override
    public boolean canContinueToUse() {
        return !this.bird.isBusy() && super.canContinueToUse();
    }
}
