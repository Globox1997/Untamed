package net.untamed.entity.ai.raccoon;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.RaccoonEntity;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class RaccoonThreatGoal extends Goal {

    private static final double PLAYER_DETECT = 8.0D;
    private static final double RESTING_PLAYER_DETECT = 4.0D;
    private static final double PREDATOR_DETECT = 10.0D;
    private static final double SNEAK_FACTOR = 0.5D;
    private static final double RELEASE_DISTANCE = 16.0D;
    private static final double DEN_RANGE = 24.0D;
    private static final double AT_DEN = 1.5D;
    private static final double CORNER_DISTANCE = 2.0D;
    private static final double KIT_GUARD_RANGE = 6.0D;
    private static final double KIT_GUARD_DISTANCE = 5.0D;
    private static final double FLEE_SPEED = 1.6D;
    private static final int RECENT_HURT_TICKS = 100;
    private static final int BITE_COOLDOWN = 20;
    private static final int HISS_INTERVAL = 50;
    private static final int ALARM_TICKS = 40;

    private enum Mode {
        FLEE_TO_DEN, FLEE_TO_MOTHER, FLEE_AWAY, HIDE, DEFEND
    }

    private final RaccoonEntity raccoon;
    @Nullable
    private LivingEntity threat;
    private Mode mode = Mode.FLEE_AWAY;
    private int biteCooldown;
    private int hissCooldown;
    private int repathTicks;

    public RaccoonThreatGoal(RaccoonEntity raccoon) {
        this.raccoon = raccoon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        this.threat = this.findThreat();
        return this.threat != null;
    }

    @Nullable
    private LivingEntity findThreat() {
        LivingEntity attacker = this.raccoon.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && this.isRecentAttacker(attacker)) {
            return attacker;
        }
        LivingEntity nearest = null;
        for (LivingEntity predator : this.raccoon.level().getEntitiesOfClass(LivingEntity.class, this.raccoon.getBoundingBox().inflate(PREDATOR_DETECT),
                entity -> entity.isAlive() && entity.getType().is(TagInit.RACCOON_PREDATORS))) {
            if (nearest == null || this.raccoon.distanceToSqr(predator) < this.raccoon.distanceToSqr(nearest)) {
                nearest = predator;
            }
        }
        double detect = this.raccoon.isResting() ? RESTING_PLAYER_DETECT : PLAYER_DETECT;
        Player player = this.raccoon.level().getNearestPlayer(this.raccoon.getX(), this.raccoon.getY(), this.raccoon.getZ(), detect,
                entity -> entity instanceof Player p && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p) && p.isAlive() && !this.isOfferingFood(p)
                        && this.raccoon.distanceTo(p) < detect * (p.isDiscrete() ? SNEAK_FACTOR : 1.0D));
        if (player != null && (nearest == null || this.raccoon.distanceToSqr(player) < this.raccoon.distanceToSqr(nearest))) {
            nearest = player;
        }
        return nearest;
    }

    private boolean isOfferingFood(Player player) {
        return this.raccoon.isFood(player.getMainHandItem()) || this.raccoon.isFood(player.getOffhandItem());
    }

    private boolean isValidThreat(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        return this.isRecentAttacker(entity) || !(entity instanceof Player player) || EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player);
    }

    private boolean isRecentAttacker(LivingEntity entity) {
        return entity == this.raccoon.getLastHurtByMob() && this.raccoon.tickCount - this.raccoon.getLastHurtByMobTimestamp() < RECENT_HURT_TICKS;
    }

    @Override
    public boolean canContinueToUse() {
        return this.threat != null && this.isValidThreat(this.threat) && this.raccoon.distanceTo(this.threat) < RELEASE_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.RESTING);
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.SITTING_UP);
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.DOUSING);
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.FORAGING);
        this.biteCooldown = 0;
        this.hissCooldown = 0;
        this.chooseMode();
    }

    @Override
    public void stop() {
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.HISSING);
        this.raccoon.getNavigation().stop();
        this.threat = null;
    }

    private boolean isGuardingKits() {
        return !this.raccoon.isBaby() && this.raccoon.hasOwnKitNearby(KIT_GUARD_RANGE) && this.threat != null
                && this.raccoon.distanceTo(this.threat) < KIT_GUARD_DISTANCE;
    }

    private void chooseMode() {
        if (this.threat == null) {
            return;
        }
        this.repathTicks = 20;
        if (this.raccoon.distanceTo(this.threat) < CORNER_DISTANCE || this.isGuardingKits()) {
            this.mode = Mode.DEFEND;
            return;
        }
        RaccoonEntity mother = this.raccoon.getMother();
        if (this.raccoon.isBaby() && mother != null && this.raccoon.getNavigation().moveTo(mother, FLEE_SPEED)) {
            this.mode = Mode.FLEE_TO_MOTHER;
            return;
        }
        BlockPos den = this.raccoon.getDenPos();
        if (den != null && this.raccoon.blockPosition().closerThan(den, DEN_RANGE) && this.threat.distanceToSqr(Vec3.atBottomCenterOf(den)) > 16.0D) {
            if (this.raccoon.position().distanceToSqr(Vec3.atBottomCenterOf(den)) < AT_DEN * AT_DEN) {
                this.mode = Mode.HIDE;
                return;
            }
            if (this.raccoon.getNavigation().moveTo(den.getX() + 0.5D, den.getY(), den.getZ() + 0.5D, FLEE_SPEED)) {
                this.mode = Mode.FLEE_TO_DEN;
                return;
            }
        }
        Vec3 away = DefaultRandomPos.getPosAway(this.raccoon, 12, 6, this.threat.position());
        if (away != null && this.raccoon.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED)) {
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
        this.raccoon.alarm(ALARM_TICKS);
        double distance = this.raccoon.distanceTo(this.threat);
        if (this.biteCooldown > 0) {
            this.biteCooldown--;
        }
        if (this.hissCooldown > 0) {
            this.hissCooldown--;
        }
        if (this.mode != Mode.DEFEND && (this.isGuardingKits() || (distance < CORNER_DISTANCE && (this.mode == Mode.HIDE || this.raccoon.getNavigation().isDone())))) {
            this.mode = Mode.DEFEND;
        }

        switch (this.mode) {
            case FLEE_TO_DEN, FLEE_TO_MOTHER, FLEE_AWAY -> {
                if (--this.repathTicks <= 0 && this.raccoon.getNavigation().isDone()) {
                    this.chooseMode();
                }
            }
            case HIDE -> {
                this.raccoon.getNavigation().stop();
                this.raccoon.getLookControl().setLookAt(this.threat, 30.0F, 30.0F);
            }
            case DEFEND -> {
                this.raccoon.getNavigation().stop();
                this.raccoon.getLookControl().setLookAt(this.threat, 30.0F, 30.0F);
                this.raccoon.setRaccoonPose(RaccoonEntity.RaccoonPose.HISSING);
                if (this.hissCooldown <= 0) {
                    this.hissCooldown = HISS_INTERVAL;
                    this.raccoon.playSound(SoundEvents.CAT_HISS, 0.7F, 1.0F + this.raccoon.getRandom().nextFloat() * 0.2F);
                }
                if (!this.raccoon.isBaby() && this.biteCooldown <= 0 && this.raccoon.isWithinMeleeAttackRange(this.threat)) {
                    this.raccoon.swing(InteractionHand.MAIN_HAND);
                    this.raccoon.doHurtTarget(this.threat);
                    this.biteCooldown = BITE_COOLDOWN;
                }
                if (distance > CORNER_DISTANCE * 2.0D && !this.isGuardingKits()) {
                    this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.HISSING);
                    this.chooseMode();
                }
            }
        }
    }
}
