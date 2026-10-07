package net.untamed.entity.ai.lion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractLionEntity;
import org.jetbrains.annotations.Nullable;

public class PrideStrollGoal extends RandomStrollGoal {

    private static final int STROLL_INTERVAL = 60;
    private static final double DRIFT_DISTANCE = 8.0D;

    private final AbstractLionEntity lion;

    public PrideStrollGoal(AbstractLionEntity lion, double speedModifier) {
        super(lion, speedModifier, STROLL_INTERVAL);
        this.lion = lion;
    }

    @Override
    public boolean canUse() {
        return !this.lion.isSleeping() && super.canUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        if (this.lion.isNomad()) {
            return LandRandomPos.getPos(this.lion, 20, 7);
        }
        Vec3 center = this.lion.getPrideCenter();
        if (center != null && this.lion.position().distanceToSqr(center) > DRIFT_DISTANCE * DRIFT_DISTANCE) {
            return LandRandomPos.getPosTowards(this.lion, 8, 7, center);
        }
        BlockPos home = this.lion.getHomePos();
        if (home != null && this.lion.blockPosition().distSqr(home) > AbstractLionEntity.TERRITORY_RADIUS * AbstractLionEntity.TERRITORY_RADIUS) {
            return LandRandomPos.getPosTowards(this.lion, 12, 7, Vec3.atBottomCenterOf(home));
        }
        return LandRandomPos.getPos(this.lion, 10, 7);
    }
}
