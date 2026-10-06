package net.untamed.entity.ai.kiwi;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.KiwiEntity;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class KiwiThreatGoal extends Goal {

    private static final double AWAKE_DETECT = 6.0D;
    private static final double ASLEEP_DETECT = 3.0D;
    private static final double PREDATOR_DETECT = 8.0D;
    private static final double SNEAK_FACTOR = 0.5D;
    private static final double RELEASE_FACTOR = 2.0D;
    private static final double BURROW_RANGE = 24.0D;
    private static final double AT_BURROW = 1.5D;
    private static final double CORNER_DISTANCE = 2.0D;
    private static final double FLEE_SPEED = 1.8D;
    private static final int RECENT_HURT_TICKS = 100;
    private static final int KICK_COOLDOWN = 20;
    private static final int HISS_INTERVAL = 60;

    private enum Mode {
        FLEE_TO_BURROW, FLEE_AWAY, HIDE, DEFEND
    }

    private final KiwiEntity kiwi;
    @Nullable
    private LivingEntity threat;
    private Mode mode = Mode.HIDE;
    private int kickCooldown;
    private int hissCooldown;
    private int repathTicks;

    public KiwiThreatGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        this.threat = this.findThreat();
        return this.threat != null;
    }

    @Nullable
    private LivingEntity findThreat() {
        LivingEntity attacker = this.kiwi.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && this.isRecentAttacker(attacker)) {
            return attacker;
        }
        LivingEntity nearest = null;
        for (LivingEntity predator : this.kiwi.level().getEntitiesOfClass(LivingEntity.class, this.kiwi.getBoundingBox().inflate(PREDATOR_DETECT),
                entity -> entity.isAlive() && entity.getType().is(TagInit.KIWI_PREDATORS))) {
            if (nearest == null || this.kiwi.distanceToSqr(predator) < this.kiwi.distanceToSqr(nearest)) {
                nearest = predator;
            }
        }
        double detect = this.kiwi.isSleeping() ? ASLEEP_DETECT : AWAKE_DETECT;
        Player player = this.kiwi.level().getNearestPlayer(this.kiwi.getX(), this.kiwi.getY(), this.kiwi.getZ(), detect,
                entity -> entity instanceof Player p && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p) && p.isAlive() && !this.isOfferingFood(p)
                        && this.kiwi.distanceTo(p) < detect * (p.isDiscrete() ? SNEAK_FACTOR : 1.0D));
        if (player != null && (nearest == null || this.kiwi.distanceToSqr(player) < this.kiwi.distanceToSqr(nearest))) {
            nearest = player;
        }
        return nearest;
    }

    private boolean isOfferingFood(Player player) {
        return this.kiwi.isFood(player.getMainHandItem()) || this.kiwi.isFood(player.getOffhandItem());
    }

    private boolean isValidThreat(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        return this.isRecentAttacker(entity) || !(entity instanceof Player player) || EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player);
    }

    private boolean isRecentAttacker(LivingEntity entity) {
        return entity == this.kiwi.getLastHurtByMob() && this.kiwi.tickCount - this.kiwi.getLastHurtByMobTimestamp() < RECENT_HURT_TICKS;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.threat == null || !this.isValidThreat(this.threat)) {
            return false;
        }
        return this.kiwi.distanceTo(this.threat) < Math.max(AWAKE_DETECT, PREDATOR_DETECT) * RELEASE_FACTOR;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.kiwi.clearKiwiPose(KiwiEntity.KiwiPose.SLEEPING);
        this.kiwi.clearKiwiPose(KiwiEntity.KiwiPose.PROBING);
        this.kickCooldown = 0;
        this.hissCooldown = 0;
        this.chooseMode();
    }

    @Override
    public void stop() {
        this.kiwi.clearKiwiPose(KiwiEntity.KiwiPose.DEFENSIVE);
        this.kiwi.getNavigation().stop();
        this.threat = null;
    }

    private void chooseMode() {
        if (this.threat == null) {
            return;
        }
        this.repathTicks = 20;
        if (this.kiwi.distanceTo(this.threat) < CORNER_DISTANCE) {
            this.mode = Mode.DEFEND;
            return;
        }
        BlockPos burrow = this.kiwi.getBurrowPos();
        if (burrow != null && this.kiwi.blockPosition().closerThan(burrow, BURROW_RANGE) && this.threat.distanceToSqr(Vec3.atBottomCenterOf(burrow)) > 9.0D) {
            if (this.kiwi.position().distanceToSqr(Vec3.atBottomCenterOf(burrow)) < AT_BURROW * AT_BURROW) {
                this.mode = Mode.HIDE;
                return;
            }
            if (this.kiwi.getNavigation().moveTo(burrow.getX() + 0.5D, burrow.getY(), burrow.getZ() + 0.5D, FLEE_SPEED)) {
                this.mode = Mode.FLEE_TO_BURROW;
                return;
            }
        }
        Vec3 away = DefaultRandomPos.getPosAway(this.kiwi, 12, 6, this.threat.position());
        if (away != null && this.kiwi.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED)) {
            this.mode = Mode.FLEE_AWAY;
        } else {
            this.mode = Mode.DEFEND;
        }
    }

    @Override
    public void tick() {
        if (this.threat == null) {
            return;
        }
        this.kiwi.alarm(40);
        double distance = this.kiwi.distanceTo(this.threat);
        if (this.kickCooldown > 0) {
            this.kickCooldown--;
        }
        if (this.hissCooldown > 0) {
            this.hissCooldown--;
        }
        if (this.mode != Mode.DEFEND && distance < CORNER_DISTANCE && (this.mode == Mode.HIDE || this.kiwi.getNavigation().isDone())) {
            this.mode = Mode.DEFEND;
        }

        switch (this.mode) {
            case FLEE_TO_BURROW -> {
                if (this.kiwi.getNavigation().isDone()) {
                    this.chooseMode();
                }
            }
            case FLEE_AWAY -> {
                if (--this.repathTicks <= 0 && this.kiwi.getNavigation().isDone()) {
                    this.chooseMode();
                }
            }
            case HIDE -> {
                this.kiwi.getNavigation().stop();
                this.kiwi.getLookControl().setLookAt(this.threat, 30.0F, 30.0F);
            }
            case DEFEND -> {
                this.kiwi.getNavigation().stop();
                this.kiwi.getLookControl().setLookAt(this.threat, 30.0F, 30.0F);
                this.kiwi.setKiwiPose(KiwiEntity.KiwiPose.DEFENSIVE);
                if (this.hissCooldown <= 0) {
                    this.hissCooldown = HISS_INTERVAL;
                    this.kiwi.playSound(SoundEvents.CAT_HISS, 0.6F, 1.6F);
                }
                if (!this.kiwi.isBaby() && this.kickCooldown <= 0 && this.kiwi.isWithinMeleeAttackRange(this.threat)) {
                    this.kiwi.doHurtTarget(this.threat);
                    this.kickCooldown = KICK_COOLDOWN;
                }
                if (distance > CORNER_DISTANCE * 2.0D) {
                    this.kiwi.clearKiwiPose(KiwiEntity.KiwiPose.DEFENSIVE);
                    this.chooseMode();
                }
            }
        }
    }
}
