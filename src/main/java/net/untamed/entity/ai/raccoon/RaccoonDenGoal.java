package net.untamed.entity.ai.raccoon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.RaccoonEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class RaccoonDenGoal extends Goal {

    private static final int START_CHANCE = 40;
    private static final int SEARCH_RANGE = 16;
    private static final double MAX_DEN_DISTANCE = 32.0D;
    private static final double AT_DEN = 1.5D;
    private static final double KIT_REST_RANGE = 4.0D;
    private static final int MIN_REST_TICKS = 1200;
    private static final int REST_TICKS_VARIATION = 1200;
    private static final int COOLDOWN = 300;

    private final RaccoonEntity raccoon;
    private boolean walking;
    private int restTicks;
    private long nextRestTime;

    public RaccoonDenGoal(RaccoonEntity raccoon) {
        this.raccoon = raccoon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!this.canRestNow() || this.raccoon.level().getGameTime() < this.nextRestTime || !this.raccoon.onGround() || this.raccoon.isInWater()) {
            return false;
        }
        return this.isMotherResting() || this.raccoon.getRandom().nextInt(reducedTickDelay(START_CHANCE)) == 0;
    }

    private boolean canRestNow() {
        if (this.raccoon.isAlarmed() || this.raccoon.isInLove() || this.raccoon.isHoldingFood()) {
            return false;
        }
        return this.raccoon.isRestTime() || this.isMotherResting();
    }

    private boolean isMotherResting() {
        RaccoonEntity mother = this.raccoon.getMother();
        return mother != null && mother.isResting() && this.raccoon.distanceToSqr(mother) < KIT_REST_RANGE * KIT_REST_RANGE;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canRestNow() && (this.walking || (this.raccoon.isResting() && this.restTicks > 0));
    }

    @Override
    public void start() {
        this.walking = false;
        BlockPos den = this.findDen();
        if (den != null && this.raccoon.position().distanceToSqr(Vec3.atBottomCenterOf(den)) > AT_DEN * AT_DEN) {
            this.walking = this.raccoon.getNavigation().moveTo(den.getX() + 0.5D, den.getY(), den.getZ() + 0.5D, 1.0D);
        }
        if (!this.walking) {
            this.lieDown();
        }
    }

    @Nullable
    private BlockPos findDen() {
        if (this.isMotherResting()) {
            return null;
        }
        RaccoonEntity mother = this.raccoon.getMother();
        if (mother != null && mother.getDenPos() != null) {
            return mother.getDenPos();
        }
        BlockPos den = this.raccoon.getDenPos();
        if (den != null && this.raccoon.blockPosition().closerThan(den, MAX_DEN_DISTANCE) && this.raccoon.scoreDenSpot(den) > 0.0D) {
            return den;
        }
        if (this.raccoon.scoreDenSpot(this.raccoon.blockPosition()) > 0.0D) {
            this.raccoon.setDenPos(this.raccoon.blockPosition());
            return null;
        }
        Vec3 spot = LandRandomPos.getPos(this.raccoon, SEARCH_RANGE, 6, this.raccoon::scoreDenSpot);
        if (spot != null && this.raccoon.scoreDenSpot(BlockPos.containing(spot)) > 0.0D) {
            this.raccoon.setDenPos(BlockPos.containing(spot));
            return BlockPos.containing(spot);
        }
        return null;
    }

    @Override
    public void tick() {
        if (this.walking) {
            if (this.raccoon.getNavigation().isDone()) {
                this.walking = false;
                this.lieDown();
            }
        } else {
            this.restTicks--;
            this.raccoon.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.walking = false;
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.RESTING);
        this.nextRestTime = this.raccoon.level().getGameTime() + COOLDOWN;
    }

    private void lieDown() {
        this.restTicks = this.adjustedTickDelay(MIN_REST_TICKS + this.raccoon.getRandom().nextInt(REST_TICKS_VARIATION));
        this.raccoon.getNavigation().stop();
        this.raccoon.setRaccoonPose(RaccoonEntity.RaccoonPose.RESTING);
    }
}
