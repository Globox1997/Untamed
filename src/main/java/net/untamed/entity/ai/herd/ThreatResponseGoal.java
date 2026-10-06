package net.untamed.entity.ai.herd;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HerdBovineEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class ThreatResponseGoal extends Goal {

    private static final int MAX_TOLERANCE = 600;
    private static final double CALF_PROTECT_RANGE = 8.0D;
    private static final double CALF_WARN_BONUS = 3.0D;
    private static final double TOO_CLOSE = 2.5D;

    private enum Stage {
        NONE, WATCH, FLEE, WARN
    }

    private final HerdBovineEntity mob;
    @Nullable
    private Player player;
    private Stage stage = Stage.NONE;
    private int warnTicks;
    private int repathCooldown;

    public ThreatResponseGoal(HerdBovineEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null) {
            return false;
        }
        this.player = this.mob.level().getNearestPlayer(this.mob.getX(), this.mob.getY(), this.mob.getZ(), this.mob.getThreatProfile().watchDistance(),
                entity -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity) && entity.isAlive());
        return this.player != null && this.computeStage(this.player) != Stage.NONE;
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.getTarget() == null && this.player != null && this.player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(this.player)
                && this.computeStage(this.player) != Stage.NONE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.warnTicks = 0;
        this.repathCooldown = 0;
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.mob.clearHerdPose(HerdBovineEntity.HerdPose.WARNING);
        this.player = null;
        this.stage = Stage.NONE;
    }

    @Override
    public void tick() {
        if (this.player == null) {
            return;
        }
        Stage newStage = this.computeStage(this.player);
        if (newStage != Stage.WARN) {
            this.mob.clearHerdPose(HerdBovineEntity.HerdPose.WARNING);
        }
        if (newStage != Stage.FLEE && this.stage == Stage.FLEE) {
            this.mob.getNavigation().stop();
        }
        this.stage = newStage;

        switch (this.stage) {
            case WATCH -> {
                this.mob.getNavigation().stop();
                this.mob.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
                this.warnTicks = Math.max(0, this.warnTicks - 1);
                this.mob.addTolerance(this.player, 1, MAX_TOLERANCE);
            }
            case FLEE -> {
                this.warnTicks = Math.max(0, this.warnTicks - 1);
                this.mob.alarm(40);
                if (--this.repathCooldown <= 0 || this.mob.getNavigation().isDone()) {
                    this.repathCooldown = this.adjustedTickDelay(20);
                    Vec3 away = DefaultRandomPos.getPosAway(this.mob, 12, 7, this.player.position());
                    if (away != null) {
                        this.mob.getNavigation().moveTo(away.x, away.y, away.z, 1.3D);
                    }
                }
            }
            case WARN -> {
                this.mob.getNavigation().stop();
                this.mob.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
                this.mob.setHerdPose(HerdBovineEntity.HerdPose.WARNING);
                this.mob.playWarningSound();
                this.mob.alarmHerd(100, 16.0D);
                this.warnTicks++;
                if (this.warnTicks >= this.mob.getThreatProfile().warnTicksBeforeCharge() || this.mob.distanceTo(this.player) < TOO_CLOSE) {
                    this.mob.provokeBy(this.player);
                }
            }
            default -> {
            }
        }
    }

    private Stage computeStage(Player player) {
        if (this.mob.isFood(player.getMainHandItem()) || this.mob.isFood(player.getOffhandItem())) {
            return Stage.NONE;
        }
        HerdBovineEntity.ThreatProfile profile = this.mob.getThreatProfile();
        double scale = 1.0D - 0.5D * this.mob.getTolerance(player) / MAX_TOLERANCE;
        if (player.isDiscrete()) {
            scale *= 0.6D;
        }
        double distance = this.mob.distanceTo(player);

        if (this.mob.isBaby()) {
            return distance < profile.fleeDistance() * scale ? Stage.FLEE : Stage.NONE;
        }

        boolean protectingCalf = this.mob.hasCalfNearby(CALF_PROTECT_RANGE);
        double warn = profile.warnDistance() * scale + (protectingCalf ? CALF_WARN_BONUS : 0.0D);
        if (distance < warn) {
            return Stage.WARN;
        }
        if (distance < profile.fleeDistance() * scale) {
            return protectingCalf ? Stage.WARN : Stage.FLEE;
        }
        if (distance < profile.watchDistance() * scale) {
            return Stage.WATCH;
        }
        return Stage.NONE;
    }
}
