package net.untamed.entity.ai.raccoon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.RaccoonEntity;
import net.untamed.entity.ai.WaterUtils;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class DouseFoodGoal extends Goal {

    private static final int WATER_RANGE = 8;
    private static final double DOUSE_DISTANCE = 2.0D;
    private static final int MIN_DOUSE_TICKS = 40;
    private static final int DOUSE_TICKS_VARIATION = 40;
    private static final int EAT_DIRECTLY_TICKS = 30;
    private static final int SPLASH_INTERVAL = 8;
    private static final int MAX_TICKS = 400;

    private final RaccoonEntity raccoon;
    @Nullable
    private BlockPos water;
    private boolean walking;
    private boolean dousing;
    private boolean done;
    private int actionTicks;
    private int ticks;

    public DouseFoodGoal(RaccoonEntity raccoon) {
        this.raccoon = raccoon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.raccoon.isHoldingFood() && this.raccoon.onGround() && !this.raccoon.isResting() && !this.raccoon.isAlarmed();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.done && this.ticks < MAX_TICKS && this.raccoon.isHoldingFood() && !this.raccoon.isAlarmed();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.done = false;
        this.ticks = 0;
        this.water = this.raccoon.level() instanceof ServerLevel serverLevel ? WaterUtils.findNearestWater(serverLevel, this.raccoon, WATER_RANGE, 4, 1) : null;
        this.walking = this.water != null && this.raccoon.getNavigation().moveTo(this.water.getX() + 0.5D, this.water.getY(), this.water.getZ() + 0.5D, 1.0D);
        if (!this.walking) {
            this.beginEating(false);
        }
    }

    private void beginEating(boolean atWater) {
        this.walking = false;
        this.dousing = atWater;
        this.actionTicks = atWater ? MIN_DOUSE_TICKS + this.raccoon.getRandom().nextInt(DOUSE_TICKS_VARIATION) : EAT_DIRECTLY_TICKS;
        this.raccoon.getNavigation().stop();
        this.raccoon.setRaccoonPose(atWater ? RaccoonEntity.RaccoonPose.DOUSING : RaccoonEntity.RaccoonPose.SITTING_UP);
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.walking) {
            Vec3 center = Vec3.atBottomCenterOf(this.water);
            double dx = center.x - this.raccoon.getX();
            double dz = center.z - this.raccoon.getZ();
            boolean close = dx * dx + dz * dz < DOUSE_DISTANCE * DOUSE_DISTANCE || this.raccoon.isInWater();
            if (close || this.raccoon.getNavigation().isDone()) {
                this.beginEating(close);
            }
            return;
        }
        if (this.water != null && this.dousing) {
            this.raccoon.getLookControl().setLookAt(this.water.getX() + 0.5D, this.water.getY() + 0.5D, this.water.getZ() + 0.5D);
            if (this.actionTicks % SPLASH_INTERVAL == 0 && this.raccoon.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH, this.water.getX() + 0.5D, this.water.getY() + 1.0D, this.water.getZ() + 0.5D, 4, 0.2D, 0.0D, 0.2D, 0.0D);
                this.raccoon.playSound(SoundEvents.GENERIC_SPLASH, 0.15F, 1.4F + this.raccoon.getRandom().nextFloat() * 0.3F);
            }
        }
        if (--this.actionTicks <= 0) {
            this.raccoon.eatHeldFood();
            this.done = true;
        }
    }

    @Override
    public void stop() {
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.DOUSING);
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.SITTING_UP);
        this.raccoon.getNavigation().stop();
        this.water = null;
    }
}
