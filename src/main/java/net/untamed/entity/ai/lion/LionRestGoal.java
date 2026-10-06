package net.untamed.entity.ai.lion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractLionEntity;

import java.util.EnumSet;

public class LionRestGoal extends Goal {

    private static final int REST_TIME_CHANCE = 40;
    private static final int ACTIVE_TIME_CHANCE = 1500;
    private static final int CONTAGION_CHANCE = 10;
    private static final double CONTAGION_RANGE = 12.0D;
    private static final int COOLDOWN = 200;

    private final AbstractLionEntity lion;
    private boolean seekingShade;
    private int restTicks;
    private long nextRestTime;

    public LionRestGoal(AbstractLionEntity lion) {
        this.lion = lion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.lion.isBaby() || this.lion.level().getGameTime() < this.nextRestTime || !this.canRest() || !this.lion.onGround() || this.lion.isInWater()) {
            return false;
        }
        int chance = this.lion.isRestTime() ? REST_TIME_CHANCE : ACTIVE_TIME_CHANCE;
        if (this.lion.countSleepingNearby(CONTAGION_RANGE) * 3 >= Math.max(1, this.lion.getPride().size())) {
            chance = CONTAGION_CHANCE;
        }
        return this.lion.getRandom().nextInt(reducedTickDelay(chance)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.canRest()) {
            return false;
        }
        return this.seekingShade || (this.restTicks > 0 && this.lion.onGround() && !this.lion.isInWater());
    }

    private boolean canRest() {
        return !this.lion.isAlarmed() && this.lion.getTarget() == null && this.lion.getHunt() == null && !this.lion.isInLove() && !this.lion.isAngry();
    }

    @Override
    public void start() {
        this.seekingShade = false;
        if (this.lion.isRestTime() && this.lion.level().canSeeSky(this.lion.blockPosition())) {
            Vec3 shade = LandRandomPos.getPos(this.lion, 12, 6, pos -> this.lion.level().canSeeSky(pos.above()) ? -10.0D : 10.0D);
            if (shade != null && !this.lion.level().canSeeSky(BlockPos.containing(shade).above())) {
                this.seekingShade = this.lion.getNavigation().moveTo(shade.x, shade.y, shade.z, 0.8D);
            }
        }
        if (!this.seekingShade) {
            this.lieDown();
        }
    }

    @Override
    public void tick() {
        if (this.seekingShade) {
            if (this.lion.getNavigation().isDone()) {
                this.seekingShade = false;
                this.lieDown();
            }
        } else {
            this.restTicks--;
            this.lion.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.lion.clearLionPose(AbstractLionEntity.LionPose.SLEEPING);
        this.nextRestTime = this.lion.level().getGameTime() + COOLDOWN;
    }

    private void lieDown() {
        this.restTicks = this.adjustedTickDelay(this.lion.isRestTime() ? 1200 + this.lion.getRandom().nextInt(2400) : 200 + this.lion.getRandom().nextInt(400));
        this.lion.getNavigation().stop();
        this.lion.setLionPose(AbstractLionEntity.LionPose.SLEEPING);
    }
}
