package net.untamed.entity.ai.raccoon;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.RaccoonEntity;
import net.untamed.entity.ai.CarcassTracker;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class ScavengeFoodGoal extends Goal {

    private static final int START_CHANCE = 20;
    private static final double ITEM_RANGE = 12.0D;
    private static final double CARCASS_RANGE = 24.0D;
    private static final double FEED_DISTANCE = 1.5D;
    private static final int FEED_TICKS = 40;
    private static final int MAX_TICKS = 400;
    private static final int REPATH_TICKS = 20;

    private final RaccoonEntity raccoon;
    @Nullable
    private ItemEntity item;
    @Nullable
    private CarcassTracker.Carcass carcass;
    private int ticks;
    private int feedTicks;
    private int repathTicks;

    public ScavengeFoodGoal(RaccoonEntity raccoon) {
        this.raccoon = raccoon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.raccoon.isBaby() || !this.raccoon.getMainHandItem().isEmpty() || this.raccoon.isResting() || this.raccoon.isAlarmed()
                || this.raccoon.getRandom().nextInt(reducedTickDelay(START_CHANCE)) != 0) {
            return false;
        }
        this.item = this.findItem();
        this.carcass = null;
        if (this.item == null && !this.raccoon.isRestTime() && this.raccoon.isHungry() && this.raccoon.level() instanceof ServerLevel serverLevel) {
            this.carcass = CarcassTracker.findNearest(serverLevel, this.raccoon.position(), CARCASS_RANGE);
        }
        return this.item != null || this.carcass != null;
    }

    @Nullable
    private ItemEntity findItem() {
        ItemEntity nearest = null;
        for (ItemEntity candidate : this.raccoon.level().getEntitiesOfClass(ItemEntity.class, this.raccoon.getBoundingBox().inflate(ITEM_RANGE, 4.0D, ITEM_RANGE),
                entity -> entity.isAlive() && !entity.hasPickUpDelay() && this.raccoon.wantsToPickUp(entity.getItem()))) {
            if (nearest == null || this.raccoon.distanceToSqr(candidate) < this.raccoon.distanceToSqr(nearest)) {
                nearest = candidate;
            }
        }
        return nearest;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.ticks >= MAX_TICKS || this.raccoon.isAlarmed() || !this.raccoon.getMainHandItem().isEmpty()) {
            return false;
        }
        if (this.item != null) {
            return this.item.isAlive();
        }
        return this.carcass != null && this.raccoon.level() instanceof ServerLevel serverLevel && this.carcass.isValid(serverLevel);
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.feedTicks = 0;
        this.repathTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
        Vec3 target = this.item != null ? this.item.position() : this.carcass != null ? Vec3.atBottomCenterOf(this.carcass.getPos()) : null;
        if (target == null) {
            return;
        }
        this.raccoon.getLookControl().setLookAt(target.x, target.y, target.z);
        if (this.carcass != null && this.raccoon.position().distanceToSqr(target) < FEED_DISTANCE * FEED_DISTANCE) {
            this.raccoon.getNavigation().stop();
            this.raccoon.setRaccoonPose(RaccoonEntity.RaccoonPose.FORAGING);
            if (++this.feedTicks >= FEED_TICKS) {
                this.feedTicks = 0;
                if (this.carcass.bite()) {
                    this.raccoon.onFed(2.0F);
                }
            }
            return;
        }
        if (--this.repathTicks <= 0) {
            this.repathTicks = REPATH_TICKS;
            this.raccoon.getNavigation().moveTo(target.x, target.y, target.z, 1.1D);
        }
    }

    @Override
    public void stop() {
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.FORAGING);
        this.raccoon.getNavigation().stop();
        this.item = null;
        this.carcass = null;
    }
}
