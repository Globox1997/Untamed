package net.untamed.entity.ai.octopus;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.OctopusEntity;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class CollectItemGoal extends Goal {

    private static final double SEARCH_RANGE = 12.0D;
    private static final double PICKUP_DISTANCE = 1.5D;
    private static final double AT_DEN = 1.5D;
    private static final int MAX_TICKS = 800;
    private static final int MAX_DEN_ITEMS = 6;

    private final OctopusEntity octopus;
    @Nullable
    private ItemEntity item;
    private int ticks;
    private int repathTicks;
    private boolean done;

    public CollectItemGoal(OctopusEntity octopus) {
        this.octopus = octopus;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.octopus.isBaby() || !this.octopus.isInWater() || this.octopus.isResting() || this.octopus.isThreatened()
                || this.octopus.getRandom().nextInt(reducedTickDelay(100)) != 0) {
            return false;
        }
        if (!this.octopus.getMainHandItem().isEmpty()) {
            return this.ensureDen() != null;
        }
        this.item = null;
        for (ItemEntity candidate : this.octopus.level().getEntitiesOfClass(ItemEntity.class, this.octopus.getBoundingBox().inflate(SEARCH_RANGE),
                entity -> entity.isAlive() && entity.isInWater() && entity.getItem().is(TagInit.OCTOPUS_COLLECTIBLES))) {
            if (this.item == null || this.octopus.distanceToSqr(candidate) < this.octopus.distanceToSqr(this.item)) {
                this.item = candidate;
            }
        }
        return this.item != null;
    }

    @Nullable
    private BlockPos ensureDen() {
        BlockPos den = this.octopus.getDenPos();
        if (den == null || !this.octopus.isDenSpot(den)) {
            den = OctopusDenGoal.findDen(this.octopus);
            this.octopus.setDenPos(den);
        }
        return den;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.done && this.ticks < MAX_TICKS && !this.octopus.isThreatened() && this.octopus.isInWater();
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.repathTicks = 0;
        this.done = false;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.octopus.getMainHandItem().isEmpty()) {
            this.goToItem();
        } else {
            this.carryToDen();
        }
    }

    private void goToItem() {
        if (this.item == null || !this.item.isAlive()) {
            this.done = true;
            return;
        }
        this.octopus.getLookControl().setLookAt(this.item, 30.0F, 30.0F);
        if (this.octopus.distanceTo(this.item) > PICKUP_DISTANCE) {
            if (--this.repathTicks <= 0) {
                this.repathTicks = this.adjustedTickDelay(10);
                this.octopus.getNavigation().moveTo(this.item, 1.0D);
            }
            return;
        }
        ItemStack stack = this.item.getItem();
        ItemStack taken = stack.split(1);
        if (stack.isEmpty()) {
            this.item.discard();
        } else {
            this.item.setItem(stack);
        }
        this.octopus.setItemSlot(EquipmentSlot.MAINHAND, taken);
        this.octopus.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        this.octopus.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 1.2F);
        this.item = null;
        if (this.ensureDen() == null) {
            this.done = true;
        }
    }

    private void carryToDen() {
        BlockPos den = this.octopus.getDenPos();
        if (den == null) {
            this.done = true;
            return;
        }
        Vec3 denCenter = Vec3.atBottomCenterOf(den);
        if (this.octopus.position().distanceToSqr(denCenter) > AT_DEN * AT_DEN) {
            if (--this.repathTicks <= 0) {
                this.repathTicks = this.adjustedTickDelay(20);
                this.octopus.getNavigation().moveTo(denCenter.x, denCenter.y, denCenter.z, 1.0D);
            }
            return;
        }
        int piled = this.octopus.level().getEntitiesOfClass(ItemEntity.class, new AABB(den).inflate(2.0D)).size();
        if (piled < MAX_DEN_ITEMS) {
            ItemEntity dropped = new ItemEntity(this.octopus.level(), denCenter.x, denCenter.y + 0.2D, denCenter.z, this.octopus.getMainHandItem().copy());
            dropped.setExtendedLifetime();
            dropped.setDefaultPickUpDelay();
            this.octopus.level().addFreshEntity(dropped);
            this.octopus.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        this.done = true;
    }

    @Override
    public void stop() {
        this.octopus.getNavigation().stop();
        this.item = null;
    }
}
