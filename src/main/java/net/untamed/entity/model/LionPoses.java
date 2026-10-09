package net.untamed.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.untamed.entity.AbstractLionEntity;

@Environment(EnvType.CLIENT)
public class LionPoses {

    private static final float SLEEP_BODY_ROLL = -1.52F;
    private static final float SLEEP_HEAD_ROLL = 1.44F;
    private static final float HEAD_MIRROR_SHIFT = 6.0F;

    private float sleepAmount;
    private float stalkAmount;
    private float roarAmount;
    private float warnAmount;
    private float playAmount;
    private float sleepSide;

    public void prepare(AbstractLionEntity entity, float partialTick) {
        this.sleepAmount = entity.getSleepAmount(partialTick);
        this.stalkAmount = entity.getStalkAmount(partialTick);
        this.roarAmount = entity.getRoarAmount(partialTick);
        this.warnAmount = entity.getWarnAmount(partialTick);
        this.playAmount = entity.getPlayAmount(partialTick);
        this.sleepSide = entity.getRestVariant() == 0 ? 1.0F : -1.0F;
    }

    public void apply(ModelPart body, ModelPart head, ModelPart tail, ModelPart rightFrontLeg, ModelPart leftFrontLeg, ModelPart rightBackLeg, ModelPart leftBackLeg, float ageInTicks) {
        if (this.stalkAmount > 0.0F) {
            float s = this.stalkAmount;
            body.y += 4.0F * s;
            rightFrontLeg.xRot = Mth.lerp(s, rightFrontLeg.xRot, rightFrontLeg.xRot * 0.4F - 0.84F);
            leftFrontLeg.xRot = Mth.lerp(s, leftFrontLeg.xRot, leftFrontLeg.xRot * 0.4F - 0.84F);
            rightBackLeg.xRot = Mth.lerp(s, rightBackLeg.xRot, rightBackLeg.xRot * 0.4F + 0.84F);
            leftBackLeg.xRot = Mth.lerp(s, leftBackLeg.xRot, leftBackLeg.xRot * 0.4F + 0.84F);
            head.y += 2.0F * s;
            head.xRot = Mth.lerp(s, head.xRot, 0.15F);
            tail.xRot += 0.2F * s;
            tail.yRot += Mth.sin(ageInTicks * 0.3F) * 0.1F * s;
        }
        if (this.roarAmount > 0.0F) {
            body.xRot -= 0.1F * this.roarAmount;
            head.xRot = Mth.lerp(this.roarAmount, head.xRot, -0.7F);
        }
        if (this.warnAmount > 0.0F) {
            head.y += 1.0F * this.warnAmount;
            head.xRot = Mth.lerp(this.warnAmount, head.xRot, 0.25F);
            tail.yRot += Mth.sin(ageInTicks * 0.9F) * 0.5F * this.warnAmount;
        }
        if (this.playAmount > 0.0F) {
            float rear = 0.5F + 0.5F * Mth.sin(ageInTicks * 0.35F);
            body.xRot -= 0.35F * rear * this.playAmount;
            rightFrontLeg.xRot = Mth.lerp(this.playAmount, rightFrontLeg.xRot, -0.8F * rear);
            leftFrontLeg.xRot = Mth.lerp(this.playAmount, leftFrontLeg.xRot, -0.6F * rear);
            tail.yRot += Mth.sin(ageInTicks * 0.6F) * 0.4F * this.playAmount;
        }
        if (this.sleepAmount > 0.0F) {
            float s = this.sleepAmount;
            float side = this.sleepSide;
            body.zRot = Mth.lerp(s, body.zRot, SLEEP_BODY_ROLL * side);
            body.y += 11.0F * s;
            head.xRot = Mth.lerp(s, head.xRot, 0.0F);
            head.yRot = Mth.lerp(s, head.yRot, 0.0F);
            head.zRot = Mth.lerp(s, head.zRot, SLEEP_HEAD_ROLL * side);
            head.y -= 2.0F * s;
            if (side < 0.0F) {
                head.x += (HEAD_MIRROR_SHIFT - HEAD_MIRROR_SHIFT * Mth.cos(-SLEEP_HEAD_ROLL)) * s;
                head.y -= HEAD_MIRROR_SHIFT * Mth.sin(-SLEEP_HEAD_ROLL) * s;
            }
            tail.yRot = Mth.lerp(s, tail.yRot, -0.174F * side);
            ModelPart lowerFrontLeg = side > 0.0F ? leftFrontLeg : rightFrontLeg;
            ModelPart lowerBackLeg = side > 0.0F ? leftBackLeg : rightBackLeg;
            ModelPart upperFrontLeg = side > 0.0F ? rightFrontLeg : leftFrontLeg;
            ModelPart upperBackLeg = side > 0.0F ? rightBackLeg : leftBackLeg;
            lowerFrontLeg.xRot = Mth.lerp(s, lowerFrontLeg.xRot, -0.26F);
            lowerBackLeg.xRot = Mth.lerp(s, lowerBackLeg.xRot, 0.17F);
            upperFrontLeg.xRot = Mth.lerp(s, upperFrontLeg.xRot, 0.2F);
            upperFrontLeg.yRot = Mth.lerp(s, upperFrontLeg.yRot, -0.1F * side);
            upperFrontLeg.zRot = Mth.lerp(s, upperFrontLeg.zRot, 0.2F * side);
            upperBackLeg.xRot = Mth.lerp(s, upperBackLeg.xRot, 0.6F);
            upperBackLeg.yRot = Mth.lerp(s, upperBackLeg.yRot, -0.26F * side);
            upperBackLeg.zRot = Mth.lerp(s, upperBackLeg.zRot, 0.48F * side);
        }
    }
}
