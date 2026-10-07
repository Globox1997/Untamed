package net.untamed.entity.ai.panther;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.BlackPantherEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PantherThreatResponseGoal extends Goal {

    private static final double NOTICE_DISTANCE = 14.0D;
    private static final double RESTING_NOTICE_DISTANCE = 8.0D;
    private static final double SNARL_DISTANCE = 3.0D;
    private static final double CUB_GUARD_DISTANCE = 8.0D;
    private static final double CUB_RANGE = 10.0D;
    private static final double TOO_CLOSE = 2.0D;
    private static final int SNARL_TICKS_BEFORE_ATTACK = 40;
    private static final int COVER_TRIES = 6;
    private static final int MAX_TOLERANCE = 600;

    private enum Stage {
        NONE, HIDE, GUARD, SNARL, FLEE
    }

    private final BlackPantherEntity panther;
    @Nullable
    private Player player;
    private int snarlTicks;
    private int repathTicks;
    private boolean cornered;

    public PantherThreatResponseGoal(BlackPantherEntity panther) {
        this.panther = panther;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.panther.getTarget() != null) {
            return false;
        }
        this.player = this.panther.level().getNearestPlayer(this.panther.getX(), this.panther.getY(), this.panther.getZ(), NOTICE_DISTANCE,
                entity -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity) && entity.isAlive());
        if (this.player == null) {
            return false;
        }
        Stage stage = this.computeStage(this.player);
        return stage != Stage.NONE && !(this.panther.isResting() && stage == Stage.HIDE && this.panther.distanceTo(this.player) > RESTING_NOTICE_DISTANCE);
    }

    @Override
    public boolean canContinueToUse() {
        return this.panther.getTarget() == null && this.player != null && this.player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(this.player)
                && this.computeStage(this.player) != Stage.NONE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.snarlTicks = 0;
        this.repathTicks = 0;
        this.cornered = false;
        this.panther.clearPantherPose(BlackPantherEntity.PantherPose.RESTING);
    }

    @Override
    public void stop() {
        this.panther.clearPantherPose(BlackPantherEntity.PantherPose.SNARLING);
        this.panther.getNavigation().stop();
        this.player = null;
    }

    @Override
    public void tick() {
        if (this.player == null) {
            return;
        }
        Stage stage = this.computeStage(this.player);
        if (stage != Stage.SNARL) {
            this.panther.clearPantherPose(BlackPantherEntity.PantherPose.SNARLING);
            this.snarlTicks = Math.max(0, this.snarlTicks - 1);
        }
        this.panther.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
        switch (stage) {
            case HIDE -> this.slipIntoCover(1.1D);
            case FLEE -> this.fleeToMother();
            case GUARD -> {
                this.panther.getNavigation().stop();
                this.panther.addTolerance(this.player, 1, MAX_TOLERANCE);
            }
            case SNARL -> {
                this.panther.getNavigation().stop();
                this.panther.setPantherPose(BlackPantherEntity.PantherPose.SNARLING);
                this.panther.playWarningSound();
                this.panther.alarm(100);
                this.snarlTicks++;
                if (this.snarlTicks >= SNARL_TICKS_BEFORE_ATTACK || this.panther.distanceTo(this.player) < TOO_CLOSE) {
                    this.panther.provokeBy(this.player);
                }
            }
            default -> {
            }
        }
    }

    private void slipIntoCover(double speed) {
        if (--this.repathTicks > 0 && !this.panther.getNavigation().isDone()) {
            return;
        }
        this.repathTicks = 20;
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < COVER_TRIES; i++) {
            Vec3 candidate = LandRandomPos.getPosAway(this.panther, 12, 6, this.player.position());
            if (candidate != null) {
                double score = this.panther.scoreCoverSpot(BlockPos.containing(candidate));
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
        }
        this.cornered = best == null || !this.panther.getNavigation().moveTo(best.x, best.y, best.z, speed);
    }

    private void fleeToMother() {
        if (--this.repathTicks > 0 && !this.panther.getNavigation().isDone()) {
            return;
        }
        this.repathTicks = 20;
        BlackPantherEntity mother = this.panther.getMother();
        BlockPos den = this.panther.getDenPos();
        if (mother != null) {
            this.panther.getNavigation().moveTo(mother, 1.4D);
        } else if (den != null) {
            this.panther.getNavigation().moveTo(den.getX() + 0.5D, den.getY(), den.getZ() + 0.5D, 1.4D);
        } else {
            this.slipIntoCover(1.4D);
        }
    }

    private Stage computeStage(Player player) {
        if (this.panther.isFood(player.getMainHandItem()) || this.panther.isFood(player.getOffhandItem())) {
            return Stage.NONE;
        }
        double distance = this.panther.distanceTo(player);
        if (this.panther.isBaby()) {
            return distance < NOTICE_DISTANCE * 0.6D ? Stage.FLEE : Stage.NONE;
        }
        double scale = 1.0D - 0.5D * this.panther.getTolerance(player) / MAX_TOLERANCE;
        if (player.isDiscrete()) {
            scale *= 0.6D;
        }
        if (this.panther.hasOwnCubNearby(CUB_RANGE)) {
            if (distance < CUB_GUARD_DISTANCE * scale) {
                return Stage.SNARL;
            }
            return distance < NOTICE_DISTANCE * scale ? Stage.GUARD : Stage.NONE;
        }
        if (distance < SNARL_DISTANCE * scale || (this.cornered && distance < NOTICE_DISTANCE * 0.5D * scale)) {
            return Stage.SNARL;
        }
        return distance < NOTICE_DISTANCE * scale ? Stage.HIDE : Stage.NONE;
    }
}
