package net.untamed.entity.ai.lion;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractLionEntity;

import java.util.List;

public class LionHunt {

    private static final double LEADER_STALK_DISTANCE = 6.0D;
    private static final double FLANK_STALK_DISTANCE = 8.0D;
    private static final float FLANK_ANGLE = 60.0F;
    private static final float MAX_FLANK_ANGLE = 150.0F;

    private final AbstractLionEntity leader;
    private final LivingEntity prey;
    private final List<AbstractLionEntity> hunters;
    private final Vec3 approachAxis;
    private boolean charging;
    private boolean finished;

    public LionHunt(AbstractLionEntity leader, LivingEntity prey, List<AbstractLionEntity> hunters) {
        this.leader = leader;
        this.prey = prey;
        this.hunters = List.copyOf(hunters);
        Vec3 axis = new Vec3(leader.getX() - prey.getX(), 0.0D, leader.getZ() - prey.getZ());
        this.approachAxis = axis.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : axis.normalize();
    }

    public AbstractLionEntity getLeader() {
        return this.leader;
    }

    public LivingEntity getPrey() {
        return this.prey;
    }

    public List<AbstractLionEntity> getHunters() {
        return this.hunters;
    }

    public boolean isCharging() {
        return this.charging;
    }

    public void startCharge() {
        this.charging = true;
    }

    public boolean isFinished() {
        return this.finished || !this.prey.isAlive() || !this.leader.isAlive();
    }

    public void finish(int cooldown) {
        if (this.finished) {
            return;
        }
        this.finished = true;
        for (AbstractLionEntity hunter : this.hunters) {
            if (hunter.getHunt() == this) {
                hunter.setHunt(null);
                if (cooldown > 0) {
                    hunter.setHuntCooldown(cooldown);
                }
            }
        }
    }

    public Vec3 getStalkTarget(AbstractLionEntity hunter) {
        int index = Math.max(0, this.hunters.indexOf(hunter));
        if (index == 0) {
            return this.prey.position().add(this.approachAxis.scale(LEADER_STALK_DISTANCE));
        }
        float angle = Math.min(MAX_FLANK_ANGLE, FLANK_ANGLE * ((index + 1) / 2)) * (index % 2 == 1 ? 1.0F : -1.0F) * Mth.DEG_TO_RAD;
        double cos = Mth.cos(angle);
        double sin = Mth.sin(angle);
        Vec3 direction = new Vec3(this.approachAxis.x * cos - this.approachAxis.z * sin, 0.0D, this.approachAxis.x * sin + this.approachAxis.z * cos);
        return this.prey.position().add(direction.scale(FLANK_STALK_DISTANCE));
    }
}
