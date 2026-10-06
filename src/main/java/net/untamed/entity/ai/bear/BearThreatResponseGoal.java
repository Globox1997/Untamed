package net.untamed.entity.ai.bear;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.BlackBearEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BearThreatResponseGoal extends Goal {

    private static final double NOTICE_DISTANCE = 16.0D;
    private static final double LEAVE_DISTANCE = 8.0D;
    private static final double HUFF_DISTANCE = 4.0D;
    private static final double CUB_HUFF_DISTANCE = 8.0D;
    private static final double CUB_RANGE = 10.0D;
    private static final double TOO_CLOSE = 2.0D;
    private static final double BLUFF_STOP = 2.5D;
    private static final int STAND_TICKS = 50;
    private static final int HUFF_TICKS_BEFORE_BLUFF = 30;
    private static final int BLUFF_TICKS = 20;
    private static final int RETREAT_TICKS = 30;
    private static final int MAX_BLUFFS = 2;
    private static final int MAX_TOLERANCE = 600;

    private enum Stage {
        NONE, WATCH, LEAVE, HUFF, FLEE
    }

    private enum Action {
        NONE, BLUFF, RETREAT
    }

    private final BlackBearEntity bear;
    @Nullable
    private Player player;
    private Action action = Action.NONE;
    private boolean stoodUp;
    private int standTicks;
    private int huffTicks;
    private int actionTicks;
    private int bluffs;
    private int repathCooldown;

    public BearThreatResponseGoal(BlackBearEntity bear) {
        this.bear = bear;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.bear.getTarget() != null) {
            return false;
        }
        this.player = this.bear.level().getNearestPlayer(this.bear.getX(), this.bear.getY(), this.bear.getZ(), NOTICE_DISTANCE,
                entity -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity) && entity.isAlive());
        if (this.player == null) {
            return false;
        }
        Stage stage = this.computeStage(this.player);
        return stage != Stage.NONE && !(this.bear.isResting() && stage == Stage.WATCH);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.bear.getTarget() != null || this.player == null || !this.player.isAlive() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(this.player)) {
            return false;
        }
        return this.action != Action.NONE || this.computeStage(this.player) != Stage.NONE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.action = Action.NONE;
        this.stoodUp = false;
        this.standTicks = 0;
        this.huffTicks = 0;
        this.bluffs = 0;
        this.repathCooldown = 0;
        this.bear.clearBearPose(BlackBearEntity.BearPose.RESTING);
    }

    @Override
    public void stop() {
        this.bear.getNavigation().stop();
        this.bear.clearBearPose(BlackBearEntity.BearPose.STANDING);
        this.bear.clearBearPose(BlackBearEntity.BearPose.HUFFING);
        this.player = null;
        this.action = Action.NONE;
    }

    @Override
    public void tick() {
        if (this.player == null) {
            return;
        }
        double distance = this.bear.distanceTo(this.player);
        this.bear.getLookControl().setLookAt(this.player, 30.0F, 30.0F);

        if (this.action == Action.BLUFF) {
            this.actionTicks--;
            if (--this.repathCooldown <= 0) {
                this.repathCooldown = 5;
                this.bear.getNavigation().moveTo(this.player, 1.8D);
            }
            if (distance <= BLUFF_STOP || this.actionTicks <= 0) {
                this.bluffs++;
                this.action = Action.RETREAT;
                this.actionTicks = RETREAT_TICKS;
                this.bear.playWarningSound();
                Vec3 back = DefaultRandomPos.getPosAway(this.bear, 6, 3, this.player.position());
                if (back != null) {
                    this.bear.getNavigation().moveTo(back.x, back.y, back.z, 1.0D);
                } else {
                    this.bear.getNavigation().stop();
                }
            }
            return;
        }
        if (this.action == Action.RETREAT) {
            if (--this.actionTicks <= 0 || this.bear.getNavigation().isDone()) {
                this.action = Action.NONE;
                this.huffTicks = 0;
            }
            return;
        }

        Stage stage = this.computeStage(this.player);
        if (stage != Stage.HUFF) {
            this.bear.clearBearPose(BlackBearEntity.BearPose.HUFFING);
            this.huffTicks = Math.max(0, this.huffTicks - 1);
        }
        if (stage != Stage.WATCH) {
            this.bear.clearBearPose(BlackBearEntity.BearPose.STANDING);
            this.standTicks = 0;
        }

        switch (stage) {
            case WATCH -> {
                this.bear.getNavigation().stop();
                if (!this.stoodUp) {
                    this.stoodUp = true;
                    this.standTicks = STAND_TICKS;
                    this.bear.setBearPose(BlackBearEntity.BearPose.STANDING);
                }
                if (this.standTicks > 0 && --this.standTicks == 0) {
                    this.bear.clearBearPose(BlackBearEntity.BearPose.STANDING);
                }
                this.bear.addTolerance(this.player, 1, MAX_TOLERANCE);
            }
            case LEAVE -> this.moveAway(1.0D);
            case FLEE -> this.fleeToAdult();
            case HUFF -> {
                this.bear.getNavigation().stop();
                this.bear.setBearPose(BlackBearEntity.BearPose.HUFFING);
                this.bear.playWarningSound();
                this.bear.alarm(100);
                this.huffTicks++;
                if (distance < TOO_CLOSE) {
                    this.bear.provokeBy(this.player);
                } else if (this.huffTicks >= HUFF_TICKS_BEFORE_BLUFF) {
                    if (this.bluffs < MAX_BLUFFS) {
                        this.bear.clearBearPose(BlackBearEntity.BearPose.HUFFING);
                        this.action = Action.BLUFF;
                        this.actionTicks = BLUFF_TICKS;
                        this.repathCooldown = 0;
                    } else {
                        this.bear.provokeBy(this.player);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void moveAway(double speed) {
        if (--this.repathCooldown > 0 && !this.bear.getNavigation().isDone()) {
            return;
        }
        this.repathCooldown = 20;
        Vec3 away = DefaultRandomPos.getPosAway(this.bear, 12, 7, this.player.position());
        if (away != null) {
            this.bear.getNavigation().moveTo(away.x, away.y, away.z, speed);
        }
    }

    private void fleeToAdult() {
        if (--this.repathCooldown > 0 && !this.bear.getNavigation().isDone()) {
            return;
        }
        this.repathCooldown = 20;
        BlackBearEntity adult = null;
        for (BlackBearEntity other : this.bear.level().getEntitiesOfClass(BlackBearEntity.class, this.bear.getBoundingBox().inflate(NOTICE_DISTANCE),
                other -> other.isAlive() && !other.isBaby())) {
            if (adult == null || this.bear.distanceToSqr(other) < this.bear.distanceToSqr(adult)) {
                adult = other;
            }
        }
        if (adult != null) {
            this.bear.getNavigation().moveTo(adult, 1.5D);
        } else {
            this.moveAway(1.5D);
        }
    }

    private boolean protectingCubs() {
        return !this.bear.isBaby() && !this.bear.level().getEntitiesOfClass(BlackBearEntity.class, this.bear.getBoundingBox().inflate(CUB_RANGE),
                other -> other.isAlive() && other.isBaby()).isEmpty();
    }

    private Stage computeStage(Player player) {
        if (this.bear.isFood(player.getMainHandItem()) || this.bear.isFood(player.getOffhandItem())) {
            return Stage.NONE;
        }
        double distance = this.bear.distanceTo(player);
        if (this.bear.isBaby()) {
            return distance < LEAVE_DISTANCE ? Stage.FLEE : Stage.NONE;
        }
        double scale = 1.0D - 0.5D * this.bear.getTolerance(player) / MAX_TOLERANCE;
        if (player.isDiscrete()) {
            scale *= 0.6D;
        }
        boolean protecting = this.protectingCubs();
        double huff = protecting ? CUB_HUFF_DISTANCE : HUFF_DISTANCE * scale;
        if (distance < huff) {
            return Stage.HUFF;
        }
        if (!protecting && distance < LEAVE_DISTANCE * scale) {
            return Stage.LEAVE;
        }
        return distance < NOTICE_DISTANCE * scale ? Stage.WATCH : Stage.NONE;
    }
}
