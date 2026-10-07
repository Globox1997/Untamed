package net.untamed.entity.ai.hyena;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HyenaEntity;
import org.jetbrains.annotations.Nullable;

public class HyenaStrollGoal extends RandomStrollGoal {

    private static final int STROLL_INTERVAL = 60;
    private static final double DEN_DRIFT_DISTANCE = 12.0D;

    private final HyenaEntity hyena;

    public HyenaStrollGoal(HyenaEntity hyena, double speedModifier) {
        super(hyena, speedModifier, STROLL_INTERVAL);
        this.hyena = hyena;
    }

    @Override
    public boolean canUse() {
        return !this.hyena.isResting() && super.canUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        BlockPos den = this.hyena.getDenPos();
        if (den != null && (this.hyena.isRestTime() || this.hyena.isBaby()) && !this.hyena.isNearDen(DEN_DRIFT_DISTANCE)) {
            return LandRandomPos.getPosTowards(this.hyena, 8, 7, Vec3.atBottomCenterOf(den));
        }
        return LandRandomPos.getPos(this.hyena, 8, 7);
    }
}
