package net.untamed.entity.ai.bear;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.untamed.entity.BlackBearEntity;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BearScavengeGoal extends Goal {

    private static final double SEARCH_RANGE = 16.0D;
    private static final double EAT_DISTANCE = 1.8D;
    private static final int EAT_TICKS = 40;
    private static final int MAX_TICKS = 600;
    private static final int ITEM_FOOD = 6000;

    private final BlackBearEntity bear;
    @Nullable
    private ItemEntity food;
    private int ticks;
    private int eatTicks;
    private int repathTicks;

    public BearScavengeGoal(BlackBearEntity bear) {
        this.bear = bear;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.bear.isPeckish() || this.bear.isAlarmed() || this.bear.getTarget() != null || this.bear.isResting()
                || this.bear.getRandom().nextInt(reducedTickDelay(20)) != 0) {
            return false;
        }
        this.food = null;
        for (ItemEntity item : this.bear.level().getEntitiesOfClass(ItemEntity.class, this.bear.getBoundingBox().inflate(SEARCH_RANGE, 6.0D, SEARCH_RANGE),
                item -> item.isAlive() && item.getItem().is(TagInit.BLACK_BEAR_SCAVENGE))) {
            if (this.food == null || this.bear.distanceToSqr(item) < this.bear.distanceToSqr(this.food)) {
                this.food = item;
            }
        }
        if (this.food == null || this.bear.getNavigation().createPath(this.food, 1) == null) {
            this.food = null;
            return false;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.food != null && this.food.isAlive() && !this.food.getItem().isEmpty() && this.bear.isPeckish() && !this.bear.isAlarmed()
                && this.bear.getTarget() == null && this.ticks > 0;
    }

    @Override
    public void start() {
        this.ticks = this.adjustedTickDelay(MAX_TICKS);
        this.eatTicks = 0;
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        if (this.food == null) {
            return;
        }
        this.ticks--;
        this.bear.getLookControl().setLookAt(this.food.getX(), this.food.getY(), this.food.getZ());
        if (this.bear.distanceToSqr(this.food) > EAT_DISTANCE * EAT_DISTANCE) {
            this.bear.clearBearPose(BlackBearEntity.BearPose.EATING);
            this.eatTicks = 0;
            if (--this.repathTicks <= 0) {
                this.repathTicks = this.adjustedTickDelay(10);
                this.bear.getNavigation().moveTo(this.food, 1.0D);
            }
            return;
        }
        this.bear.getNavigation().stop();
        this.bear.setBearPose(BlackBearEntity.BearPose.EATING);
        if (++this.eatTicks >= this.adjustedTickDelay(EAT_TICKS)) {
            this.eatTicks = 0;
            ItemStack stack = this.food.getItem();
            stack.shrink(1);
            if (stack.isEmpty()) {
                this.food.discard();
            } else {
                this.food.setItem(stack);
            }
            this.bear.feed(ITEM_FOOD);
            this.bear.playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.8F);
        }
    }

    @Override
    public void stop() {
        this.bear.clearBearPose(BlackBearEntity.BearPose.EATING);
        this.bear.getNavigation().stop();
        this.food = null;
    }
}
