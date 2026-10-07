package net.untamed.entity.ai.panther;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.BlackPantherEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PantherStalkGoal extends Goal {

    private static final double SEARCH_RANGE = 16.0D;
    private static final double STALK_SPEED = 0.5D;
    private static final double CHASE_SPEED = 1.8D;
    private static final double POUNCE_DISTANCE = 4.0D;
    private static final double FREEZE_RANGE = 10.0D;
    private static final double PREY_LOOKING_DOT = 0.8D;
    private static final double GIVE_UP_DISTANCE = 10.0D;
    private static final double POUNCE_HORIZONTAL = 0.8D;
    private static final double POUNCE_VERTICAL = 0.45D;
    private static final int MAX_CHASE_TICKS = 40;
    private static final int MAX_TICKS = 600;
    private static final int FAILED_HUNT_COOLDOWN = 1200;
    private static final int NO_PREY_COOLDOWN = 200;

    private final BlackPantherEntity panther;
    @Nullable
    private LivingEntity prey;
    private boolean chasing;
    private boolean failed;
    private int ticks;
    private int chaseTicks;
    private int attackCooldown;
    private int repathTicks;

    public PantherStalkGoal(BlackPantherEntity panther) {
        this.panther = panther;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.panther.isBaby() || !this.panther.isHungry() || this.panther.isRestTime() || this.panther.isAlarmed() || this.panther.getTarget() != null
                || !this.panther.canStartHunt() || this.panther.getRandom().nextInt(reducedTickDelay(80)) != 0) {
            return false;
        }
        this.prey = null;
        for (Animal candidate : this.panther.level().getEntitiesOfClass(Animal.class, this.panther.getBoundingBox().inflate(SEARCH_RANGE, 6.0D, SEARCH_RANGE),
                this.panther::isValidPrey)) {
            if (this.prey == null || this.panther.distanceToSqr(candidate) < this.panther.distanceToSqr(this.prey)) {
                this.prey = candidate;
            }
        }
        if (this.prey == null) {
            this.panther.setHuntCooldown(NO_PREY_COOLDOWN);
        }
        return this.prey != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.prey != null && this.prey.isAlive() && !this.failed && this.ticks < MAX_TICKS && this.panther.getTarget() == null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.chasing = false;
        this.failed = false;
        this.ticks = 0;
        this.chaseTicks = 0;
        this.attackCooldown = 0;
        this.repathTicks = 0;
        this.panther.setHunting(true);
        this.panther.setPantherPose(BlackPantherEntity.PantherPose.STALKING);
    }

    @Override
    public void tick() {
        if (this.prey == null) {
            return;
        }
        this.ticks++;
        this.panther.getLookControl().setLookAt(this.prey, 30.0F, 30.0F);
        double distance = this.panther.distanceTo(this.prey);

        if (!this.chasing) {
            if (this.isPreyLookingAtMe() && distance < FREEZE_RANGE) {
                this.panther.getNavigation().stop();
            } else if (--this.repathTicks <= 0 || this.panther.getNavigation().isDone()) {
                this.repathTicks = 10;
                this.panther.getNavigation().moveTo(this.prey, STALK_SPEED);
            }
            if (distance < POUNCE_DISTANCE && this.panther.getSensing().hasLineOfSight(this.prey)) {
                this.pounce();
            }
            return;
        }

        this.chaseTicks++;
        if (--this.repathTicks <= 0) {
            this.repathTicks = 5;
            this.panther.getNavigation().moveTo(this.prey, CHASE_SPEED);
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        } else if (this.panther.isWithinMeleeAttackRange(this.prey)) {
            this.panther.doHurtTarget(this.prey);
            this.attackCooldown = 15;
        }
        if (distance > GIVE_UP_DISTANCE || (this.chaseTicks > MAX_CHASE_TICKS && distance > POUNCE_DISTANCE)) {
            this.failed = true;
        }
    }

    private boolean isPreyLookingAtMe() {
        Vec3 toPanther = this.panther.position().subtract(this.prey.position());
        if (toPanther.lengthSqr() < 1.0E-4D) {
            return false;
        }
        return this.prey.getViewVector(1.0F).dot(toPanther.normalize()) > PREY_LOOKING_DOT;
    }

    private void pounce() {
        Vec3 direction = new Vec3(this.prey.getX() - this.panther.getX(), 0.0D, this.prey.getZ() - this.panther.getZ());
        if (direction.lengthSqr() > 1.0E-4D) {
            direction = direction.normalize();
            this.panther.setDeltaMovement(direction.x * POUNCE_HORIZONTAL, POUNCE_VERTICAL, direction.z * POUNCE_HORIZONTAL);
            this.panther.hasImpulse = true;
        }
        this.panther.startPounce();
        this.chasing = true;
        this.repathTicks = 0;
    }

    @Override
    public void stop() {
        this.panther.clearPantherPose(BlackPantherEntity.PantherPose.STALKING);
        this.panther.setHunting(false);
        this.panther.getNavigation().stop();
        if (this.failed || this.ticks >= MAX_TICKS) {
            this.panther.setHuntCooldown(FAILED_HUNT_COOLDOWN);
        }
        this.prey = null;
    }
}
