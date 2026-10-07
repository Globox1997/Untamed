package net.untamed.entity.ai.octopus;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.untamed.entity.OctopusEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class InvestigatePlayerGoal extends Goal {

    private static final double NOTICE_DISTANCE = 8.0D;
    private static final double RELEASE_DISTANCE = 10.0D;
    private static final double REACH_DISTANCE = 1.6D;
    private static final double STILL_MOVEMENT_SQR = 0.0004D;
    private static final int REACH_TICKS = 60;
    private static final int MAX_TICKS = 400;
    private static final int COOLDOWN = 1200;

    private final OctopusEntity octopus;
    @Nullable
    private Player player;
    private int ticks;
    private int reachTicks;
    private int repathTicks;
    private long nextAttemptTime;

    public InvestigatePlayerGoal(OctopusEntity octopus) {
        this.octopus = octopus;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.octopus.isInWater() || this.octopus.isResting() || this.octopus.isThreatened() || this.octopus.level().getGameTime() < this.nextAttemptTime
                || this.octopus.getRandom().nextInt(reducedTickDelay(100)) != 0) {
            return false;
        }
        this.player = this.octopus.level().getNearestPlayer(this.octopus.getX(), this.octopus.getY(), this.octopus.getZ(), NOTICE_DISTANCE,
                entity -> EntitySelector.NO_SPECTATORS.test(entity) && entity.isAlive() && entity.isInWater() && this.isKeepingStill(entity));
        return this.player != null;
    }

    private boolean isKeepingStill(Entity entity) {
        return Mth.square(entity.getX() - entity.xo) + Mth.square(entity.getY() - entity.yo) + Mth.square(entity.getZ() - entity.zo) < STILL_MOVEMENT_SQR;
    }

    @Override
    public boolean canContinueToUse() {
        return this.player != null && this.player.isAlive() && !this.octopus.isThreatened() && this.ticks < MAX_TICKS && this.reachTicks < REACH_TICKS
                && this.octopus.distanceToSqr(this.player) < RELEASE_DISTANCE * RELEASE_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.reachTicks = 0;
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        if (this.player == null) {
            return;
        }
        this.ticks++;
        this.octopus.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
        if (this.octopus.distanceTo(this.player) > REACH_DISTANCE) {
            this.octopus.clearOctopusPose(OctopusEntity.OctopusPose.REACH);
            if (--this.repathTicks <= 0) {
                this.repathTicks = 10;
                this.octopus.getNavigation().moveTo(this.player, 0.8D);
            }
        } else {
            this.octopus.getNavigation().stop();
            this.octopus.setOctopusPose(OctopusEntity.OctopusPose.REACH);
            this.reachTicks++;
        }
    }

    @Override
    public void stop() {
        this.octopus.clearOctopusPose(OctopusEntity.OctopusPose.REACH);
        this.octopus.getNavigation().stop();
        this.player = null;
        this.nextAttemptTime = this.octopus.level().getGameTime() + COOLDOWN;
    }
}
