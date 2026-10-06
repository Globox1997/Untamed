package net.untamed.entity.ai.herd;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HerdBovineEntity;
import net.untamed.init.TagInit;

import java.util.EnumSet;

public class GrazeGoal extends Goal {

    private static final int ACTIVE_CHANCE = 60;
    private static final int RESTING_TIME_CHANCE = 300;

    private final HerdBovineEntity mob;
    private int grazeTicks;
    private int nextStepTicks;

    public GrazeGoal(HerdBovineEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        int chance = this.mob.isRestTime() ? RESTING_TIME_CHANCE : ACTIVE_CHANCE;
        if (this.mob.getRandom().nextInt(reducedTickDelay(chance)) != 0) {
            return false;
        }
        return this.canGrazeHere() && this.mob.getNavigation().isDone();
    }

    @Override
    public boolean canContinueToUse() {
        return this.grazeTicks > 0 && this.canGrazeHere();
    }

    private boolean canGrazeHere() {
        return !this.mob.isAlarmed() && this.mob.getTarget() == null && this.mob.onGround() && !this.mob.isInWater()
                && this.mob.level().getBlockState(this.mob.blockPosition().below()).is(TagInit.HERD_GRAZEABLE);
    }

    @Override
    public void start() {
        this.grazeTicks = this.adjustedTickDelay(100 + this.mob.getRandom().nextInt(160));
        this.nextStepTicks = this.adjustedTickDelay(40 + this.mob.getRandom().nextInt(40));
        this.mob.getNavigation().stop();
        this.mob.setHerdPose(HerdBovineEntity.HerdPose.GRAZING);
    }

    @Override
    public void stop() {
        this.mob.clearHerdPose(HerdBovineEntity.HerdPose.GRAZING);
    }

    @Override
    public void tick() {
        this.grazeTicks--;
        if (--this.nextStepTicks <= 0) {
            this.nextStepTicks = this.adjustedTickDelay(40 + this.mob.getRandom().nextInt(40));
            this.mob.grazeAt(this.mob.blockPosition());
            float yaw = (this.mob.getYRot() + (this.mob.getRandom().nextFloat() - 0.5F) * 60.0F) * Mth.DEG_TO_RAD;
            Vec3 step = this.mob.position().add(-Mth.sin(yaw) * 1.2D, 0.0D, Mth.cos(yaw) * 1.2D);
            this.mob.getMoveControl().setWantedPosition(step.x, step.y, step.z, 0.4D);
        }
    }
}
