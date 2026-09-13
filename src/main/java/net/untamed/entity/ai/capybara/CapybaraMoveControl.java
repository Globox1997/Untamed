package net.untamed.entity.ai.capybara;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.untamed.entity.CapybaraEntity;

public class CapybaraMoveControl extends MoveControl {

    // Tunables
    // KNOWN ISSUE: Capybaras start to bey blade on riverbed
    // when using vanilla SQR = 2.5000003E-7F;
    private static final double ARRIVED_DIST_SQR = 2.5F;  // EVIL MAGIC NUMBER. FIX THE ERROR. WHY? IDK
    private static final double VERTICAL_NOISE_DIST = 0.1;        // below this horizontal distance the bearing is noise
    private static final double INTENT_THRESHOLD = 0.5;           // vertical gap needed to request descend/ascend
    private static final float PITCH_LEVEL_SPEED = 5.0F;          // rotlerp step toward level body

    private final CapybaraEntity capybara;
    private final int maxTurnY;
    private final float inWaterSpeedModifier;
    private final float outsideWaterSpeedModifier;

    public CapybaraMoveControl(CapybaraEntity capybara) {
        super(capybara);
        this.capybara = capybara;
        this.maxTurnY = 15;
        this.inWaterSpeedModifier = 0.6F;
        this.outsideWaterSpeedModifier = 1.0F;
    }

    @Override
    public void tick() {
        if (this.operation == Operation.MOVE_TO && !this.mob.getNavigation().isDone()) {
            double dx = this.wantedX - this.mob.getX();
            double dy = this.wantedY - this.mob.getY();
            double dz = this.wantedZ - this.mob.getZ();
            double distSqr = dx * dx + dy * dy + dz * dz;
            if (distSqr < ARRIVED_DIST_SQR) {
                this.capybara.setNavVerticalIntent(0.0F);
                this.mob.setZza(0.0F);
            } else {
                double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                float targetYaw = horizontalDist < VERTICAL_NOISE_DIST
                        ? this.mob.getYRot()
                        : (float) (Mth.atan2(dz, dx) * (180F / Math.PI)) - 90.0F;
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, this.maxTurnY));
                this.mob.yBodyRot = this.mob.getYRot();
                this.mob.yHeadRot = this.mob.getYRot();
                float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
                if (this.capybara.isFloating()) {
                    this.mob.setSpeed(speed * this.inWaterSpeedModifier);
                    if (this.capybara.isDiving()) {
                        this.capybara.setNavVerticalIntent(-1.0F);
                    } else {
                        this.capybara.setNavVerticalIntent(dy > INTENT_THRESHOLD ? 1.0F : dy < -INTENT_THRESHOLD ? -1.0F : 0.0F);
                    }
                    this.mob.setXRot(this.rotlerp(this.mob.getXRot(), 0.0F, PITCH_LEVEL_SPEED));
                    this.mob.zza = speed;
                    this.mob.yya = 0.0F;
                } else {
                    this.capybara.setNavVerticalIntent(0.0F);
                    this.mob.setSpeed(speed * this.outsideWaterSpeedModifier * getTurningSpeedFactor(Math.abs(Mth.wrapDegrees(this.mob.getYRot() - targetYaw))));
                }
            }
        } else {
            this.capybara.setNavVerticalIntent(0.0F);
            this.mob.setSpeed(0.0F);
            this.mob.setXxa(0.0F);
            this.mob.setYya(0.0F);
            this.mob.setZza(0.0F);
        }
    }

    private static float getTurningSpeedFactor(float degrees) {
        return 1.0F - Mth.clamp((degrees - 10.0F) / 50.0F, 0.0F, 1.0F);
    }
}
