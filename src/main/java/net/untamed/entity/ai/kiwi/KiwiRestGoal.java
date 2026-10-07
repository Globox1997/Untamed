package net.untamed.entity.ai.kiwi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.KiwiEntity;

import java.util.EnumSet;

public class KiwiRestGoal extends Goal {

    private static final int START_CHANCE = 80;
    private static final int MIN_REST_TICKS = 600;
    private static final int REST_TICKS_VARIATION = 900;
    private static final int COOLDOWN = 400;
    private static final int SEARCH_RANGE = 16;
    private static final double MAX_BURROW_DISTANCE = 32.0D;
    private static final double AT_BURROW = 1.5D;

    private final KiwiEntity kiwi;
    private boolean walking;
    private int restTicks;
    private long nextRestTime;

    public KiwiRestGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.kiwi.isRestTime() && !this.kiwi.isAlarmed() && !this.kiwi.isInLove() && this.kiwi.onGround() && !this.kiwi.isInWater()
                && this.kiwi.level().getGameTime() >= this.nextRestTime && this.kiwi.getRandom().nextInt(reducedTickDelay(START_CHANCE)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.kiwi.isRestTime() && !this.kiwi.isAlarmed() && !this.kiwi.isInLove() && (this.walking || (this.kiwi.isSleeping() && this.restTicks > 0));
    }

    @Override
    public void start() {
        this.walking = false;
        BlockPos burrow = this.kiwi.getBurrowPos();
        if (burrow == null || !this.kiwi.blockPosition().closerThan(burrow, MAX_BURROW_DISTANCE) || !this.kiwi.isHiddenSpot(burrow)) {
            burrow = this.findBurrow();
            if (burrow != null) {
                this.kiwi.setBurrowPos(burrow);
            }
        }
        if (burrow != null && this.kiwi.position().distanceToSqr(Vec3.atBottomCenterOf(burrow)) > AT_BURROW * AT_BURROW) {
            this.walking = this.kiwi.getNavigation().moveTo(burrow.getX() + 0.5D, burrow.getY(), burrow.getZ() + 0.5D, 1.0D);
        }
        if (!this.walking) {
            this.sleep();
        }
    }

    private BlockPos findBurrow() {
        Vec3 spot = LandRandomPos.getPos(this.kiwi, SEARCH_RANGE, 6, this.kiwi::scoreHiddenSpot);
        if (spot != null && this.kiwi.isHiddenSpot(BlockPos.containing(spot))) {
            return BlockPos.containing(spot);
        }
        return this.kiwi.isHiddenSpot(this.kiwi.blockPosition()) ? this.kiwi.blockPosition() : null;
    }

    @Override
    public void tick() {
        if (this.walking) {
            if (this.kiwi.getNavigation().isDone()) {
                this.walking = false;
                this.sleep();
            }
        } else {
            this.restTicks--;
            this.kiwi.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.walking = false;
        this.kiwi.clearKiwiPose(KiwiEntity.KiwiPose.SLEEPING);
        this.nextRestTime = this.kiwi.level().getGameTime() + COOLDOWN;
    }

    private void sleep() {
        this.restTicks = this.adjustedTickDelay(MIN_REST_TICKS + this.kiwi.getRandom().nextInt(REST_TICKS_VARIATION));
        this.kiwi.getNavigation().stop();
        this.kiwi.setKiwiPose(KiwiEntity.KiwiPose.SLEEPING);
    }
}
