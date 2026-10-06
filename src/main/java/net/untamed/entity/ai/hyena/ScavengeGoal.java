package net.untamed.entity.ai.hyena;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.untamed.entity.HyenaEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class ScavengeGoal extends Goal {

    private static final double SEARCH_RANGE = 24.0D;
    private static final double EAT_DISTANCE = 1.5D;
    private static final double CLAN_NEARBY = 16.0D;
    private static final int EAT_TICKS = 40;
    private static final int MAX_TICKS = 600;
    private static final int MEAT_FOOD = 8000;
    private static final int BONE_FOOD = 3000;
    private static final int RECRUIT_FOOD_COUNT = 3;

    private final HyenaEntity hyena;
    @Nullable
    private ItemEntity food;
    private int ticks;
    private int eatTicks;
    private int repathTicks;

    public ScavengeGoal(HyenaEntity hyena) {
        this.hyena = hyena;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.hyena.isPeckish() || this.hyena.isAlarmed() || this.hyena.getTarget() != null || this.hyena.getHunt() != null
                || this.hyena.getRandom().nextInt(reducedTickDelay(20)) != 0) {
            return false;
        }
        List<ItemEntity> items = this.hyena.level().getEntitiesOfClass(ItemEntity.class, this.hyena.getBoundingBox().inflate(SEARCH_RANGE, 8.0D, SEARCH_RANGE),
                item -> item.isAlive() && this.hyena.isFood(item.getItem()));
        this.food = null;
        int total = 0;
        for (ItemEntity item : items) {
            total += item.getItem().getCount();
            if (this.food == null || this.hyena.distanceToSqr(item) < this.hyena.distanceToSqr(this.food)) {
                this.food = item;
            }
        }
        if (this.food == null || this.hyena.getNavigation().createPath(this.food, 1) == null) {
            this.food = null;
            return false;
        }
        if (total >= RECRUIT_FOOD_COUNT && !this.hyena.isBaby() && this.hyena.countAdultsNearby(CLAN_NEARBY) == 0) {
            this.hyena.requestWhoop(0, true);
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.food != null && this.food.isAlive() && !this.food.getItem().isEmpty() && this.hyena.isPeckish() && this.hyena.getTarget() == null
                && this.hyena.getHunt() == null && this.ticks > 0;
    }

    @Override
    public void start() {
        this.ticks = this.adjustedTickDelay(MAX_TICKS);
        this.eatTicks = 0;
        this.repathTicks = 0;
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.RESTING);
    }

    @Override
    public void tick() {
        if (this.food == null) {
            return;
        }
        this.ticks--;
        this.hyena.getLookControl().setLookAt(this.food.getX(), this.food.getY(), this.food.getZ());
        if (this.hyena.distanceToSqr(this.food) > EAT_DISTANCE * EAT_DISTANCE) {
            this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.EATING);
            this.eatTicks = 0;
            if (--this.repathTicks <= 0) {
                this.repathTicks = this.adjustedTickDelay(10);
                this.hyena.getNavigation().moveTo(this.food, 1.2D);
            }
            return;
        }
        this.hyena.getNavigation().stop();
        this.hyena.setHyenaPose(HyenaEntity.HyenaPose.EATING);
        if (++this.eatTicks >= this.adjustedTickDelay(EAT_TICKS)) {
            this.eatTicks = 0;
            this.eat(this.food);
        }
    }

    private void eat(ItemEntity item) {
        ItemStack stack = item.getItem();
        boolean bone = stack.is(Items.BONE);
        this.hyena.feed(bone ? BONE_FOOD : MEAT_FOOD);
        this.hyena.playSound(bone ? SoundEvents.BONE_BLOCK_BREAK : SoundEvents.GENERIC_EAT, 0.8F, this.hyena.getVoicePitch());
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            item.setItem(stack);
        }
    }

    @Override
    public void stop() {
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.EATING);
        this.hyena.getNavigation().stop();
        this.food = null;
    }
}
