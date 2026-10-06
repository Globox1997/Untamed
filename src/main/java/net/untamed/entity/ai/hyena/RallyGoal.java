package net.untamed.entity.ai.hyena;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HyenaEntity;

import java.util.EnumSet;

// Answers a recruiting whoop: runs to the caller, then joins its hunt or its fight
public class RallyGoal extends Goal {

    private static final double ARRIVE_DISTANCE = 6.0D;

    private final HyenaEntity hyena;
    private int repathTicks;

    public RallyGoal(HyenaEntity hyena) {
        this.hyena = hyena;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.hyena.getRallyPos() != null && this.hyena.getTarget() == null && this.hyena.getHunt() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() && this.hyena.position().distanceToSqr(this.destination()) > ARRIVE_DISTANCE * ARRIVE_DISTANCE;
    }

    private Vec3 destination() {
        HyenaEntity caller = this.hyena.getRallyCaller();
        Vec3 rallyPos = this.hyena.getRallyPos();
        return caller != null && caller.isAlive() ? caller.position() : rallyPos != null ? rallyPos : this.hyena.position();
    }

    @Override
    public void start() {
        this.repathTicks = 0;
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.RESTING);
    }

    @Override
    public void tick() {
        if (--this.repathTicks <= 0 || this.hyena.getNavigation().isDone()) {
            this.repathTicks = this.adjustedTickDelay(20);
            Vec3 destination = this.destination();
            this.hyena.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.3D);
        }
    }

    @Override
    public void stop() {
        this.hyena.getNavigation().stop();
        if (this.hyena.getRallyPos() != null && this.hyena.position().distanceToSqr(this.destination()) <= ARRIVE_DISTANCE * ARRIVE_DISTANCE) {
            HyenaEntity caller = this.hyena.getRallyCaller();
            if (caller != null && caller.isAlive()) {
                HyenaHunt hunt = caller.getHunt();
                LivingEntity enemy = caller.getTarget();
                if (hunt != null && !hunt.isFinished()) {
                    hunt.addHunter(this.hyena);
                } else if (enemy != null && enemy.isAlive() && !(enemy instanceof HyenaEntity other && other.isInClanWith(this.hyena))) {
                    this.hyena.setTarget(enemy);
                }
            }
            this.hyena.clearRally();
        }
    }
}
