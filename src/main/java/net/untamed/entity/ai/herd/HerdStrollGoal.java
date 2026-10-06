package net.untamed.entity.ai.herd;

import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HerdBovineEntity;
import org.jetbrains.annotations.Nullable;

public class HerdStrollGoal extends RandomStrollGoal {

    private static final double DRIFT_DISTANCE = 6.0D;
    private static final double LEADER_WAIT_DISTANCE = 12.0D;
    private static final int TRAVEL_INTERVAL = 40;

    private final HerdBovineEntity herdMob;

    public HerdStrollGoal(HerdBovineEntity mob, double speedModifier) {
        super(mob, speedModifier);
        this.herdMob = mob;
    }

    @Override
    public boolean canUse() {
        this.setInterval(this.herdMob.getHerdTravelTarget() != null ? TRAVEL_INTERVAL : DEFAULT_INTERVAL);
        return super.canUse();
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        Vec3 travelTarget = this.herdMob.getHerdTravelTarget();
        if (travelTarget != null) {
            return LandRandomPos.getPosTowards(this.herdMob, 10, 7, travelTarget);
        }
        Vec3 center = this.herdMob.getHerdCenter();
        if (center != null) {
            double distanceSqr = this.herdMob.position().distanceToSqr(center);
            if (!this.herdMob.followsHerd()) {
                return LandRandomPos.getPos(this.herdMob, 10, 7);
            }
            if (this.herdMob.isHerdLeader()) {
                if (distanceSqr > LEADER_WAIT_DISTANCE * LEADER_WAIT_DISTANCE) {
                    return null;
                }
            } else if (distanceSqr > DRIFT_DISTANCE * DRIFT_DISTANCE) {
                return LandRandomPos.getPosTowards(this.herdMob, 8, 7, center);
            }
        }
        return LandRandomPos.getPos(this.herdMob, 10, 7);
    }
}
