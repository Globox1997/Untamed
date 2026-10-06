package net.untamed.entity.ai.hyena;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.untamed.entity.HyenaEntity;

public class ClanHurtByTargetGoal extends HurtByTargetGoal {

    private static final double ALERT_RANGE = 24.0D;

    private final HyenaEntity hyena;

    public ClanHurtByTargetGoal(HyenaEntity hyena) {
        super(hyena);
        this.hyena = hyena;
    }

    @Override
    public void start() {
        super.start();
        LivingEntity attacker = this.hyena.getLastHurtByMob();
        if (attacker != null && !(attacker instanceof HyenaEntity other && this.hyena.isInClanWith(other))) {
            for (HyenaEntity member : this.hyena.getClan()) {
                if (member.isAlive() && !member.isBaby() && member.getTarget() == null && this.hyena.distanceToSqr(member) < ALERT_RANGE * ALERT_RANGE) {
                    member.setTarget(attacker);
                }
            }
        }
        if (this.hyena.isBaby()) {
            this.stop();
        }
    }
}
