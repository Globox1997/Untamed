package net.untamed.entity.ai.gazelle;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.GazelleEntity;

import java.util.EnumSet;

public class GazelleFleeGoal extends Goal {

    private static final double FLEE_SPEED = 2.0D;
    private static final double SAFE_DISTANCE = 20.0D;
    private static final int REPATH_TICKS = 10;
    private static final int ALARM_TICKS = 40;
    private static final int HERD_ALARM_TICKS = 100;
    private static final double HERD_ALARM_RANGE = 16.0D;

    private final GazelleEntity gazelle;
    private int repathTicks;

    public GazelleFleeGoal(GazelleEntity gazelle) {
        this.gazelle = gazelle;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity predator = this.gazelle.getNearestPredator();
        return predator != null && this.gazelle.distanceTo(predator) < GazelleEntity.FLEE_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity predator = this.gazelle.getNearestPredator();
        return predator != null && predator.isAlive() && this.gazelle.distanceTo(predator) < SAFE_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
        this.gazelle.alarmHerd(HERD_ALARM_TICKS, HERD_ALARM_RANGE);
    }

    @Override
    public void tick() {
        LivingEntity predator = this.gazelle.getNearestPredator();
        if (predator == null) {
            return;
        }
        this.gazelle.alarm(ALARM_TICKS);
        if (--this.repathTicks <= 0 || this.gazelle.getNavigation().isDone()) {
            this.repathTicks = REPATH_TICKS;
            Vec3 away = LandRandomPos.getPosAway(this.gazelle, 16, 7, predator.position());
            if (away != null) {
                this.gazelle.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED);
            }
        }
    }

    @Override
    public void stop() {
        this.gazelle.getNavigation().stop();
    }
}
