package net.untamed.entity.ai.herd;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.untamed.entity.AbstractLionEntity;
import net.untamed.entity.HerdBovineEntity;

public class MobThreatGoal extends NearestAttackableTargetGoal<LivingEntity> {

    private static final double THREAT_RANGE = 12.0D;
    private static final double RALLY_RANGE = 16.0D;
    private static final double CALF_GUARD_RANGE = 16.0D;

    private final HerdBovineEntity herdMob;

    public MobThreatGoal(HerdBovineEntity mob, TagKey<EntityType<?>> threats) {
        super(mob, LivingEntity.class, 10, true, false, entity -> entity.getType().is(threats)
                && (mob.hasCalfNearby(CALF_GUARD_RANGE) || (entity instanceof Mob predator && predator.getTarget() instanceof HerdBovineEntity)
                || (entity instanceof AbstractLionEntity lion && lion.getHuntPrey() instanceof HerdBovineEntity)));
        this.herdMob = mob;
    }

    @Override
    public boolean canUse() {
        return !this.herdMob.isBaby() && super.canUse();
    }

    @Override
    protected double getFollowDistance() {
        return THREAT_RANGE;
    }

    @Override
    public void start() {
        super.start();
        if (this.target == null) {
            return;
        }
        for (HerdBovineEntity member : this.herdMob.getHerd()) {
            if (member.isAlive() && !member.isBaby() && member.getTarget() == null && this.herdMob.distanceToSqr(member) < RALLY_RANGE * RALLY_RANGE) {
                member.setTarget(this.target);
            }
        }
    }
}
