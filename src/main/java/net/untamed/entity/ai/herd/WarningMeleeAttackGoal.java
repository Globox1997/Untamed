package net.untamed.entity.ai.herd;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.untamed.entity.HerdBovineEntity;

public class WarningMeleeAttackGoal extends MeleeAttackGoal {

    private final HerdBovineEntity herdMob;

    public WarningMeleeAttackGoal(HerdBovineEntity mob, double speedModifier) {
        super(mob, speedModifier, true);
        this.herdMob = mob;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity livingEntity) {
        if (this.canPerformAttack(livingEntity)) {
            this.resetAttackCooldown();
            this.mob.doHurtTarget(livingEntity);
            this.herdMob.clearHerdPose(HerdBovineEntity.HerdPose.WARNING);
        } else if (this.mob.distanceToSqr(livingEntity) < (livingEntity.getBbWidth() + 3.0F) * (livingEntity.getBbWidth() + 3.0F)) {
            if (this.isTimeToAttack()) {
                this.herdMob.clearHerdPose(HerdBovineEntity.HerdPose.WARNING);
                this.resetAttackCooldown();
            }

            if (this.getTicksUntilNextAttack() <= 10) {
                this.herdMob.setHerdPose(HerdBovineEntity.HerdPose.WARNING);
                this.herdMob.playWarningSound();
            }
        } else {
            this.resetAttackCooldown();
            this.herdMob.clearHerdPose(HerdBovineEntity.HerdPose.WARNING);
        }
    }

    @Override
    public void stop() {
        this.herdMob.clearHerdPose(HerdBovineEntity.HerdPose.WARNING);
        super.stop();
    }
}
