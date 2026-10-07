package net.untamed.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.untamed.entity.OctopusEntity;

@Environment(EnvType.CLIENT)
public class OctopusModel<T extends OctopusEntity> extends HierarchicalModel<T> {

    private static final float RING_RADIUS = 2.5F;
    private static final String[] ARM_NAMES = {"arm0", "arm2", "arm3", "arm1", "arm5", "arm7", "arm6", "arm4"};
    private static final float[] NATIVE_DIRECTIONS = {0.0F, 90.0F, 90.0F, 180.0F, 180.0F, -90.0F, -90.0F, 0.0F};
    private static final float[] DIRECTIONS = {22.5F, 67.5F, 112.5F, 157.5F, -157.5F, -112.5F, -67.5F, -22.5F};

    private final ModelPart root;
    private final ModelPart base;
    private final ModelPart head;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart[] slots = new ModelPart[ARM_NAMES.length];
    private final ModelPart[] arms = new ModelPart[ARM_NAMES.length];

    private float restAmount;
    private float jetAmount;
    private float threatAmount;
    private float pounceAmount;
    private float reachAmount;
    private float stalkAmount;
    private int tint = -1;

    public OctopusModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.base = this.root.getChild("base");
        this.head = this.base.getChild("head");
        this.leftEye = this.head.getChild("leftEye");
        this.rightEye = this.head.getChild("rightEye");
        for (int i = 0; i < ARM_NAMES.length; i++) {
            this.slots[i] = this.root.getChild(ARM_NAMES[i]);
            this.arms[i] = this.slots[i].getChild("segment");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition base = root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 42).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.005F))
                .texOffs(27, 51).addBox(-3.0F, -6.0F, -2.0F, 6.0F, 3.0F, 5.0F, new CubeDeformation(0.005F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition head = base.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, 0.0F));

        head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -14.25F, -5.0F, 10.0F, 13.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.48F, 0.0F, 0.0F));

        head.addOrReplaceChild("leftEye", CubeListBuilder.create().texOffs(59, 57).addBox(-1.0F, -3.0F, -3.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, 0.0F, 0.0F));

        head.addOrReplaceChild("rightEye", CubeListBuilder.create().texOffs(57, 36).addBox(-3.0F, -3.0F, -3.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, 0.0F, 0.0F));

        addArm(root, 0, CubeListBuilder.create().texOffs(38, 29).addBox(-1.5F, -2.0F, -12.0F, 3.0F, 3.0F, 13.0F, new CubeDeformation(0.0F))
                .texOffs(25, 59).addBox(-1.5F, -5.0F, -12.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)));
        addArm(root, 1, CubeListBuilder.create().texOffs(46, 12).addBox(-1.0F, -2.0F, -1.5F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(56, 23).addBox(6.0F, -5.0F, -1.5F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)));
        addArm(root, 2, CubeListBuilder.create().texOffs(46, 6).addBox(-1.0F, -2.0F, -1.5F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(13, 56).addBox(6.0F, -5.0F, -1.5F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)));
        addArm(root, 3, CubeListBuilder.create().texOffs(27, 10).addBox(-1.5F, -2.0F, -1.0F, 3.0F, 3.0F, 13.0F, new CubeDeformation(0.0F))
                .texOffs(0, 59).addBox(-1.5F, -5.0F, 8.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)));
        addArm(root, 4, CubeListBuilder.create().texOffs(0, 23).addBox(-1.5F, -2.0F, -1.0F, 3.0F, 3.0F, 13.0F, new CubeDeformation(0.0F))
                .texOffs(57, 29).addBox(-1.5F, -5.0F, 8.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)));
        addArm(root, 5, CubeListBuilder.create().texOffs(30, 0).addBox(-11.0F, -2.0F, -1.5F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(49, 51).addBox(-11.0F, -5.0F, -1.5F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)));
        addArm(root, 6, CubeListBuilder.create().texOffs(32, 45).addBox(-11.0F, -2.0F, -1.5F, 12.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 53).addBox(-11.0F, -5.0F, -1.5F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)));
        addArm(root, 7, CubeListBuilder.create().texOffs(19, 26).addBox(-1.5F, -2.0F, -12.0F, 3.0F, 3.0F, 13.0F, new CubeDeformation(0.0F))
                .texOffs(45, 57).addBox(-1.5F, -5.0F, -12.0F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    private static void addArm(PartDefinition root, int index, CubeListBuilder geometry) {
        float direction = DIRECTIONS[index] * Mth.DEG_TO_RAD;
        PartDefinition slot = root.addOrReplaceChild(ARM_NAMES[index], CubeListBuilder.create(),
                PartPose.offsetAndRotation(Mth.sin(direction) * RING_RADIUS, -1.0F, -Mth.cos(direction) * RING_RADIUS, 0.0F, yawFor(index, DIRECTIONS[index]), 0.0F));
        slot.addOrReplaceChild("segment", geometry, PartPose.ZERO);
    }

    private static float yawFor(int index, float direction) {
        return (NATIVE_DIRECTIONS[index] - direction) * Mth.DEG_TO_RAD;
    }

    @Override
    public void prepareMobModel(T entity, float f, float g, float partialTick) {
        this.restAmount = entity.getRestAmount(partialTick);
        this.jetAmount = entity.getJetAmount(partialTick);
        this.threatAmount = entity.getThreatAmount(partialTick);
        this.pounceAmount = entity.getPounceAmount(partialTick);
        this.reachAmount = entity.getReachAmount(partialTick);
        this.stalkAmount = entity.getStalkAmount(partialTick);
        float threat = this.threatAmount;
        this.tint = FastColor.ARGB32.colorFromFloat(1.0F, Mth.lerp(threat, 1.0F, 0.55F), Mth.lerp(threat, 1.0F, 0.2F), Mth.lerp(threat, 1.0F, 0.15F));
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        boolean crawling = entity.isCrawling();
        float swimStrength = crawling ? 0.0F : Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float crawlStrength = crawling ? Mth.clamp(limbSwingAmount, 0.0F, 1.0F) : 0.0F;
        float calm = 1.0F - Math.max(this.restAmount, this.stalkAmount * 0.7F);

        for (int i = 0; i < this.arms.length; i++) {
            float phase = i * Mth.TWO_PI / this.arms.length;
            float lift = Mth.sin(ageInTicks * 0.045F + phase) * 0.15F * calm
                    + Mth.sin(limbSwing * 0.6F + phase) * swimStrength * 0.45F
                    + Math.max(0.0F, Mth.sin(limbSwing * 0.5F + phase)) * crawlStrength * 0.35F;
            float sway = Mth.cos(ageInTicks * 0.03F + phase) * 0.08F * calm;
            float direction = DIRECTIONS[i];

            lift = Mth.lerp(this.restAmount, lift, -0.05F);
            lift = Mth.lerp(this.stalkAmount, lift, 0.0F);
            lift = Mth.lerp(this.threatAmount, lift, -0.1F);
            sway = Mth.lerp(this.threatAmount, sway, 0.0F);

            float trailing = Math.signum(direction) * (180.0F - (180.0F - Math.abs(direction)) * 0.15F);
            direction = Mth.lerp(this.jetAmount, direction, trailing);
            lift = Mth.lerp(this.jetAmount, lift, 0.1F + Mth.sin(ageInTicks * 0.8F + phase) * 0.05F);
            direction = Mth.lerp(this.pounceAmount, direction, direction * 0.5F);
            lift = Mth.lerp(this.pounceAmount, lift, 0.6F);
            if (i == 0) {
                direction = Mth.lerp(this.reachAmount, direction, 0.0F);
                lift = Mth.lerp(this.reachAmount, lift, 0.35F + Mth.sin(ageInTicks * 0.5F) * 0.15F);
            }

            this.slots[i].yRot = yawFor(i, direction);
            this.applyLift(i, lift);
            this.arms[i].yRot = sway;
        }

        float breath = 1.0F + Mth.sin(ageInTicks * 0.1F) * 0.03F;
        float puff = 1.0F + 0.12F * this.threatAmount;
        float squeeze = 1.0F - 0.08F * Math.max(0.0F, Mth.sin(ageInTicks * 0.8F)) * this.jetAmount;
        this.head.xScale = breath * puff * squeeze;
        this.head.zScale = breath * puff * squeeze;
        this.head.yScale = breath * puff;

        float jetPulse = Mth.sin(limbSwing * 0.6F) * swimStrength;
        this.base.y = -jetPulse * 0.6F;

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.25F;
        this.head.xRot = headPitch * ((float) Math.PI / 180F) * 0.2F + 0.3F * this.restAmount;
        this.leftEye.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.3F;
        this.rightEye.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.3F;
    }

    private void applyLift(int index, float lift) {
        float nativeDirection = NATIVE_DIRECTIONS[index];
        ModelPart arm = this.arms[index];
        if (nativeDirection == 0.0F) {
            arm.xRot = -lift;
        } else if (nativeDirection == 180.0F) {
            arm.xRot = lift;
        } else if (nativeDirection == 90.0F) {
            arm.zRot = -lift;
        } else {
            arm.zRot = lift;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k) {
        this.renderScaled(poseStack, vertexConsumer, i, j, FastColor.ARGB32.multiply(k, this.tint));
    }

    public void renderOverlay(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        this.renderScaled(poseStack, vertexConsumer, light, overlay, color);
    }

    private void renderScaled(PoseStack poseStack, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        if (this.young) {
            poseStack.pushPose();
            poseStack.scale(0.5f, 0.5f, 0.5f);
            poseStack.translate(0.0F, 1.5F, 0.0F);
            super.renderToBuffer(poseStack, vertexConsumer, light, overlay, color);
            poseStack.popPose();
        } else {
            super.renderToBuffer(poseStack, vertexConsumer, light, overlay, color);
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

}
