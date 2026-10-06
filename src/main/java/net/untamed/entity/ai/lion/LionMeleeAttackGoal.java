package net.untamed.entity.ai.lion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.untamed.entity.AbstractLionEntity;

public class LionMeleeAttackGoal extends MeleeAttackGoal {

    private final AbstractLionEntity lion;

    public LionMeleeAttackGoal(AbstractLionEntity lion, double speedModifier) {
        super(lion, speedModifier, true);
        this.lion = lion;
    }

    @Override
    public void start() {
        super.start();
        this.lion.clearLionPose(AbstractLionEntity.LionPose.SLEEPING);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity livingEntity) {
        if (this.canPerformAttack(livingEntity)) {
            this.resetAttackCooldown();
            this.mob.doHurtTarget(livingEntity);
            this.lion.clearLionPose(AbstractLionEntity.LionPose.WARNING);
        } else if (this.mob.distanceToSqr(livingEntity) < (livingEntity.getBbWidth() + 3.0F) * (livingEntity.getBbWidth() + 3.0F)) {
            if (this.isTimeToAttack()) {
                this.lion.clearLionPose(AbstractLionEntity.LionPose.WARNING);
                this.resetAttackCooldown();
            }

            if (this.getTicksUntilNextAttack() <= 10) {
                this.lion.setLionPose(AbstractLionEntity.LionPose.WARNING);
                this.lion.playWarningSound();
            }
        } else {
            this.resetAttackCooldown();
            this.lion.clearLionPose(AbstractLionEntity.LionPose.WARNING);
        }
    }

    @Override
    public void stop() {
        this.lion.clearLionPose(AbstractLionEntity.LionPose.WARNING);
        super.stop();
    }
}
