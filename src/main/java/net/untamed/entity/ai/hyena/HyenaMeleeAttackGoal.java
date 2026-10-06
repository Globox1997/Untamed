package net.untamed.entity.ai.hyena;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.untamed.entity.HyenaEntity;

public class HyenaMeleeAttackGoal extends MeleeAttackGoal {

    private final HyenaEntity hyena;

    public HyenaMeleeAttackGoal(HyenaEntity hyena, double speedModifier) {
        super(hyena, speedModifier, true);
        this.hyena = hyena;
    }

    @Override
    public void start() {
        super.start();
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.RESTING);
        this.hyena.setHyenaPose(HyenaEntity.HyenaPose.EXCITED);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity livingEntity) {
        if (this.canPerformAttack(livingEntity)) {
            this.resetAttackCooldown();
            this.mob.doHurtTarget(livingEntity);
        } else if (this.mob.distanceToSqr(livingEntity) < (livingEntity.getBbWidth() + 3.0F) * (livingEntity.getBbWidth() + 3.0F)) {
            if (this.isTimeToAttack()) {
                this.resetAttackCooldown();
            }
            if (this.getTicksUntilNextAttack() <= 10) {
                this.hyena.playWarningSound();
            }
        } else {
            this.resetAttackCooldown();
        }
    }

    @Override
    public void stop() {
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.EXCITED);
        super.stop();
    }
}
