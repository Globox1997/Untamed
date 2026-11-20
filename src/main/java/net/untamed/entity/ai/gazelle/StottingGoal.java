package net.untamed.entity.ai.gazelle;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.GazelleEntity;

import java.util.EnumSet;

public class StottingGoal extends Goal {

    private static final int MIN_JUMPS = 3;
    private static final int JUMPS_VARIATION = 3;
    private static final int JUMP_INTERVAL = 8;
    private static final double JUMP_VELOCITY = 0.5D;
    private static final double AWAY_PUSH = 0.1D;
    private static final int COOLDOWN = 400;

    private final GazelleEntity gazelle;
    private int jumpsLeft;
    private int nextJumpTicks;

    public StottingGoal(GazelleEntity gazelle) {
        this.gazelle = gazelle;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity predator = this.gazelle.getNearestPredator();
        if (predator == null || !this.gazelle.canStot() || !this.gazelle.onGround() || this.gazelle.isInWater()) {
            return false;
        }
        double distance = this.gazelle.distanceTo(predator);
        return distance >= GazelleEntity.FLEE_DISTANCE && distance <= GazelleEntity.STOT_DISTANCE;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity predator = this.gazelle.getNearestPredator();
        return this.jumpsLeft > 0 && predator != null && predator.isAlive() && this.gazelle.distanceTo(predator) >= GazelleEntity.FLEE_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.jumpsLeft = MIN_JUMPS + this.gazelle.getRandom().nextInt(JUMPS_VARIATION);
        this.nextJumpTicks = 0;
        this.gazelle.getNavigation().stop();
        this.gazelle.setStotting(true);
    }

    @Override
    public void tick() {
        LivingEntity predator = this.gazelle.getNearestPredator();
        if (predator == null) {
            return;
        }
        this.gazelle.getLookControl().setLookAt(predator, 30.0F, 30.0F);
        if (this.nextJumpTicks > 0) {
            this.nextJumpTicks--;
            return;
        }
        if (!this.gazelle.onGround()) {
            return;
        }
        Vec3 away = this.gazelle.position().subtract(predator.position()).multiply(1.0D, 0.0D, 1.0D);
        away = away.lengthSqr() > 1.0E-4D ? away.normalize().scale(AWAY_PUSH) : Vec3.ZERO;
        this.gazelle.setDeltaMovement(away.x, JUMP_VELOCITY, away.z);
        this.gazelle.hasImpulse = true;
        this.jumpsLeft--;
        this.nextJumpTicks = JUMP_INTERVAL;
    }

    @Override
    public void stop() {
        this.gazelle.setStotting(false);
        this.gazelle.setStotCooldown(COOLDOWN);
    }
}
