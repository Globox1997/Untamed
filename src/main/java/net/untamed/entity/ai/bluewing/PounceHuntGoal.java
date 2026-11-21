package net.untamed.entity.ai.bluewing;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.AbstractBirdEntity;
import net.untamed.entity.BluewingEntity;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PounceHuntGoal extends Goal {

    private static final int START_CHANCE = 60;
    private static final double SEARCH_HORIZONTAL = 12.0D;
    private static final double SEARCH_VERTICAL = 8.0D;
    private static final double DIVE_SPEED = 1.6D;
    private static final double GIVE_UP_DISTANCE = 16.0D;
    private static final int REPATH_TICKS = 5;
    private static final int ATTACK_COOLDOWN = 15;
    private static final int BEAT_TICKS = 40;
    private static final int MAX_TICKS = 200;

    private final BluewingEntity bluewing;
    @Nullable
    private LivingEntity prey;
    private int ticks;
    private int repathTicks;
    private int attackCooldown;
    private int beatTicks;

    public PounceHuntGoal(BluewingEntity bluewing) {
        this.bluewing = bluewing;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.bluewing.isBaby() || !this.bluewing.isHungry() || this.bluewing.isAlarmed() || this.bluewing.isBusy() || this.bluewing.isRoostTime()
                || this.bluewing.getRandom().nextInt(reducedTickDelay(START_CHANCE)) != 0) {
            return false;
        }
        this.prey = null;
        for (LivingEntity candidate : this.bluewing.level().getEntitiesOfClass(LivingEntity.class, this.bluewing.getBoundingBox().inflate(SEARCH_HORIZONTAL, SEARCH_VERTICAL, SEARCH_HORIZONTAL),
                entity -> entity.isAlive() && entity.getType().is(TagInit.BLUEWING_PREY) && !entity.isInWater() && entity.getY() <= this.bluewing.getY() + 1.0D)) {
            if (this.prey == null || this.bluewing.distanceToSqr(candidate) < this.bluewing.distanceToSqr(this.prey)) {
                this.prey = candidate;
            }
        }
        return this.prey != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.ticks >= MAX_TICKS || this.bluewing.isAlarmed()) {
            return false;
        }
        if (this.beatTicks > 0) {
            return true;
        }
        return this.prey != null && this.prey.isAlive() && this.bluewing.distanceTo(this.prey) < GIVE_UP_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.repathTicks = 0;
        this.attackCooldown = 0;
        this.beatTicks = 0;
        this.bluewing.clearBirdPose(AbstractBirdEntity.BirdPose.PERCHED);
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.beatTicks > 0) {
            this.bluewing.getNavigation().stop();
            if (--this.beatTicks == 0) {
                this.bluewing.clearBirdPose(AbstractBirdEntity.BirdPose.BEATING);
                this.bluewing.onFed(3.0F);
            }
            return;
        }
        if (this.prey == null) {
            return;
        }
        this.bluewing.getLookControl().setLookAt(this.prey, 30.0F, 30.0F);
        if (--this.repathTicks <= 0) {
            this.repathTicks = REPATH_TICKS;
            this.bluewing.getNavigation().moveTo(this.prey, DIVE_SPEED);
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        } else if (this.bluewing.isWithinMeleeAttackRange(this.prey)) {
            this.bluewing.doHurtTarget(this.prey);
            this.attackCooldown = ATTACK_COOLDOWN;
            if (!this.prey.isAlive()) {
                this.prey = null;
                this.beatTicks = BEAT_TICKS;
                this.bluewing.getNavigation().stop();
                this.bluewing.setBirdPose(AbstractBirdEntity.BirdPose.BEATING);
            }
        }
    }

    @Override
    public void stop() {
        this.bluewing.clearBirdPose(AbstractBirdEntity.BirdPose.BEATING);
        this.bluewing.getNavigation().stop();
        this.prey = null;
        this.beatTicks = 0;
    }
}
