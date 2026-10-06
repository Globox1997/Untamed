package net.untamed.entity.ai.hyena;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HyenaEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

// Hyenas do not stalk: they run at a herd to test it, the leader picks out the weakest animal and the clan chases it down
public class HyenaHuntGoal extends Goal {

    private static final double SEARCH_RANGE = 24.0D;
    private static final double MARK_RANGE = 10.0D;
    private static final double TEST_SPEED = 1.4D;
    private static final double CHASE_SPEED = 1.5D;
    private static final double TEST_CLOSE_DISTANCE = 6.0D;
    private static final double GIVE_UP_DISTANCE = 24.0D;
    private static final double EXCITED_DISTANCE = 4.0D;
    private static final int MAX_TEST_TICKS = 60;
    private static final int MAX_HUNT_TICKS = 2400;
    private static final int FAILED_HUNT_COOLDOWN = 1200;
    private static final int NO_PREY_COOLDOWN = 200;

    private final HyenaEntity hyena;
    @Nullable
    private HyenaHunt hunt;
    private int ticks;
    private int attackCooldown;
    private int repathTicks;
    private boolean gaveUp;

    public HyenaHuntGoal(HyenaEntity hyena) {
        this.hyena = hyena;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.hyena.isBaby() || this.hyena.getTarget() != null) {
            return false;
        }
        HyenaHunt joined = this.hyena.getHunt();
        if (joined != null && !joined.isFinished()) {
            this.hunt = joined;
            return true;
        }
        if (!this.hyena.isHungry() || this.hyena.isRestTime() || this.hyena.isAlarmed() || !this.hyena.canStartHunt()
                || this.hyena.getRandom().nextInt(reducedTickDelay(100)) != 0) {
            return false;
        }
        return this.startNewHunt();
    }

    private boolean startNewHunt() {
        List<HyenaEntity> hunters = new ArrayList<>();
        hunters.add(this.hyena);
        for (HyenaEntity member : this.hyena.getClan()) {
            if (member.isAlive() && !member.isBaby() && member.getTarget() == null && member.getHunt() == null
                    && this.hyena.distanceToSqr(member) < SEARCH_RANGE * SEARCH_RANGE) {
                hunters.add(member);
            }
        }
        int hunterCount = hunters.size();
        LivingEntity prey = null;
        for (Animal candidate : this.hyena.level().getEntitiesOfClass(Animal.class, this.hyena.getBoundingBox().inflate(SEARCH_RANGE, 8.0D, SEARCH_RANGE),
                animal -> this.hyena.isValidPrey(animal, hunterCount))) {
            if (prey == null || this.hyena.distanceToSqr(candidate) < this.hyena.distanceToSqr(prey)) {
                prey = candidate;
            }
        }
        if (prey == null) {
            this.hyena.setHuntCooldown(NO_PREY_COOLDOWN);
            return false;
        }
        this.hunt = new HyenaHunt(this.hyena, prey, hunters);
        for (HyenaEntity hunter : hunters) {
            hunter.setHunt(this.hunt);
        }
        this.hyena.requestWhoop(0, true);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.hunt != null && !this.hunt.isFinished() && !this.gaveUp && this.hyena.getHunt() == this.hunt && this.hyena.getTarget() == null
                && this.ticks < MAX_HUNT_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.attackCooldown = 0;
        this.repathTicks = 0;
        this.gaveUp = false;
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.RESTING);
    }

    @Override
    public void tick() {
        if (this.hunt == null) {
            return;
        }
        this.ticks++;
        LivingEntity prey = this.hunt.getPrey();
        this.hyena.getLookControl().setLookAt(prey, 30.0F, 30.0F);

        if (!this.hunt.isChasing()) {
            if (--this.repathTicks <= 0) {
                this.repathTicks = 10;
                this.hyena.getNavigation().moveTo(prey, TEST_SPEED);
            }
            if (this.hyena == this.hunt.getLeader() && (this.ticks > MAX_TEST_TICKS || this.hyena.distanceTo(prey) < TEST_CLOSE_DISTANCE)) {
                this.hunt.startChase(this.markWeakest(prey));
            }
            return;
        }

        this.hyena.drainStamina();
        double distance = this.hyena.distanceTo(prey);
        if (distance < EXCITED_DISTANCE) {
            this.hyena.setHyenaPose(HyenaEntity.HyenaPose.EXCITED);
        } else {
            this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.EXCITED);
        }
        if (--this.repathTicks <= 0) {
            this.repathTicks = 10;
            this.hyena.getNavigation().moveTo(prey, CHASE_SPEED);
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        } else if (this.hyena.isWithinMeleeAttackRange(prey) && this.hyena.getSensing().hasLineOfSight(prey)) {
            this.hyena.doHurtTarget(prey);
            this.attackCooldown = 15;
        }
        if (this.hyena.getStamina() <= 0 || distance > GIVE_UP_DISTANCE) {
            this.gaveUp = true;
        }
    }

    // The test run scatters the herd; pick the young, injured or straggling animal of the prey's kind
    private LivingEntity markWeakest(LivingEntity prey) {
        List<Animal> candidates = prey.level().getEntitiesOfClass(Animal.class, prey.getBoundingBox().inflate(MARK_RANGE),
                animal -> animal.getType() == prey.getType() && this.hyena.isValidPrey(animal, this.hunt.getHunters().size()));
        if (candidates.isEmpty()) {
            return prey;
        }
        Vec3 center = Vec3.ZERO;
        for (Animal animal : candidates) {
            center = center.add(animal.position());
        }
        center = center.scale(1.0D / candidates.size());
        LivingEntity best = prey;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Animal animal : candidates) {
            double score = (animal.isBaby() ? 4.0D : 0.0D) + 3.0D * (1.0D - animal.getHealth() / animal.getMaxHealth()) + animal.position().distanceTo(center) / 4.0D;
            if (score > bestScore) {
                bestScore = score;
                best = animal;
            }
        }
        return best;
    }

    @Override
    public void stop() {
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.EXCITED);
        this.hyena.getNavigation().stop();
        if (this.hunt != null && !this.hunt.isFinished()) {
            boolean failed = this.gaveUp || this.ticks >= MAX_HUNT_TICKS;
            if (this.hyena == this.hunt.getLeader()) {
                this.hunt.finish(failed ? FAILED_HUNT_COOLDOWN : 0);
            } else if (this.hyena.getHunt() == this.hunt) {
                this.hyena.setHunt(null);
                if (failed) {
                    this.hyena.setHuntCooldown(FAILED_HUNT_COOLDOWN);
                }
            }
        }
        this.hunt = null;
        this.gaveUp = false;
    }
}
