package net.untamed.entity.ai.panther;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.untamed.entity.BlackPantherEntity;

public class PantherMeleeAttackGoal extends MeleeAttackGoal {

    private final BlackPantherEntity panther;

    public PantherMeleeAttackGoal(BlackPantherEntity panther, double speedModifier) {
        super(panther, speedModifier, true);
        this.panther = panther;
    }

    @Override
    public boolean canUse() {
        return !this.panther.isBaby() && super.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.panther.clearPantherPose(BlackPantherEntity.PantherPose.RESTING);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity livingEntity) {
        if (this.canPerformAttack(livingEntity)) {
            this.resetAttackCooldown();
            this.mob.doHurtTarget(livingEntity);
            this.panther.clearPantherPose(BlackPantherEntity.PantherPose.SNARLING);
        } else if (this.mob.distanceToSqr(livingEntity) < (livingEntity.getBbWidth() + 3.0F) * (livingEntity.getBbWidth() + 3.0F)) {
            if (this.isTimeToAttack()) {
                this.resetAttackCooldown();
            }
            if (this.getTicksUntilNextAttack() <= 10) {
                this.panther.setPantherPose(BlackPantherEntity.PantherPose.SNARLING);
                this.panther.playWarningSound();
            }
        } else {
            this.resetAttackCooldown();
            this.panther.clearPantherPose(BlackPantherEntity.PantherPose.SNARLING);
        }
    }

    @Override
    public void stop() {
        this.panther.clearPantherPose(BlackPantherEntity.PantherPose.SNARLING);
        super.stop();
    }
}
