package net.untamed.entity.ai.lion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractLionEntity;
import net.untamed.entity.HerdBovineEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class LionHuntGoal extends Goal {

    private static final double SEARCH_RANGE = 24.0D;
    private static final double STALK_SPEED = 0.6D;
    private static final double CHARGE_SPEED = 2.0D;
    private static final double CHARGE_START_DISTANCE = 7.0D;
    private static final double GIVE_UP_DISTANCE = 12.0D;
    private static final int MAX_STALK_TICKS = 400;
    private static final int MAX_HUNT_TICKS = 1600;
    private static final int GIVE_UP_AFTER_CHARGE_TICKS = 40;
    private static final int FAILED_HUNT_COOLDOWN = 1200;
    private static final int NO_PREY_COOLDOWN = 200;

    private final AbstractLionEntity lion;
    @Nullable
    private LionHunt hunt;
    private int ticks;
    private int chargeTicks;
    private int attackCooldown;
    private int repathTicks;
    private boolean gaveUp;

    public LionHuntGoal(AbstractLionEntity lion) {
        this.lion = lion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.lion.isBaby() || this.lion.isMale() || this.lion.getTarget() != null) {
            return false;
        }
        LionHunt joined = this.lion.getHunt();
        if (joined != null && !joined.isFinished()) {
            this.hunt = joined;
            return true;
        }
        if (!this.lion.isHungry() || this.lion.isRestTime() || this.lion.isAlarmed() || !this.lion.canStartHunt()
                || this.lion.getRandom().nextInt(reducedTickDelay(100)) != 0) {
            return false;
        }
        return this.startNewHunt();
    }

    private boolean startNewHunt() {
        List<AbstractLionEntity> hunters = new ArrayList<>();
        hunters.add(this.lion);
        for (AbstractLionEntity member : this.lion.getPride()) {
            if (member.isAlive() && !member.isBaby() && !member.isMale() && member.getTarget() == null && member.getHunt() == null
                    && this.lion.distanceToSqr(member) < SEARCH_RANGE * SEARCH_RANGE) {
                hunters.add(member);
            }
        }
        int hunterCount = hunters.size();
        LivingEntity prey = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Animal candidate : this.lion.level().getEntitiesOfClass(Animal.class, this.lion.getBoundingBox().inflate(SEARCH_RANGE, 8.0D, SEARCH_RANGE),
                animal -> this.lion.isValidPrey(animal, hunterCount))) {
            double score = this.score(candidate);
            if (score > bestScore) {
                bestScore = score;
                prey = candidate;
            }
        }
        if (prey == null) {
            this.lion.setHuntCooldown(NO_PREY_COOLDOWN);
            return false;
        }
        this.hunt = new LionHunt(this.lion, prey, hunters);
        for (AbstractLionEntity hunter : hunters) {
            hunter.setHunt(this.hunt);
        }
        return true;
    }

    private double score(Animal prey) {
        double score = 0.0D;
        if (prey.isBaby()) {
            score += 4.0D;
        }
        score += 3.0D * (1.0D - prey.getHealth() / prey.getMaxHealth());
        int companions = prey.level().getEntities(prey, prey.getBoundingBox().inflate(8.0D), other -> other.getType() == prey.getType()).size();
        score += Math.max(0, 3 - companions);
        score -= this.lion.distanceTo(prey) / 8.0D;
        return score;
    }

    @Override
    public boolean canContinueToUse() {
        return this.hunt != null && !this.hunt.isFinished() && !this.gaveUp && this.lion.getHunt() == this.hunt && this.lion.getTarget() == null && this.ticks < MAX_HUNT_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.chargeTicks = 0;
        this.attackCooldown = 0;
        this.repathTicks = 0;
        this.gaveUp = false;
        this.lion.clearLionPose(AbstractLionEntity.LionPose.SLEEPING);
        this.lion.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.hunt == null) {
            return;
        }
        this.ticks++;
        LivingEntity prey = this.hunt.getPrey();
        this.lion.getLookControl().setLookAt(prey, 30.0F, 30.0F);

        if (!this.hunt.isCharging()) {
            this.lion.setLionPose(AbstractLionEntity.LionPose.STALKING);
            if (--this.repathTicks <= 0) {
                this.repathTicks = 10;
                Vec3 spot = this.hunt.getStalkTarget(this.lion);
                this.lion.getNavigation().moveTo(spot.x, spot.y, spot.z, STALK_SPEED);
            }
            if (this.lion.distanceTo(prey) < CHARGE_START_DISTANCE || this.ticks > MAX_STALK_TICKS || this.isPreyAlerted(prey)) {
                this.hunt.startCharge();
            }
            return;
        }

        this.lion.clearLionPose(AbstractLionEntity.LionPose.STALKING);
        this.chargeTicks++;
        this.lion.drainStamina();
        if (--this.repathTicks <= 0) {
            this.repathTicks = 5;
            this.lion.getNavigation().moveTo(prey, CHARGE_SPEED);
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        } else if (this.lion.isWithinMeleeAttackRange(prey) && this.lion.getSensing().hasLineOfSight(prey)) {
            this.lion.doHurtTarget(prey);
            this.attackCooldown = 20;
        }
        if (this.lion.getStamina() <= 0 || (this.chargeTicks > GIVE_UP_AFTER_CHARGE_TICKS && this.lion.distanceTo(prey) > GIVE_UP_DISTANCE)) {
            this.gaveUp = true;
        }
    }

    private boolean isPreyAlerted(LivingEntity prey) {
        if (prey.getLastHurtByMob() != null && prey.tickCount - prey.getLastHurtByMobTimestamp() < 40) {
            return true;
        }
        return prey instanceof HerdBovineEntity bovine && bovine.isAlarmed();
    }

    @Override
    public void stop() {
        this.lion.clearLionPose(AbstractLionEntity.LionPose.STALKING);
        this.lion.getNavigation().stop();
        if (this.hunt != null && !this.hunt.isFinished()) {
            boolean failed = this.gaveUp || this.ticks >= MAX_HUNT_TICKS;
            if (this.lion == this.hunt.getLeader()) {
                this.hunt.finish(failed ? FAILED_HUNT_COOLDOWN : 0);
            } else if (this.lion.getHunt() == this.hunt) {
                this.lion.setHunt(null);
                if (failed) {
                    this.lion.setHuntCooldown(FAILED_HUNT_COOLDOWN);
                }
            }
        }
        this.hunt = null;
        this.gaveUp = false;
    }
}
