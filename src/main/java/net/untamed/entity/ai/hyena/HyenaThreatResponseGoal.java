package net.untamed.entity.ai.hyena;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HyenaEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class HyenaThreatResponseGoal extends Goal {

    private static final double NOTICE_DISTANCE = 16.0D;
    private static final double LONE_AVOID_DISTANCE = 10.0D;
    private static final double GROUP_AVOID_DISTANCE = 5.0D;
    private static final double WARN_DISTANCE = 6.0D;
    private static final double FOLLOW_MIN = 7.0D;
    private static final double FOLLOW_MAX = 11.0D;
    private static final double GROUP_RANGE = 12.0D;
    private static final double CUB_RANGE = 10.0D;
    private static final double DEN_RANGE = 16.0D;
    private static final double TOO_CLOSE = 2.5D;
    private static final double CLAN_RALLY_RANGE = 24.0D;
    private static final int WARN_TICKS_BEFORE_ATTACK = 40;
    private static final int MAX_TOLERANCE = 600;

    private enum Stage {
        NONE, AVOID, WATCH, FOLLOW, WARN, FLEE
    }

    private final HyenaEntity hyena;
    @Nullable
    private Player player;
    private int warnTicks;
    private int repathCooldown;

    public HyenaThreatResponseGoal(HyenaEntity hyena) {
        this.hyena = hyena;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.hyena.getTarget() != null || this.hyena.getHunt() != null) {
            return false;
        }
        this.player = this.hyena.level().getNearestPlayer(this.hyena.getX(), this.hyena.getY(), this.hyena.getZ(), NOTICE_DISTANCE,
                entity -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity) && entity.isAlive());
        if (this.player == null) {
            return false;
        }
        Stage stage = this.computeStage(this.player);
        return stage != Stage.NONE && !(this.hyena.isResting() && (stage == Stage.WATCH || stage == Stage.FOLLOW));
    }

    @Override
    public boolean canContinueToUse() {
        return this.hyena.getTarget() == null && this.player != null && this.player.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(this.player)
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
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.RESTING);
    }

    @Override
    public void stop() {
        this.hyena.getNavigation().stop();
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.ALERT);
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.EXCITED);
        this.player = null;
    }

    @Override
    public void tick() {
        if (this.player == null) {
            return;
        }
        double distance = this.hyena.distanceTo(this.player);
        if (!this.hyena.isBaby() && this.courage(this.player) >= 3 && this.player.getHealth() <= this.player.getMaxHealth() * 0.3F && distance < 10.0D) {
            this.hyena.provokeBy(this.player, CLAN_RALLY_RANGE);
            return;
        }
        Stage stage = this.computeStage(this.player);
        if (stage != Stage.WARN) {
            this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.EXCITED);
            this.warnTicks = Math.max(0, this.warnTicks - 1);
        }
        if (stage != Stage.WATCH && stage != Stage.AVOID) {
            this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.ALERT);
        }
        this.hyena.getLookControl().setLookAt(this.player, 30.0F, 30.0F);

        switch (stage) {
            case WATCH -> {
                this.hyena.getNavigation().stop();
                this.hyena.setHyenaPose(HyenaEntity.HyenaPose.ALERT);
                this.hyena.addTolerance(this.player, 1, MAX_TOLERANCE);
            }
            case FOLLOW -> {
                if (distance > FOLLOW_MAX && --this.repathCooldown <= 0) {
                    this.repathCooldown = 20;
                    this.hyena.getNavigation().moveTo(this.player, 1.0D);
                } else if (distance < FOLLOW_MIN) {
                    this.hyena.getNavigation().stop();
                }
                this.hyena.addTolerance(this.player, 1, MAX_TOLERANCE);
            }
            case AVOID -> {
                this.hyena.setHyenaPose(HyenaEntity.HyenaPose.ALERT);
                this.moveAway(1.2D);
            }
            case FLEE -> this.fleeToClan();
            case WARN -> {
                this.hyena.getNavigation().stop();
                this.hyena.setHyenaPose(HyenaEntity.HyenaPose.EXCITED);
                this.hyena.playWarningSound();
                this.hyena.alarmClan(100, 24.0D);
                this.warnTicks++;
                if (this.warnTicks >= WARN_TICKS_BEFORE_ATTACK || distance < TOO_CLOSE) {
                    this.hyena.provokeBy(this.player, CLAN_RALLY_RANGE);
                }
            }
            default -> {
            }
        }
    }

    private void moveAway(double speed) {
        if (--this.repathCooldown > 0 && !this.hyena.getNavigation().isDone()) {
            return;
        }
        this.repathCooldown = 20;
        Vec3 away = DefaultRandomPos.getPosAway(this.hyena, 10, 7, this.player.position());
        if (away != null) {
            this.hyena.getNavigation().moveTo(away.x, away.y, away.z, speed);
        }
    }

    private void fleeToClan() {
        if (--this.repathCooldown > 0 && !this.hyena.getNavigation().isDone()) {
            return;
        }
        this.repathCooldown = 20;
        HyenaEntity nearestAdult = null;
        for (HyenaEntity member : this.hyena.getClan()) {
            if (member.isAlive() && !member.isBaby() && (nearestAdult == null || this.hyena.distanceToSqr(member) < this.hyena.distanceToSqr(nearestAdult))) {
                nearestAdult = member;
            }
        }
        if (nearestAdult != null) {
            this.hyena.getNavigation().moveTo(nearestAdult, 1.5D);
        } else if (this.hyena.getDenPos() != null) {
            Vec3 den = Vec3.atBottomCenterOf(this.hyena.getDenPos());
            this.hyena.getNavigation().moveTo(den.x, den.y, den.z, 1.5D);
        } else {
            this.moveAway(1.5D);
        }
    }

    private int courage(Player player) {
        int courage = 1 + this.hyena.countAdultsNearby(GROUP_RANGE);
        if (this.hyena.level().isNight()) {
            courage++;
        }
        if (player.getHealth() <= player.getMaxHealth() * 0.5F) {
            courage++;
        }
        return courage;
    }

    private Stage computeStage(Player player) {
        if (this.hyena.isFood(player.getMainHandItem()) || this.hyena.isFood(player.getOffhandItem())) {
            return Stage.NONE;
        }
        double distance = this.hyena.distanceTo(player);
        if (this.hyena.isBaby()) {
            return distance < LONE_AVOID_DISTANCE ? Stage.FLEE : Stage.NONE;
        }
        double scale = 1.0D - 0.5D * this.hyena.getTolerance(player) / MAX_TOLERANCE;
        if (player.isDiscrete()) {
            scale *= 0.6D;
        }
        boolean defending = this.hyena.hasCubNearby(CUB_RANGE) || this.hyena.isNearDen(DEN_RANGE);
        int courage = this.courage(player);
        if (defending || courage >= 4) {
            if (distance < WARN_DISTANCE * scale) {
                return Stage.WARN;
            }
            return distance < NOTICE_DISTANCE * scale ? Stage.WATCH : Stage.NONE;
        }
        if (courage >= 2) {
            if (distance < GROUP_AVOID_DISTANCE * scale) {
                return Stage.AVOID;
            }
            return distance < NOTICE_DISTANCE * scale ? Stage.FOLLOW : Stage.NONE;
        }
        if (distance < LONE_AVOID_DISTANCE * scale) {
            return Stage.AVOID;
        }
        return distance < NOTICE_DISTANCE * scale ? Stage.WATCH : Stage.NONE;
    }
}
