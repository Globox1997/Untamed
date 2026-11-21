package net.untamed.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.untamed.entity.QuetzalEntity;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class QuetzalModel<T extends QuetzalEntity> extends HierarchicalModel<T> {

    private static final float TAIL_HANG = -1.25F;
    private static final float TAIL_FLIGHT = -1.45F;
    private static final float TAIL_HOVER = -1.0F;
    private static final float BODY_FLIGHT = 1.35F;
    private static final float BODY_HOVER = 0.6F;
    private static final float WING_SPREAD = 1.45F;
    private static final float WING_FLAP = 0.7F;
    private static final float WING_FLIGHT_TWIST = (float) (Math.PI / 2.0);
    private static final float SLEEP_HEAD_YAW = 2.6F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftWingBlade;
    private final ModelPart rightWingBlade;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart[] plumes;

    private float flyAmount;
    private float hoverAmount;
    private float sleepAmount;
    private float frozenAmount;
    private boolean longPlumes;

    public QuetzalModel(ModelPart root) {
        super();
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.tail = this.body.getChild("tail");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.leftWingBlade = this.leftWing.getChild("left_wing_blade");
        this.rightWingBlade = this.rightWing.getChild("right_wing_blade");
        this.leftLeg = this.body.getChild("left_leg");
        this.rightLeg = this.body.getChild("right_leg");
        ModelPart plume = this.tail.getChild("plume");
        ModelPart plume2 = plume.getChild("plume2");
        ModelPart plume3 = plume2.getChild("plume3");
        this.plumes = new ModelPart[]{plume, plume2, plume3};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 5.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.5F, 14.0F, 0.0F));

        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(50, 0).addBox(-1.5F, -3.0F, -2.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(36, 26).addBox(-2.0F, -4.0F, 0.0F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.2F))
                .texOffs(18, 5).addBox(-1.0F, -2.0F, -4.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 21).addBox(0.0F, -2.0F, -6.0F, 0.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 0.0F, 0.0F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 11).addBox(-2.5F, -0.5F, 0.0F, 5.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(0, 21).addBox(-2.5F, -0.5F, 0.0F, 5.0F, 1.0F, 8.0F, new CubeDeformation(0.2F)), PartPose.offsetAndRotation(0.5F, 5.5F, 2.0F, TAIL_HANG, 0.0F, 0.0F));

        PartDefinition plume = tail.addOrReplaceChild("plume", CubeListBuilder.create().texOffs(22, 0).addBox(-2.5F, 0.0F, 0.0F, 5.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 4.0F));

        PartDefinition plume2 = plume.addOrReplaceChild("plume2", CubeListBuilder.create().texOffs(23, 5).addBox(-2.5F, 0.0F, 0.0F, 5.0F, 0.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 5.0F));

        plume2.addOrReplaceChild("plume3", CubeListBuilder.create().texOffs(23, 5).addBox(-2.5F, 0.0F, 0.0F, 5.0F, 0.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 4.0F));

        PartDefinition leftWing = body.addOrReplaceChild("left_wing", CubeListBuilder.create(), PartPose.offset(3.0F, 1.0F, 1.0F));

        leftWing.addOrReplaceChild("left_wing_blade", CubeListBuilder.create().texOffs(26, 21).addBox(0.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(18, 21).addBox(0.0F, 5.0F, -1.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.ZERO);

        PartDefinition rightWing = body.addOrReplaceChild("right_wing", CubeListBuilder.create(), PartPose.offset(-2.0F, 1.0F, 1.0F));

        rightWing.addOrReplaceChild("right_wing_blade", CubeListBuilder.create().texOffs(28, 9).addBox(-1.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(18, 0).addBox(-1.0F, 5.0F, -1.0F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.ZERO);

        body.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(36, 18).addBox(-1.5F, -0.1F, -3.0F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.01F)), PartPose.offset(2.5F, 5.0F, 2.0F));

        body.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(38, 9).addBox(-1.5F, -0.1F, -3.0F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.01F)), PartPose.offset(-1.5F, 5.0F, 2.0F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.flyAmount = entity.getFlyAmount(partialTick);
        this.hoverAmount = entity.getHoverAmount(partialTick);
        this.sleepAmount = entity.getSleepAmount(partialTick);
        this.frozenAmount = entity.getFrozenAmount(partialTick);
        this.longPlumes = entity.hasPlumes();
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float headPitchRad = Mth.clamp(headPitch * ((float) Math.PI / 180.0F), -0.6F, 0.6F);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.head.xRot = headPitchRad;
        this.plumes[1].visible = this.longPlumes;
        this.plumes[2].visible = this.longPlumes;
        this.body.xScale = 1.0F;
        this.body.zScale = 1.0F;

        float calm = 1.0F - this.frozenAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 1.2F) * 0.8F * limbSwingAmount;
        this.rightLeg.xRot = Mth.cos(limbSwing * 1.2F + (float) Math.PI) * 0.8F * limbSwingAmount;
        this.tail.xRot = TAIL_HANG + Mth.sin(ageInTicks * 0.08F) * 0.03F * calm;
        for (int k = 0; k < this.plumes.length; k++) {
            this.plumes[k].xRot = 0.08F * k + Mth.sin(ageInTicks * 0.05F - k) * 0.05F * calm;
            this.plumes[k].yRot = Mth.sin(ageInTicks * 0.04F - k * 0.5F) * 0.08F * calm;
        }

        float fly = Math.max(this.flyAmount, this.hoverAmount);
        if (fly > 0.0F) {
            float bodyPitch = Mth.lerp(this.hoverAmount, BODY_FLIGHT, BODY_HOVER);
            float flapSpeed = this.hoverAmount > 0.5F ? 2.2F : 1.3F;
            float flap = Mth.sin(ageInTicks * flapSpeed) * WING_FLAP;
            this.body.xRot = Mth.lerp(fly, this.body.xRot, bodyPitch);
            this.head.xRot = Mth.lerp(fly, this.head.xRot, -bodyPitch + headPitchRad * 0.5F);
            this.rightWing.zRot = Mth.lerp(fly, this.rightWing.zRot, WING_SPREAD);
            this.leftWing.zRot = Mth.lerp(fly, this.leftWing.zRot, -WING_SPREAD);
            this.rightWing.xRot = flap * fly;
            this.leftWing.xRot = flap * fly;
            this.rightWingBlade.yRot = WING_FLIGHT_TWIST * fly;
            this.leftWingBlade.yRot = -WING_FLIGHT_TWIST * fly;
            this.tail.xRot = Mth.lerp(fly, this.tail.xRot, Mth.lerp(this.hoverAmount, TAIL_FLIGHT, TAIL_HOVER));
            this.leftLeg.xRot = Mth.lerp(fly, this.leftLeg.xRot, -0.3F);
            this.rightLeg.xRot = Mth.lerp(fly, this.rightLeg.xRot, -0.3F);
            for (int k = 0; k < this.plumes.length; k++) {
                this.plumes[k].xRot = Mth.lerp(fly, this.plumes[k].xRot, Mth.sin(ageInTicks * 0.4F - k * 0.9F) * 0.25F);
            }
        }
        if (this.sleepAmount > 0.0F) {
            float s = this.sleepAmount;
            this.head.yRot = Mth.lerp(s, this.head.yRot, SLEEP_HEAD_YAW);
            this.head.xRot = Mth.lerp(s, this.head.xRot, 0.3F);
            float fluff = 1.0F + 0.08F * s;
            this.body.xScale = fluff;
            this.body.zScale = fluff;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k) {
        if (this.young) {
            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 0.5f);
            poseStack.translate(0.0F, 1.5F, 0.0F);
            super.renderToBuffer(poseStack, vertexConsumer, i, j, k);
            poseStack.popPose();
        } else {
            super.renderToBuffer(poseStack, vertexConsumer, i, j, k);
        }
    }

    @Override
    public @NotNull ModelPart root() {
        return this.body;
    }
}
