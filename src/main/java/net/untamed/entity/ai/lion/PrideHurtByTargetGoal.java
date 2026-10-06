package net.untamed.entity.ai.lion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.untamed.entity.AbstractLionEntity;

public class PrideHurtByTargetGoal extends HurtByTargetGoal {

    private static final double ALERT_RANGE = 24.0D;

    private final AbstractLionEntity lion;

    public PrideHurtByTargetGoal(AbstractLionEntity lion) {
        super(lion);
        this.lion = lion;
    }

    @Override
    public void start() {
        super.start();
        LivingEntity attacker = this.lion.getLastHurtByMob();
        if (attacker != null && !(attacker instanceof AbstractLionEntity other && this.lion.isInPrideWith(other))) {
            for (AbstractLionEntity member : this.lion.getPride()) {
                if (member.isAlive() && !member.isBaby() && member.getTarget() == null && member != attacker && this.lion.distanceToSqr(member) < ALERT_RANGE * ALERT_RANGE) {
                    member.setTarget(attacker);
                }
            }
        }
        if (this.lion.isBaby()) {
            this.stop();
        }
    }
}
