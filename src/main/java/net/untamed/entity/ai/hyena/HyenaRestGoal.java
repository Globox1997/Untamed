package net.untamed.entity.ai.hyena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HyenaEntity;
import net.untamed.init.TagInit;

import java.util.EnumSet;

public class HyenaRestGoal extends Goal {

    private static final int REST_TIME_CHANCE = 40;
    private static final int ACTIVE_TIME_CHANCE = 1500;
    private static final int CONTAGION_CHANCE = 10;
    private static final double CONTAGION_RANGE = 12.0D;
    private static final double DEN_REST_RANGE = 6.0D;
    private static final double DEN_SEEK_RANGE = 48.0D;
    private static final int COOLDOWN = 200;

    private final HyenaEntity hyena;
    private boolean movingToSpot;
    private int restTicks;
    private long nextRestTime;

    public HyenaRestGoal(HyenaEntity hyena) {
        this.hyena = hyena;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.hyena.level().getGameTime() < this.nextRestTime || !this.canRest() || !this.hyena.onGround()) {
            return false;
        }
        int chance = this.hyena.isRestTime() ? REST_TIME_CHANCE : ACTIVE_TIME_CHANCE;
        if (this.hyena.countRestingNearby(CONTAGION_RANGE) * 3 >= Math.max(1, this.hyena.getClan().size())) {
            chance = CONTAGION_CHANCE;
        }
        return this.hyena.getRandom().nextInt(reducedTickDelay(chance)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canRest() && (this.movingToSpot || this.restTicks > 0);
    }

    private boolean canRest() {
        return !this.hyena.isAlarmed() && this.hyena.getTarget() == null && this.hyena.getHunt() == null && this.hyena.getRallyPos() == null
                && !this.hyena.isInLove() && !this.hyena.isAngry();
    }

    @Override
    public void start() {
        this.movingToSpot = false;
        if (this.hyena.isRestTime()) {
            BlockPos den = this.hyena.getDenPos();
            if (den != null && !this.hyena.isNearDen(DEN_REST_RANGE) && this.hyena.isNearDen(DEN_SEEK_RANGE)) {
                this.movingToSpot = this.hyena.getNavigation().moveTo(den.getX() + 0.5D, den.getY(), den.getZ() + 0.5D, 0.8D);
            } else if (!this.hyena.isNearDen(DEN_REST_RANGE) && this.hyena.level().canSeeSky(this.hyena.blockPosition())) {
                Vec3 spot = LandRandomPos.getPos(this.hyena, 10, 4, this::scoreSpot);
                if (spot != null && this.scoreSpot(BlockPos.containing(spot)) > 0.0D) {
                    this.movingToSpot = this.hyena.getNavigation().moveTo(spot.x, spot.y, spot.z, 0.8D);
                }
            }
        }
        if (!this.movingToSpot) {
            this.lieDown();
        }
    }

    private double scoreSpot(BlockPos pos) {
        Level level = this.hyena.level();
        if (level.getBlockState(pos.below()).is(TagInit.HYENA_COOLING_SPOTS)) {
            return 20.0D;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER) || level.getFluidState(pos.below().relative(direction)).is(FluidTags.WATER)) {
                return 20.0D;
            }
        }
        return level.canSeeSky(pos.above()) ? -10.0D : 10.0D;
    }

    @Override
    public void tick() {
        if (this.movingToSpot) {
            if (this.hyena.getNavigation().isDone()) {
                this.movingToSpot = false;
                this.lieDown();
            }
        } else {
            this.restTicks--;
            this.hyena.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.RESTING);
        this.nextRestTime = this.hyena.level().getGameTime() + COOLDOWN;
    }

    private void lieDown() {
        this.restTicks = this.adjustedTickDelay(this.hyena.isRestTime() ? 1200 + this.hyena.getRandom().nextInt(2400) : 200 + this.hyena.getRandom().nextInt(400));
        this.hyena.getNavigation().stop();
        this.hyena.setHyenaPose(HyenaEntity.HyenaPose.RESTING);
    }
}
