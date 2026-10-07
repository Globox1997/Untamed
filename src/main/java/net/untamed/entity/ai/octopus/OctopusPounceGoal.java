package net.untamed.entity.ai.octopus;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.OctopusEntity;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class OctopusPounceGoal extends Goal {

    private static final double SEARCH_RANGE = 12.0D;
    private static final double LUNGE_DISTANCE = 2.5D;
    private static final double BITE_DISTANCE = 1.8D;
    private static final double GIVE_UP_DISTANCE = 16.0D;
    private static final double STALK_SPEED = 0.6D;
    private static final double LUNGE_SPEED = 0.7D;
    private static final int MAX_TICKS = 400;
    private static final int LUNGE_COOLDOWN = 30;

    private final OctopusEntity octopus;
    @Nullable
    private LivingEntity prey;
    private int ticks;
    private int lungeCooldown;
    private int repathTicks;

    public OctopusPounceGoal(OctopusEntity octopus) {
        this.octopus = octopus;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.octopus.isBaby() || this.octopus.isRestTime() || !this.octopus.isHungry() || !this.octopus.isInWater() || this.octopus.isThreatened()
                || this.octopus.getRandom().nextInt(reducedTickDelay(60)) != 0) {
            return false;
        }
        this.prey = null;
        for (LivingEntity candidate : this.octopus.level().getEntitiesOfClass(LivingEntity.class, this.octopus.getBoundingBox().inflate(SEARCH_RANGE),
                entity -> entity.isAlive() && entity.isInWater() && entity.getType().is(TagInit.OCTOPUS_PREY))) {
            if (this.prey == null || this.octopus.distanceToSqr(candidate) < this.octopus.distanceToSqr(this.prey)) {
                this.prey = candidate;
            }
        }
        return this.prey != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.prey != null && this.prey.isAlive() && this.ticks < MAX_TICKS && !this.octopus.isThreatened() && this.octopus.isHungry()
                && this.octopus.distanceToSqr(this.prey) < GIVE_UP_DISTANCE * GIVE_UP_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.lungeCooldown = 0;
        this.repathTicks = 0;
        this.octopus.setOctopusPose(OctopusEntity.OctopusPose.STALKING);
    }

    @Override
    public void tick() {
        if (this.prey == null) {
            return;
        }
        this.ticks++;
        this.octopus.getLookControl().setLookAt(this.prey, 30.0F, 30.0F);
        if (this.lungeCooldown > 0) {
            this.lungeCooldown--;
        }
        double distance = this.octopus.distanceTo(this.prey);
        OctopusEntity.OctopusPose pose = this.octopus.getOctopusPose();
        if (pose == OctopusEntity.OctopusPose.NONE) {
            this.octopus.setOctopusPose(OctopusEntity.OctopusPose.STALKING);
        }
        if (distance > LUNGE_DISTANCE) {
            if (--this.repathTicks <= 0) {
                this.repathTicks = 10;
                this.octopus.getNavigation().moveTo(this.prey, STALK_SPEED);
            }
            return;
        }
        if (this.lungeCooldown <= 0) {
            this.lungeCooldown = LUNGE_COOLDOWN;
            Vec3 direction = this.prey.position().subtract(this.octopus.position());
            if (direction.lengthSqr() > 1.0E-4D) {
                direction = direction.normalize();
                this.octopus.setDeltaMovement(direction.scale(LUNGE_SPEED));
                this.octopus.hasImpulse = true;
            }
            this.octopus.startPounce();
            if (distance < BITE_DISTANCE || this.octopus.isWithinMeleeAttackRange(this.prey)) {
                this.octopus.doHurtTarget(this.prey);
            }
        }
    }

    @Override
    public void stop() {
        this.octopus.clearOctopusPose(OctopusEntity.OctopusPose.STALKING);
        this.octopus.getNavigation().stop();
        this.prey = null;
    }
}
