package net.untamed.entity.ai.vulture;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.VultureEntity;

public class VultureMoveControl extends MoveControl {

    private static final double NAV_ACCEL = 0.01;
    private static final double NAV_MAX_SPEED = 0.45;
    private static final float AIR_MAX_TURN = 4.0F;
    private static final float GROUND_MAX_TURN = 30.0F;

    private final VultureEntity vulture;

    public VultureMoveControl(VultureEntity vulture) {
        super(vulture);
        this.vulture = vulture;
    }

    @Override
    public void tick() {
        if (this.operation != Operation.MOVE_TO) {
            if (this.mob.onGround()) {
                this.mob.setSpeed(0.0F);
                this.mob.setXxa(0.0F);
                this.mob.setYya(0.0F);
                this.mob.setZza(0.0F);
            }
            return;
        }
        this.operation = Operation.WAIT;

        double dx = this.wantedX - this.mob.getX();
        double dy = this.wantedY - this.mob.getY();
        double dz = this.wantedZ - this.mob.getZ();
        double distSqr = dx * dx + dy * dy + dz * dz;

        if (this.vulture.getFlightState() == VultureEntity.FLIGHT_GROUND) {
            if (distSqr < 1.0E-4) {
                this.mob.setZza(0.0F);
                return;
            }
            float targetYaw = (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, GROUND_MAX_TURN));
            float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            this.mob.setSpeed(speed);
            this.mob.zza = speed;
        } else {
            if (distSqr < 0.0025) return;
            double dist = Math.sqrt(distSqr);
            Vec3 dm = this.mob.getDeltaMovement();
            double accel = NAV_ACCEL * this.speedModifier;
            this.mob.setDeltaMovement(dm.add(dx / dist * accel, dy / dist * accel, dz / dist * accel));
            Vec3 nd = this.mob.getDeltaMovement();
            double speed = Math.sqrt(nd.x * nd.x + nd.y * nd.y + nd.z * nd.z);
            if (speed > NAV_MAX_SPEED) this.mob.setDeltaMovement(nd.scale(NAV_MAX_SPEED / speed));
            float targetYaw = (float) (Mth.atan2(-nd.x, nd.z) * (180F / Math.PI));
            float delta = Mth.wrapDegrees(targetYaw - this.mob.getYRot());
            this.mob.setYRot(this.mob.getYRot() + Mth.clamp(delta, -AIR_MAX_TURN, AIR_MAX_TURN));
            this.mob.yBodyRot = this.mob.getYRot();
            this.mob.yHeadRot = this.mob.getYRot();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > 1.0E-4) {
                float targetPitch = (float) (-(Mth.atan2(dy, horizontal) * (180F / Math.PI)));
                float pitchDelta = Mth.wrapDegrees(targetPitch - this.mob.getXRot());
                this.mob.setXRot(this.mob.getXRot() + Mth.clamp(pitchDelta, -5.0F, 5.0F));
            }
        }
    }
}