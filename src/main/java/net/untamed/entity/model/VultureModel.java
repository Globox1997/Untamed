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
import net.untamed.entity.VultureEntity;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class VultureModel<T extends VultureEntity> extends HierarchicalModel<T> {

    private static final float WALK_CYCLE_FREQ = 0.6662F;
    private static final float WALK_CYCLE_AMP = 1.4F;

    private final ModelPart root;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart tail2;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;

    public VultureModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.leftWing = this.root.getChild("leftWing");
        this.rightWing = this.root.getChild("rightWing");
        this.body = this.root.getChild("body");
        this.tail = this.root.getChild("tail");
        this.tail2 = this.tail.getChild("tail2");
        this.neck = this.root.getChild("neck");
        this.head = this.neck.getChild("head");
        this.leftFoot = this.root.getChild("leftFoot");
        this.rightFoot = this.root.getChild("rightFoot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition leftWing = root.addOrReplaceChild("leftWing", CubeListBuilder.create(), PartPose.offset(4.0F, -10.0F, -1.5F));

        PartDefinition cube_r1 = leftWing.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(-9, 0).addBox(3.0F, 1.0F, -0.5F, 16.0F, 0.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(0, 9).addBox(3.0F, 0.0F, -0.5F, 7.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition rightWing = root.addOrReplaceChild("rightWing", CubeListBuilder.create(), PartPose.offset(-2.0F, -10.0F, -1.5F));

        PartDefinition cube_r2 = rightWing.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(-9, 0).mirror().addBox(-17.0F, 1.0F, -0.5F, 16.0F, 0.0F, 9.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(0, 9).mirror().addBox(-8.0F, 0.0F, -0.5F, 7.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(1.0F, -10.0F, -1.5F));

        PartDefinition cube_r3 = body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(36, 54).addBox(-4.0F, -2.0F, -3.5F, 8.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition cube_r4 = body.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(34, 35).addBox(-3.0F, -2.0F, -0.5F, 6.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F));

        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(1.0F, -7.0F, 5.5F));

        PartDefinition cube_r5 = tail.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(36, 23).addBox(-2.0F, -1.0F, 6.5F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -3.0F, -7.0F, -0.3927F, 0.0F, 0.0F));

        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 0.0F, -0.5F, 8.0F, 0.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, 2.0F));

        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create(), PartPose.offset(1.0F, -10.1506F, -4.5533F));

        PartDefinition cube_r6 = neck.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(51, 15).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.1263F, -0.5958F, 0.7854F, 0.0F, 0.0F));

        PartDefinition cube_r7 = neck.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(39, 13).addBox(-1.0F, -2.5F, -2.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.3927F, 0.0F, 0.0F));

        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(14, 18).mirror().addBox(-1.5F, -4.0F, -4.5F, 3.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(35, 1).addBox(-0.5F, -3.0F, -7.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 12).addBox(0.0F, -2.0F, -8.5F, 0.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.8494F, -0.9467F));

        PartDefinition leftFoot = root.addOrReplaceChild("leftFoot", CubeListBuilder.create(), PartPose.offset(3.0F, -5.8352F, 4.2263F));

        PartDefinition cube_r8 = leftFoot.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(6, 33).addBox(0.998F, -2.0F, 0.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, 2.0F, 1.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition cube_r9 = leftFoot.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(19, 34).addBox(0.9999F, 1.2912F, -4.7071F, 5.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, 2.0F, 2.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition rightFoot = root.addOrReplaceChild("rightFoot", CubeListBuilder.create(), PartPose.offset(-1.0F, -5.8352F, 4.2263F));

        PartDefinition cube_r10 = rightFoot.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(19, 34).addBox(-2.4999F, 1.2922F, -4.7071F, 5.0F, 0.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 2.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition cube_r11 = rightFoot.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(6, 33).addBox(-2.999F, -2.0F, 0.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 2.0F, 1.0F, 0.7854F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float headYaw = netHeadYaw * ((float) Math.PI / 180.0F);
        float headPitchRad = Mth.clamp(headPitch * ((float) Math.PI / 180.0F), -0.6F, 0.6F);
        this.resetPose();
        this.head.yRot = headYaw;
        int state = entity.getFlightState();

        if (entity.isPerched()) this.posePerched(ageInTicks);
        else if (state == VultureEntity.FLIGHT_GLIDE) this.poseGlide(ageInTicks, headPitchRad);
        else if (state == VultureEntity.FLIGHT_THERMAL) this.poseThermal(headPitchRad);
        else if (state == VultureEntity.FLIGHT_TAKEOFF) this.poseFlap(ageInTicks, headPitchRad, 1.1F, 0.55F);
        else if (state == VultureEntity.FLIGHT_DESCEND) this.poseFlap(ageInTicks, headPitchRad, 0.6F, 0.45F);
        else this.poseGround(limbSwing, limbSwingAmount, headPitchRad);
    }

    private void resetPose() {
        this.root.zRot = 0.0F;
        this.head.xRot = 0.0F;
        this.leftWing.xRot = 0.0F; this.leftWing.yRot = 0.0F; this.leftWing.zRot = 0.0F;
        this.rightWing.xRot = 0.0F; this.rightWing.yRot = 0.0F; this.rightWing.zRot = 0.0F;
        this.leftFoot.xRot = 0.0F; this.rightFoot.xRot = 0.0F;
    }

    private void poseFlap(float ageInTicks, float headPitchRad, float flapSpeed, float flapAmp) {
        float flap = Mth.sin(ageInTicks * flapSpeed) * flapAmp;
        this.leftWing.zRot = -0.0873F - flap;
        this.rightWing.zRot = 0.0873F + flap;
        this.head.xRot = headPitchRad;
        this.leftFoot.xRot = 0.4F;
        this.rightFoot.xRot = 0.4F;
    }

    private void poseGlide(float ageInTicks, float headPitchRad) {
        float wobble = Mth.cos(ageInTicks * 0.05F) * 0.03F;
        this.leftWing.zRot = -0.0873F + wobble;
        this.rightWing.zRot = 0.0873F - wobble;
        this.head.xRot = headPitchRad;
        this.leftFoot.xRot = 0.4F;
        this.rightFoot.xRot = 0.4F;
    }

    private void poseThermal(float headPitchRad) {
        this.root.zRot = 0.35F;
        this.leftWing.zRot = -0.0873F;
        this.rightWing.zRot = 0.0873F;
        this.head.xRot = headPitchRad;
        this.leftFoot.xRot = 0.4F;
        this.rightFoot.xRot = 0.4F;
    }

    private void poseGround(float limbSwing, float limbSwingAmount, float headPitchRad) {
        this.head.xRot = headPitchRad;
        this.leftWing.xRot = -0.1745F; this.leftWing.yRot = -0.8727F; this.leftWing.zRot = 1.3963F;
        this.rightWing.xRot = -0.0873F; this.rightWing.yRot = 0.8727F; this.rightWing.zRot = -1.3963F;
        this.leftFoot.xRot = -0.7854F + Mth.cos(limbSwing * WALK_CYCLE_FREQ) * WALK_CYCLE_AMP * limbSwingAmount;
        this.rightFoot.xRot = -0.7854F + Mth.cos(limbSwing * WALK_CYCLE_FREQ + (float) Math.PI) * WALK_CYCLE_AMP * limbSwingAmount;
    }

    private void posePerched(float ageInTicks) {
        this.leftWing.xRot = -0.1745F; this.leftWing.yRot = -0.8727F; this.leftWing.zRot = 1.3963F;
        this.rightWing.xRot = -0.0873F; this.rightWing.yRot = 0.8727F; this.rightWing.zRot = -1.3963F;
        this.head.xRot = 0.5F + Mth.cos(ageInTicks * 0.03F) * 0.02F;
        this.leftFoot.xRot = -0.7854F;
        this.rightFoot.xRot = -0.7854F;
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
        return this.root;
    }

}
