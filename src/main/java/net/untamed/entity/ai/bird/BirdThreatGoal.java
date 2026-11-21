package net.untamed.entity.ai.bird;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractBirdEntity;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BirdThreatGoal extends Goal {

    private static final double PLAYER_DETECT = 8.0D;
    private static final double SLEEPING_PLAYER_DETECT = 4.0D;
    private static final double PREDATOR_DETECT = 10.0D;
    private static final double SNEAK_FACTOR = 0.5D;
    private static final double FLEE_DISTANCE = 4.0D;
    private static final double RELEASE_DISTANCE = 16.0D;
    private static final double FLEE_RANGE = 14.0D;
    private static final double FLEE_HEIGHT = 5.0D;
    private static final double FLEE_SPEED = 1.6D;
    private static final int RECENT_HURT_TICKS = 100;
    private static final int ALARM_TICKS = 100;

    private final AbstractBirdEntity bird;
    @Nullable
    private LivingEntity threat;
    private boolean fleeing;

    public BirdThreatGoal(AbstractBirdEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        this.threat = this.findThreat();
        return this.threat != null;
    }

    @Nullable
    private LivingEntity findThreat() {
        LivingEntity attacker = this.bird.getLastHurtByMob();
        if (attacker != null && attacker.isAlive() && this.isRecentAttacker(attacker)) {
            return attacker;
        }
        LivingEntity shared = this.bird.getSharedThreat();
        if (shared != null) {
            return shared;
        }
        LivingEntity nearest = null;
        for (LivingEntity predator : this.bird.level().getEntitiesOfClass(LivingEntity.class, this.bird.getBoundingBox().inflate(PREDATOR_DETECT),
                entity -> entity.isAlive() && entity.getType().is(TagInit.BIRD_PREDATORS))) {
            if (nearest == null || this.bird.distanceToSqr(predator) < this.bird.distanceToSqr(nearest)) {
                nearest = predator;
            }
        }
        double detect = this.bird.isAsleep() ? SLEEPING_PLAYER_DETECT : PLAYER_DETECT;
        Player player = this.bird.level().getNearestPlayer(this.bird.getX(), this.bird.getY(), this.bird.getZ(), detect,
                entity -> entity instanceof Player p && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p) && p.isAlive() && !this.isOfferingFood(p)
                        && this.bird.distanceTo(p) < detect * (p.isDiscrete() ? SNEAK_FACTOR : 1.0D));
        if (player != null && (nearest == null || this.bird.distanceToSqr(player) < this.bird.distanceToSqr(nearest))) {
            nearest = player;
        }
        return nearest;
    }

    private boolean isOfferingFood(Player player) {
        return this.bird.isFood(player.getMainHandItem()) || this.bird.isFood(player.getOffhandItem());
    }

    private boolean isRecentAttacker(LivingEntity entity) {
        return entity == this.bird.getLastHurtByMob() && this.bird.tickCount - this.bird.getLastHurtByMobTimestamp() < RECENT_HURT_TICKS;
    }

    private boolean isValidThreat(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        return this.isRecentAttacker(entity) || entity == this.bird.getSharedThreat() || !(entity instanceof Player player) || EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player);
    }

    @Override
    public boolean canContinueToUse() {
        return this.threat != null && this.isValidThreat(this.threat) && this.bird.distanceTo(this.threat) < RELEASE_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.fleeing = false;
        this.bird.clearBirdPose(AbstractBirdEntity.BirdPose.SLEEPING);
        this.bird.clearBirdPose(AbstractBirdEntity.BirdPose.PERCHED);
        if (!this.shouldFreeze()) {
            this.startFleeing();
        }
    }

    private boolean shouldFreeze() {
        return this.threat != null && this.bird.freezesWhenThreatened() && this.bird.onGround() && !this.isRecentAttacker(this.threat)
                && this.bird.distanceTo(this.threat) > FLEE_DISTANCE;
    }

    private void startFleeing() {
        this.fleeing = true;
        this.bird.clearBirdPose(AbstractBirdEntity.BirdPose.FROZEN);
        this.bird.alarm(ALARM_TICKS);
        if (this.threat != null) {
            this.bird.onThreatAlarm(this.threat);
        }
        this.fleeFromThreat();
    }

    private void fleeFromThreat() {
        if (this.threat == null) {
            return;
        }
        Vec3 away = this.bird.position().subtract(this.threat.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 1.0E-4D) {
            float angle = this.bird.getRandom().nextFloat() * Mth.TWO_PI;
            away = new Vec3(Mth.cos(angle), 0.0D, Mth.sin(angle));
        }
        Vec3 target = this.bird.position().add(away.normalize().scale(FLEE_RANGE));
        int ground = this.bird.level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(target.x), Mth.floor(target.z));
        this.bird.getNavigation().moveTo(target.x, Math.max(ground + 1.0D, this.bird.getY()) + FLEE_HEIGHT, target.z, FLEE_SPEED);
    }

    @Override
    public void stop() {
        this.bird.clearBirdPose(AbstractBirdEntity.BirdPose.FROZEN);
        this.threat = null;
    }

    @Override
    public void tick() {
        if (this.threat == null) {
            return;
        }
        if (!this.fleeing) {
            this.bird.getNavigation().stop();
            this.bird.setBirdPose(AbstractBirdEntity.BirdPose.FROZEN);
            this.bird.getLookControl().setLookAt(this.threat, 30.0F, 30.0F);
            if (!this.shouldFreeze()) {
                this.startFleeing();
            }
            return;
        }
        this.bird.alarm(ALARM_TICKS);
        if (this.bird.getNavigation().isDone()) {
            this.fleeFromThreat();
        }
    }
}
