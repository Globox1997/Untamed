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
import net.untamed.entity.OctopusEntity;

@Environment(EnvType.CLIENT)
public class OctopusModel<T extends OctopusEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart base;
    private final ModelPart head;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart leftArms;
    private final ModelPart arm0;
    private final ModelPart arm1;
    private final ModelPart arm2;
    private final ModelPart arm3;
    private final ModelPart rightArms;
    private final ModelPart arm4;
    private final ModelPart arm5;
    private final ModelPart arm6;
    private final ModelPart arm7;

    private ModelPart[] leftArmParts;
    private ModelPart[] rightArmParts;

    public OctopusModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.base = this.root.getChild("base");
        this.head = this.base.getChild("head");
        this.leftEye = this.head.getChild("leftEye");
        this.rightEye = this.head.getChild("rightEye");
        this.leftArms = this.root.getChild("leftArms");
        this.arm0 = this.leftArms.getChild("arm0");
        this.arm1 = this.leftArms.getChild("arm1");
        this.arm2 = this.leftArms.getChild("arm2");
        this.arm3 = this.leftArms.getChild("arm3");
        this.rightArms = this.root.getChild("rightArms");
        this.arm4 = this.rightArms.getChild("arm4");
        this.arm5 = this.rightArms.getChild("arm5");
        this.arm6 = this.rightArms.getChild("arm6");
        this.arm7 = this.rightArms.getChild("arm7");

        this.leftArmParts = new ModelPart[]{this.arm0, this.arm1, this.arm2, this.arm3};
        this.rightArmParts = new ModelPart[]{this.arm4, this.arm5, this.arm6, this.arm7};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition base = root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 42).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.005F))
                .texOffs(27, 51).addBox(-3.0F, -6.0F, -2.0F, 6.0F, 3.0F, 5.0F, new CubeDeformation(0.005F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition head = base.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, 0.0F));

        PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -14.25F, -5.0F, 10.0F, 13.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.48F, 0.0F, 0.0F));

        PartDefinition leftEye = head.addOrReplaceChild("leftEye", CubeListBuilder.create().texOffs(59, 57).addBox(-1.0F, -3.0F, -3.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, 0.0F, 0.0F));

        PartDefinition rightEye = head.addOrReplaceChild("rightEye", CubeListBuilder.create().texOffs(57, 36).addBox(-3.0F, -3.0F, -3.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, 0.0F, 0.0F));

        PartDefinition leftArms = root.addOrReplaceChild("leftArms", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition arm0 = leftArms.addOrReplaceChild("arm0", CubeListBuilder.create(), PartPose.offset(2.0F, -1.0F, -3.0F));

        PartDefinition cube_r2 = arm0.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(25, 59).addBox(-1.5F, -6.0F, -12.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(38, 29).addBox(-1.5F, -3.0F, -12.0F, 3.0F, 3.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

        PartDefinition arm1 = leftArms.addOrReplaceChild("arm1", CubeListBuilder.create(), PartPose.offset(2.0F, -1.0F, 3.0F));

        PartDefinition cube_r3 = arm1.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 59).addBox(-1.5F, -6.0F, 8.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(27, 10).addBox(-1.5F, -3.0F, -1.0F, 3.0F, 3.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        PartDefinition arm2 = leftArms.addOrReplaceChild("arm2", CubeListBuilder.create().texOffs(46, 12).addBox(2.0F, -2.0F, -3.0F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(56, 23).addBox(9.0F, -5.0F, -3.0F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -1.0F, 0.5F, 0.0F, 0.2182F, 0.0F));

        PartDefinition arm3 = leftArms.addOrReplaceChild("arm3", CubeListBuilder.create().texOffs(46, 6).addBox(2.0F, -2.0F, 0.0F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(13, 56).addBox(9.0F, -5.0F, 0.0F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, -1.0F, -0.5F, 0.0F, -0.2182F, 0.0F));

        PartDefinition rightArms = root.addOrReplaceChild("rightArms", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition arm4 = rightArms.addOrReplaceChild("arm4", CubeListBuilder.create(), PartPose.offset(-2.0F, -1.0F, -3.0F));

        PartDefinition cube_r4 = arm4.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(45, 57).addBox(-1.5F, -6.0F, -12.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(19, 26).addBox(-1.5F, -3.0F, -12.0F, 3.0F, 3.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        PartDefinition arm5 = rightArms.addOrReplaceChild("arm5", CubeListBuilder.create(), PartPose.offset(-2.0F, -1.0F, 3.0F));

        PartDefinition cube_r5 = arm5.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(57, 29).addBox(-1.5F, -6.0F, 8.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 23).addBox(-1.5F, -3.0F, -1.0F, 3.0F, 3.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 0.0F, 0.0F, -0.7854F, 0.0F));

        PartDefinition arm6 = rightArms.addOrReplaceChild("arm6", CubeListBuilder.create().texOffs(32, 45).addBox(-14.0F, -2.0F, -3.0F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 53).addBox(-14.0F, -5.0F, -3.0F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -1.0F, 0.5F, 0.0F, -0.2182F, 0.0F));

        PartDefinition arm7 = rightArms.addOrReplaceChild("arm7", CubeListBuilder.create().texOffs(30, 0).addBox(-14.0F, -2.0F, 0.0F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(49, 51).addBox(-14.0F, -5.0F, 0.0F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.0F, -1.0F, -0.5F, 0.0F, 0.2182F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        final float idleFrequency = 0.045F;
        final float idleAmplitude = 0.20F;

        final float swimFrequency = 0.6F;
        boolean crawling = entity.isCrawling();
        float swimStrength = crawling ? 0.0F : Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float crawlStrength = crawling ? Mth.clamp(limbSwingAmount, 0.0F, 1.0F) : 0.0F;

        for (int idx = 0; idx < this.leftArmParts.length; idx++) {
            float phase = idx * ((float) Math.PI / 3.5F);

            float idleWave = Mth.sin(ageInTicks * idleFrequency + phase) * idleAmplitude;
            float swimWave = Mth.sin(limbSwing * swimFrequency + phase) * swimStrength * 0.5F;
            float crawlWave = Mth.sin(limbSwing * 0.5F + phase) * crawlStrength * 0.35F * (idx % 2 == 0 ? 1.0F : -1.0F);
            float totalZRot = idleWave + swimWave + crawlWave;

            float sway = Mth.cos(ageInTicks * idleFrequency * 0.6F + phase) * 0.1F;

            ModelPart left = this.leftArmParts[idx];
            ModelPart right = this.rightArmParts[idx];

            left.zRot = totalZRot;
            left.yRot = sway;

            right.zRot = -totalZRot;
            right.yRot = -sway;
        }

        float jetPulse = Mth.sin(limbSwing * swimFrequency) * swimStrength;
        this.base.y = -jetPulse * 0.6F;

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.25F;
        this.head.xRot = headPitch * ((float) Math.PI / 180F) * 0.2F;
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
    public ModelPart root() {
        return this.root;
    }

}