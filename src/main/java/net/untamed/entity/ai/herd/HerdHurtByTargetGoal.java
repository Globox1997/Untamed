package net.untamed.entity.ai.herd;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.untamed.entity.HerdBovineEntity;

public class HerdHurtByTargetGoal extends HurtByTargetGoal {

    private final HerdBovineEntity herdMob;

    public HerdHurtByTargetGoal(HerdBovineEntity mob) {
        super(mob);
        this.herdMob = mob;
    }

    @Override
    public void start() {
        super.start();
        if (this.mob.isBaby()) {
            this.alertOthers();
            this.stop();
        } else if (this.herdMob.defendsAsGroup()) {
            this.alertOthers();
        }
    }

    @Override
    protected void alertOther(Mob mob, LivingEntity livingEntity) {
        if (mob instanceof HerdBovineEntity && !mob.isBaby()) {
            super.alertOther(mob, livingEntity);
        }
    }
}
