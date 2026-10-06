package net.untamed.entity.ai.herd;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HerdBovineEntity;

import java.util.EnumSet;

public class HerdRestGoal extends Goal {

    private static final int REST_TIME_CHANCE = 80;
    private static final int OFF_TIME_CHANCE = 1200;
    private static final int CONTAGION_CHANCE = 10;
    private static final double CONTAGION_RANGE = 12.0D;
    private static final int COOLDOWN = 200;

    private final HerdBovineEntity mob;
    private boolean seekingShade;
    private int restTicks;
    private long nextRestTime;

    public HerdRestGoal(HerdBovineEntity mob) {
        this.mob = mob;
        // LOOK is left free so resting animals still turn their heads
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.mob.level().getGameTime() < this.nextRestTime || !this.canRestHere()) {
            return false;
        }
        int chance = this.mob.isRestTime() ? REST_TIME_CHANCE : OFF_TIME_CHANCE;
        if (this.mob.countRestingNearby(CONTAGION_RANGE) * 3 >= Math.max(1, this.mob.getHerd().size())) {
            chance = CONTAGION_CHANCE;
        }
        return this.mob.getRandom().nextInt(reducedTickDelay(chance)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mob.isAlarmed() || this.mob.getTarget() != null || this.mob.isInLove()) {
            return false;
        }
        return this.seekingShade || (this.restTicks > 0 && this.mob.onGround() && !this.mob.isInWater());
    }

    private boolean canRestHere() {
        return !this.mob.isAlarmed() && this.mob.getTarget() == null && !this.mob.isInLove() && this.mob.onGround() && !this.mob.isInWater();
    }

    @Override
    public void start() {
        this.seekingShade = false;
        if (this.mob.prefersShade() && this.mob.isRestTime() && this.mob.level().canSeeSky(this.mob.blockPosition())) {
            Vec3 shade = LandRandomPos.getPos(this.mob, 12, 6, this.mob::scoreRestSpot);
            if (shade != null && this.mob.scoreRestSpot(BlockPos.containing(shade)) > 0.0D) {
                this.seekingShade = this.mob.getNavigation().moveTo(shade.x, shade.y, shade.z, 0.9D);
            }
        }
        if (!this.seekingShade) {
            this.lieDown();
        }
    }

    @Override
    public void tick() {
        if (this.seekingShade) {
            if (this.mob.getNavigation().isDone()) {
                this.seekingShade = false;
                this.lieDown();
            }
        } else {
            this.restTicks--;
            this.mob.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.mob.clearHerdPose(HerdBovineEntity.HerdPose.RESTING);
        this.nextRestTime = this.mob.level().getGameTime() + COOLDOWN;
        this.mob.onRestEnded();
    }

    private void lieDown() {
        this.restTicks = this.adjustedTickDelay(this.mob.isRestTime() ? 600 + this.mob.getRandom().nextInt(1200) : 200 + this.mob.getRandom().nextInt(300));
        this.mob.getNavigation().stop();
        this.mob.setHerdPose(HerdBovineEntity.HerdPose.RESTING);
    }
}
