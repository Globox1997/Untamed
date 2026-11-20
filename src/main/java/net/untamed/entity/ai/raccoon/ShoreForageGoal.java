package net.untamed.entity.ai.raccoon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.RaccoonEntity;
import net.untamed.entity.ai.WaterUtils;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class ShoreForageGoal extends Goal {

    private static final int START_CHANCE = 400;
    private static final int WATER_RANGE = 12;
    private static final double FORAGE_DISTANCE = 2.0D;
    private static final int MIN_FORAGE_TICKS = 60;
    private static final int FORAGE_TICKS_VARIATION = 60;
    private static final int SPLASH_INTERVAL = 10;
    private static final int MAX_TICKS = 500;
    private static final float SNACK_CHANCE = 0.3F;
    private static final float FISH_CHANCE = 0.08F;

    private final RaccoonEntity raccoon;
    @Nullable
    private BlockPos water;
    private boolean walking;
    private int forageTicks;
    private int ticks;

    public ShoreForageGoal(RaccoonEntity raccoon) {
        this.raccoon = raccoon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.raccoon.isRestTime() || !this.raccoon.getMainHandItem().isEmpty() || this.raccoon.isAlarmed() || this.raccoon.isResting() || !this.raccoon.onGround()
                || this.raccoon.getRandom().nextInt(reducedTickDelay(START_CHANCE)) != 0 || !(this.raccoon.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        this.water = WaterUtils.findNearestWater(serverLevel, this.raccoon, WATER_RANGE, 4, 1);
        return this.water != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.water != null && this.ticks < MAX_TICKS && !this.raccoon.isAlarmed() && (this.walking || this.forageTicks > 0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.forageTicks = 0;
        this.walking = this.raccoon.getNavigation().moveTo(this.water.getX() + 0.5D, this.water.getY(), this.water.getZ() + 0.5D, 0.9D);
        if (!this.walking) {
            this.beginForaging();
        }
    }

    private void beginForaging() {
        this.walking = false;
        this.forageTicks = MIN_FORAGE_TICKS + this.raccoon.getRandom().nextInt(FORAGE_TICKS_VARIATION);
        this.raccoon.getNavigation().stop();
        this.raccoon.setRaccoonPose(RaccoonEntity.RaccoonPose.FORAGING);
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.water == null) {
            return;
        }
        if (this.walking) {
            Vec3 center = Vec3.atBottomCenterOf(this.water);
            double dx = center.x - this.raccoon.getX();
            double dz = center.z - this.raccoon.getZ();
            if (dx * dx + dz * dz < FORAGE_DISTANCE * FORAGE_DISTANCE || this.raccoon.isInWater() || this.raccoon.getNavigation().isDone()) {
                this.beginForaging();
            }
            return;
        }
        this.raccoon.getLookControl().setLookAt(this.water.getX() + 0.5D, this.water.getY() + 0.5D, this.water.getZ() + 0.5D);
        if (this.forageTicks % SPLASH_INTERVAL == 0 && this.raccoon.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SPLASH, this.water.getX() + 0.5D, this.water.getY() + 1.0D, this.water.getZ() + 0.5D, 3, 0.2D, 0.0D, 0.2D, 0.0D);
        }
        if (--this.forageTicks <= 0) {
            this.finishForaging();
        }
    }

    private void finishForaging() {
        float roll = this.raccoon.getRandom().nextFloat();
        if (roll < FISH_CHANCE) {
            ItemEntity fish = new ItemEntity(this.raccoon.level(), this.raccoon.getX(), this.raccoon.getY() + 0.3D, this.raccoon.getZ(), new ItemStack(Items.COD));
            fish.setDefaultPickUpDelay();
            this.raccoon.level().addFreshEntity(fish);
        } else if (roll < FISH_CHANCE + SNACK_CHANCE) {
            this.raccoon.onFed(1.0F);
        }
    }

    @Override
    public void stop() {
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.FORAGING);
        this.raccoon.getNavigation().stop();
        this.water = null;
    }
}
