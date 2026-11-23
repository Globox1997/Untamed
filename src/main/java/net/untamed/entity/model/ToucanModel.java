package net.untamed.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;
import net.untamed.entity.ToucanEntity;

@Environment(EnvType.CLIENT)
public class ToucanModel<T extends ToucanEntity> extends SinglePartEntityModel<T> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public ToucanModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.tail = this.body.getChild("tail");
        this.leftLeg = this.body.getChild("left_leg");
        this.rightLeg = this.body.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        ModelPartData body = modelPartData.addChild("body", ModelPartBuilder.create()
                .uv(0, 0).cuboid(-2.5F, -3.0F, -3.5F, 5.0F, 5.0F, 7.0F),
                ModelTransform.pivot(0.0F, 19.0F, 0.0F));

        ModelPartData head = body.addChild("head", ModelPartBuilder.create()
                .uv(0, 12).cuboid(-2.0F, -4.0F, -2.5F, 4.0F, 4.0F, 4.0F),
                ModelTransform.pivot(0.0F, -2.5F, -2.5F));

        head.addChild("beak", ModelPartBuilder.create()
                .uv(16, 12).cuboid(-1.5F, -3.5F, -7.5F, 3.0F, 3.0F, 5.0F),
                ModelTransform.NONE);

        body.addChild("left_wing", ModelPartBuilder.create()
                .uv(0, 20).cuboid(0.0F, 0.0F, -2.5F, 1.0F, 4.0F, 6.0F),
                ModelTransform.pivot(2.5F, -2.5F, -0.5F));

        body.addChild("right_wing", ModelPartBuilder.create()
                .uv(14, 20).cuboid(-1.0F, 0.0F, -2.5F, 1.0F, 4.0F, 6.0F),
                ModelTransform.pivot(-2.5F, -2.5F, -0.5F));

        body.addChild("tail", ModelPartBuilder.create()
                .uv(24, 0).cuboid(-1.5F, -0.5F, 0.0F, 3.0F, 1.0F, 5.0F),
                ModelTransform.of(0.0F, -1.0F, 3.5F, -0.2618F, 0.0F, 0.0F));

        body.addChild("left_leg", ModelPartBuilder.create()
                .uv(24, 6).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                ModelTransform.pivot(1.25F, 2.0F, 0.0F));

        body.addChild("right_leg", ModelPartBuilder.create()
                .uv(28, 6).cuboid(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                ModelTransform.pivot(-1.25F, 2.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        this.head.pitch = headPitch * 0.017453292F;
        this.head.yaw = headYaw * 0.017453292F;

        if (!entity.isOnGround()) {
            float flap = MathHelper.cos(animationProgress * 1.4F) * 1.1F;
            this.leftWing.roll = -flap - 0.2F;
            this.rightWing.roll = flap + 0.2F;
            this.leftLeg.pitch = 0.4F;
            this.rightLeg.pitch = 0.4F;
        } else {
            this.leftWing.roll = 0.0F;
            this.rightWing.roll = 0.0F;
            this.leftLeg.pitch = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
            this.rightLeg.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 1.4F * limbDistance;
        }
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }
}
