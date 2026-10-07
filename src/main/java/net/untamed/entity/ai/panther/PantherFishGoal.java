package net.untamed.entity.ai.panther;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.BlackPantherEntity;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PantherFishGoal extends Goal {

    private static final double SEARCH_RANGE = 12.0D;
    private static final double SWIPE_DISTANCE = 2.2D;
    private static final double GIVE_UP_DISTANCE = 16.0D;
    private static final int MAX_TICKS = 400;
    private static final int SWIPE_COOLDOWN = 20;

    private final BlackPantherEntity panther;
    @Nullable
    private LivingEntity fish;
    private int ticks;
    private int swipeCooldown;
    private int repathTicks;

    public PantherFishGoal(BlackPantherEntity panther) {
        this.panther = panther;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.panther.isBaby() || !this.panther.isPeckish() || this.panther.isRestTime() || this.panther.isAlarmed() || this.panther.getTarget() != null
                || this.panther.isHunting() || this.panther.getRandom().nextInt(reducedTickDelay(120)) != 0) {
            return false;
        }
        this.fish = null;
        for (LivingEntity candidate : this.panther.level().getEntitiesOfClass(LivingEntity.class, this.panther.getBoundingBox().inflate(SEARCH_RANGE, 6.0D, SEARCH_RANGE),
                entity -> entity.isAlive() && entity.isInWater() && entity.getType().is(TagInit.BLACK_PANTHER_FISH))) {
            if (this.fish == null || this.panther.distanceToSqr(candidate) < this.panther.distanceToSqr(this.fish)) {
                this.fish = candidate;
            }
        }
        return this.fish != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.fish != null && this.fish.isAlive() && this.ticks < MAX_TICKS && !this.panther.isAlarmed() && this.panther.getTarget() == null
                && this.panther.distanceToSqr(this.fish) < GIVE_UP_DISTANCE * GIVE_UP_DISTANCE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.swipeCooldown = 0;
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        if (this.fish == null) {
            return;
        }
        this.ticks++;
        this.panther.getLookControl().setLookAt(this.fish, 30.0F, 30.0F);
        if (this.swipeCooldown > 0) {
            this.swipeCooldown--;
        }
        if (this.panther.distanceTo(this.fish) > SWIPE_DISTANCE) {
            this.panther.clearPantherPose(BlackPantherEntity.PantherPose.FISHING);
            if (--this.repathTicks <= 0) {
                this.repathTicks = 10;
                this.panther.getNavigation().moveTo(this.fish, 1.0D);
            }
            return;
        }
        this.panther.getNavigation().stop();
        this.panther.setPantherPose(BlackPantherEntity.PantherPose.FISHING);
        if (this.swipeCooldown <= 0) {
            this.swipeCooldown = SWIPE_COOLDOWN;
            this.panther.doHurtTarget(this.fish);
        }
    }

    @Override
    public void stop() {
        this.panther.clearPantherPose(BlackPantherEntity.PantherPose.FISHING);
        this.panther.getNavigation().stop();
        this.fish = null;
    }
}
