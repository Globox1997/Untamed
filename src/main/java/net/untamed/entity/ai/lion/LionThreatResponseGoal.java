package net.untamed.entity.ai.lion;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractLionEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class LionThreatResponseGoal extends Goal {

    private static final int MAX_TOLERANCE = 600;
    private static final double CUB_RANGE = 10.0D;
    private static final double CUB_WARN_BONUS = 4.0D;
    private static final double CUB_MOCK_BONUS = 2.0D;
    private static final double GROUP_RANGE = 12.0D;
    private static final double TOO_CLOSE = 2.0D;
    private static final double MOCK_CHARGE_STOP = 3.0D;
    private static final int MOCK_CHARGE_TICKS = 25;
    private static final int RETREAT_TICKS = 30;
    private static final int MIN_MOCK_CHARGE_STAMINA = 10;

    private enum Stage {
        NONE, WATCH, WARN, FLEE
    }

    private enum Action {
        NONE, MOCK_CHARGE, RETREAT
    }

    private final AbstractLionEntity lion;
    @Nullable
    private Player player;
    private Action action = Action.NONE;
    private int warnTicks;
    private int actionTicks;
    private int mockCharges;
    private int repathCooldown;

    public LionThreatResponseGoal(AbstractLionEntity lion) {
        this.lion = lion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.lion.getTarget() != null || this.lion.getHunt() != null) {
            return false;
        }
        this.player = this.lion.level().getNearestPlayer(this.lion.getX(), this.lion.getY(), this.lion.getZ(), this.lion.getThreatProfile().watchDistance() * 1.5D,
                entity -> EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity) && entity.isAlive());
        if (this.player == null) {
            return false;
        }
        Stage stage = this.computeStage(this.player);
        return stage != Stage.NONE && !(stage == Stage.WATCH && this.lion.isSleeping());
    }

    @Override
    public boolean canContinueToUse() {
        if (this.lion.getTarget() != null || this.player == null || !this.player.isAlive() || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(this.player)) {
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
        this.warnTicks = 0;
        this.mockCharges = 0;
        this.action = Action.NONE;
        this.repathCooldown = 0;
        this.lion.clearLionPose(AbstractLionEntity.LionPose.SLEEPING);
    }

    @Override
    public void stop() {
        this.lion.getNavigation().stop();
        this.lion.clearLionPose(AbstractLionEntity.LionPose.WARNING);
        this.player = null;
        this.action = Action.NONE;
    }

    @Override
    public void tick() {
        if (this.player == null) {
            return;
        }
        double distance = this.lion.distanceTo(this.player);

        if (this.action == Action.MOCK_CHARGE) {
            this.actionTicks--;
            this.lion.drainStamina();
            this.lion.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
            if (--this.repathCooldown <= 0) {
                this.repathCooldown = 5;
                this.lion.getNavigation().moveTo(this.player, 1.9D);
            }
            if (distance <= MOCK_CHARGE_STOP || this.actionTicks <= 0 || this.lion.getStamina() <= 0) {
                this.startRetreat();
            }
            return;
        }
        if (this.action == Action.RETREAT) {
            this.actionTicks--;
            this.lion.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
            if (this.actionTicks <= 0 || this.lion.getNavigation().isDone()) {
                this.action = Action.NONE;
                this.warnTicks = 0;
            }
            return;
        }

        Stage stage = this.computeStage(this.player);
        if (stage != Stage.WARN) {
            this.lion.clearLionPose(AbstractLionEntity.LionPose.WARNING);
        }
        switch (stage) {
            case WATCH -> {
                this.lion.getNavigation().stop();
                this.lion.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
                this.warnTicks = Math.max(0, this.warnTicks - 1);
                this.lion.addTolerance(this.player, 1, MAX_TOLERANCE);
            }
            case FLEE -> this.fleeToAdult();
            case WARN -> {
                this.lion.getNavigation().stop();
                this.lion.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
                this.lion.setLionPose(AbstractLionEntity.LionPose.WARNING);
                this.lion.playWarningSound();
                this.lion.alarmPride(100, 24.0D);
                this.warnTicks++;
                if (distance < TOO_CLOSE) {
                    this.lion.provokeBy(this.player);
                } else if (this.warnTicks >= this.lion.getThreatProfile().warnTicksBeforeMockCharge() || distance < this.mockChargeDistance(this.player)) {
                    if (this.mockCharges < this.allowedMockCharges() && this.lion.getStamina() > MIN_MOCK_CHARGE_STAMINA) {
                        this.action = Action.MOCK_CHARGE;
                        this.actionTicks = MOCK_CHARGE_TICKS;
                        this.repathCooldown = 0;
                    } else {
                        this.lion.provokeBy(this.player);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void startRetreat() {
        this.mockCharges++;
        this.action = Action.RETREAT;
        this.actionTicks = RETREAT_TICKS;
        this.lion.playWarningSound();
        Vec3 back = DefaultRandomPos.getPosAway(this.lion, 5, 3, this.player.position());
        if (back != null) {
            this.lion.getNavigation().moveTo(back.x, back.y, back.z, 1.0D);
        } else {
            this.lion.getNavigation().stop();
        }
    }

    private void fleeToAdult() {
        if (--this.repathCooldown > 0 && !this.lion.getNavigation().isDone()) {
            return;
        }
        this.repathCooldown = 20;
        AbstractLionEntity nearestAdult = null;
        for (AbstractLionEntity member : this.lion.getPride()) {
            if (member.isAlive() && !member.isBaby() && (nearestAdult == null || this.lion.distanceToSqr(member) < this.lion.distanceToSqr(nearestAdult))) {
                nearestAdult = member;
            }
        }
        if (nearestAdult != null) {
            this.lion.getNavigation().moveTo(nearestAdult, 1.5D);
        } else {
            Vec3 away = DefaultRandomPos.getPosAway(this.lion, 12, 7, this.player.position());
            if (away != null) {
                this.lion.getNavigation().moveTo(away.x, away.y, away.z, 1.5D);
            }
        }
    }

    private double scale(Player player) {
        double scale = 1.0D - 0.5D * this.lion.getTolerance(player) / MAX_TOLERANCE;
        if (player.isDiscrete()) {
            scale *= 0.6D;
        }
        if (this.lion.level().isNight()) {
            scale *= 1.25D;
        }
        if (this.lion.countAdultsNearby(GROUP_RANGE) >= 2) {
            scale *= 1.2D;
        }
        return scale;
    }

    private boolean protectingCubs() {
        return !this.lion.isBaby() && this.lion.hasCubNearby(CUB_RANGE);
    }

    private double mockChargeDistance(Player player) {
        return this.lion.getThreatProfile().mockChargeDistance() * this.scale(player) + (this.protectingCubs() ? CUB_MOCK_BONUS : 0.0D);
    }

    private int allowedMockCharges() {
        int allowed = this.lion.getThreatProfile().mockChargesBeforeAttack();
        return this.protectingCubs() ? Math.min(1, allowed) : allowed;
    }

    private Stage computeStage(Player player) {
        if (this.lion.isFood(player.getMainHandItem()) || this.lion.isFood(player.getOffhandItem())) {
            return Stage.NONE;
        }
        AbstractLionEntity.ThreatProfile profile = this.lion.getThreatProfile();
        double scale = this.scale(player);
        double distance = this.lion.distanceTo(player);

        if (this.lion.isBaby()) {
            return distance < profile.warnDistance() * scale ? Stage.FLEE : Stage.NONE;
        }
        double warn = profile.warnDistance() * scale + (this.protectingCubs() ? CUB_WARN_BONUS : 0.0D);
        if (distance < warn) {
            return Stage.WARN;
        }
        if (distance < profile.watchDistance() * scale) {
            return Stage.WATCH;
        }
        return Stage.NONE;
    }
}
