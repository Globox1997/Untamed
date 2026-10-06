package net.untamed.entity.ai.herd;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HerdBovineEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class FollowMotherGoal extends Goal {

    private static final double START_DISTANCE = 5.0D;
    private static final double STOP_DISTANCE = 2.5D;
    private static final double RUN_DISTANCE = 12.0D;
    private static final double LEAD_DISTANCE = 2.5D;

    private final HerdBovineEntity calf;
    @Nullable
    private HerdBovineEntity mother;
    private int repathTicks;

    public FollowMotherGoal(HerdBovineEntity calf) {
        this.calf = calf;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.calf.isBaby()) {
            return false;
        }
        this.mother = this.calf.getMother();
        return this.mother != null && this.calf.position().distanceToSqr(this.destination()) > START_DISTANCE * START_DISTANCE;
    }

    // Calves of species that lead (white rhino) keep a spot in front of the mother instead of following her
    private Vec3 destination() {
        if (!this.calf.calfLeadsMother()) {
            return this.mother.position();
        }
        float yaw = this.mother.yBodyRot * Mth.DEG_TO_RAD;
        return this.mother.position().add(-Mth.sin(yaw) * LEAD_DISTANCE, 0.0D, Mth.cos(yaw) * LEAD_DISTANCE);
    }

    @Override
    public boolean canContinueToUse() {
        return this.calf.isBaby() && this.mother != null && this.mother.isAlive() && this.calf.position().distanceToSqr(this.destination()) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void stop() {
        this.mother = null;
        this.calf.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.mother != null && --this.repathTicks <= 0) {
            this.repathTicks = this.adjustedTickDelay(10);
            Vec3 destination = this.destination();
            double speed = this.calf.position().distanceToSqr(destination) > RUN_DISTANCE * RUN_DISTANCE ? 1.5D : 1.15D;
            this.calf.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
        }
    }
}
