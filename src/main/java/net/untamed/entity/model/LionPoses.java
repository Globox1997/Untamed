package net.untamed.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.untamed.entity.AbstractLionEntity;

@Environment(EnvType.CLIENT)
public class LionPoses {

    private float sleepAmount;
    private float stalkAmount;
    private float roarAmount;
    private float warnAmount;
    private float playAmount;

    public void prepare(AbstractLionEntity entity, float partialTick) {
        this.sleepAmount = entity.getSleepAmount(partialTick);
        this.stalkAmount = entity.getStalkAmount(partialTick);
        this.roarAmount = entity.getRoarAmount(partialTick);
        this.warnAmount = entity.getWarnAmount(partialTick);
        this.playAmount = entity.getPlayAmount(partialTick);
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
            body.zRot = Mth.lerp(s, body.zRot, -1.52F);
            body.y += 11.0F * s;
            head.xRot = Mth.lerp(s, head.xRot, 0.0F);
            head.yRot = Mth.lerp(s, head.yRot, 0.0F);
            head.zRot = Mth.lerp(s, head.zRot, 1.44F);
            head.y -= 2.0F * s;
            tail.yRot = Mth.lerp(s, tail.yRot, -0.174F);
            leftFrontLeg.xRot = Mth.lerp(s, leftFrontLeg.xRot, -0.26F);
            leftBackLeg.xRot = Mth.lerp(s, leftBackLeg.xRot, 0.17F);
            rightFrontLeg.xRot = Mth.lerp(s, rightFrontLeg.xRot, 0.2F);
            rightFrontLeg.yRot = Mth.lerp(s, rightFrontLeg.yRot, -0.1F);
            rightFrontLeg.zRot = Mth.lerp(s, rightFrontLeg.zRot, 0.2F);
            rightBackLeg.xRot = Mth.lerp(s, rightBackLeg.xRot, 0.6F);
            rightBackLeg.yRot = Mth.lerp(s, rightBackLeg.yRot, -0.26F);
            rightBackLeg.zRot = Mth.lerp(s, rightBackLeg.zRot, 0.48F);
        }
    }
}
