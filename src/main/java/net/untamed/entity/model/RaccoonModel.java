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
import net.untamed.entity.RaccoonEntity;

@Environment(EnvType.CLIENT)
public class RaccoonModel<T extends RaccoonEntity> extends HierarchicalModel<T> {

    private static final float TAIL_BASE_PITCH = -0.4F;
    private static final float TAIL_MID_PITCH = -0.15F;
    private static final float TAIL_TIP_PITCH = 0.2F;
    private static final float[] REST_TAIL_YAW = {0.8F, 0.8F, 0.8F};
    private static final float HISS_TAIL_PUFF = 0.25F;
    private static final float SIT_BODY_PITCH = -1.2F;
    private static final float SIT_MAX_HEAD_YAW = 35.0F * Mth.DEG_TO_RAD;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart snoot;
    private final ModelPart tail;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart[] tailSegments;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;

    private float restAmount;
    private float douseAmount;
    private float sitAmount;
    private float hissAmount;
    private float forageAmount;
    private int restVariant;

    public RaccoonModel(ModelPart root) {
        super();
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftEar = this.head.getChild("leftEar");
        this.rightEar = this.head.getChild("rightEar");
        this.snoot = this.head.getChild("snoot");
        this.tail = this.body.getChild("tail");
        this.tail2 = this.tail.getChild("tail2");
        this.tail3 = this.tail2.getChild("tail3");
        this.tailSegments = new ModelPart[]{this.tail, this.tail2, this.tail3};
        this.leftFrontLeg = this.root.getChild("leftFrontLeg");
        this.rightFrontLeg = this.root.getChild("rightFrontLeg");
        this.leftBackLeg = this.root.getChild("leftBackLeg");
        this.rightBackLeg = this.root.getChild("rightBackLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(1, 13).addBox(-3.0F, -2.5F, -5.5F, 6.0F, 6.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.5F, -0.5F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(40, 8).addBox(-6.0F, 0.0F, -4.0F, 12.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(1, 1).addBox(-4.0F, -4.0F, -5.0F, 8.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -5.5F));

        PartDefinition leftEar = head.addOrReplaceChild("leftEar", CubeListBuilder.create().texOffs(28, 4).addBox(-1.5F, -2.0F, -1.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, -4.0F, -1.0F));

        PartDefinition rightEar = head.addOrReplaceChild("rightEar", CubeListBuilder.create().texOffs(28, 4).mirror().addBox(-1.5F, -2.0F, -1.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.5F, -4.0F, -1.0F));

        PartDefinition snoot = head.addOrReplaceChild("snoot", CubeListBuilder.create().texOffs(28, 8).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 1.0F, -5.0F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(1, 31).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.5F, 5.0F, TAIL_BASE_PITCH, 0.0F, 0.0F));

        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(16, 31).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(-0.01F)), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, TAIL_MID_PITCH, 0.0F, 0.0F));

        tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(1, 39).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, TAIL_TIP_PITCH, 0.0F, 0.0F));

        PartDefinition leftFrontLeg = root.addOrReplaceChild("leftFrontLeg", CubeListBuilder.create().texOffs(36, 15).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, -4.0F, -4.0F));

        PartDefinition rightFrontLeg = root.addOrReplaceChild("rightFrontLeg", CubeListBuilder.create().texOffs(36, 15).mirror().addBox(-1.0F, -1.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-1.5F, -4.0F, -4.0F));

        PartDefinition leftBackLeg = root.addOrReplaceChild("leftBackLeg", CubeListBuilder.create().texOffs(36, 23).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, -4.0F, 3.0F));

        PartDefinition rightBackLeg = root.addOrReplaceChild("rightBackLeg", CubeListBuilder.create().texOffs(36, 23).mirror().addBox(-1.0F, -1.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-1.5F, -4.0F, 3.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void prepareMobModel(T entity, float f, float g, float partialTick) {
        this.restAmount = entity.getRestAmount(partialTick);
        this.douseAmount = entity.getDouseAmount(partialTick);
        this.sitAmount = entity.getSitAmount(partialTick);
        this.hissAmount = entity.getHissAmount(partialTick);
        this.forageAmount = entity.getForageAmount(partialTick);
        this.restVariant = entity.getRestVariant();
    }

    @Override
    public void setupAnim(T entity, float f, float g, float h, float i, float j) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.head.xRot = j * (float) (Math.PI / 180.0);
        this.head.yRot = i * (float) (Math.PI / 180.0);

        this.rightBackLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
        this.leftBackLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.rightFrontLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.leftFrontLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
        this.body.xRot = 0.06F;
        this.body.zRot = Mth.sin(f * 0.6662F) * 0.07F * g;
        this.body.y += Math.abs(Mth.cos(f * 0.6662F)) * 0.4F * g;
        for (int k = 0; k < this.tailSegments.length; k++) {
            ModelPart segment = this.tailSegments[k];
            segment.yRot = Mth.sin(f * 0.6662F - k * 0.6F) * 0.25F * g + Mth.sin(h * 0.08F - k * 0.5F) * 0.05F;
            segment.xScale = 1.0F;
            segment.yScale = 1.0F;
        }

        if (this.forageAmount > 0.0F) {
            this.poseForaging(h);
        }
        if (this.douseAmount > 0.0F) {
            this.poseDousing(h);
        }
        if (this.sitAmount > 0.0F) {
            this.poseSittingUp();
        }
        if (this.hissAmount > 0.0F) {
            this.poseHissing(h);
        }
        if (this.restAmount > 0.0F) {
            this.poseResting();
        }
    }

    private void poseForaging(float h) {
        float s = this.forageAmount;
        this.body.xRot = Mth.lerp(s, this.body.xRot, 0.2F);
        this.head.xRot = Mth.lerp(s, this.head.xRot, 0.8F);
        this.head.yRot = Mth.lerp(s, this.head.yRot, 0.0F);
        this.rightFrontLeg.xRot = Mth.lerp(s, this.rightFrontLeg.xRot, -0.6F + Mth.sin(h * 0.6F) * 0.4F);
        this.leftFrontLeg.xRot = Mth.lerp(s, this.leftFrontLeg.xRot, -0.6F - Mth.sin(h * 0.6F) * 0.4F);
    }

    private void poseDousing(float h) {
        float s = this.douseAmount;
        this.body.xRot = Mth.lerp(s, this.body.xRot, -0.5F);
        this.body.y += 1.0F * s;
        this.head.xRot = Mth.lerp(s, this.head.xRot, 1.0F);
        this.head.yRot = Mth.lerp(s, this.head.yRot, 0.0F);
        this.rightFrontLeg.y -= 2.0F * s;
        this.leftFrontLeg.y -= 2.0F * s;
        this.rightFrontLeg.xRot = Mth.lerp(s, this.rightFrontLeg.xRot, -0.9F + Mth.sin(h * 0.9F) * 0.3F);
        this.leftFrontLeg.xRot = Mth.lerp(s, this.leftFrontLeg.xRot, -0.9F - Mth.sin(h * 0.9F) * 0.3F);
        this.rightFrontLeg.zRot = Mth.lerp(s, this.rightFrontLeg.zRot, -0.25F);
        this.leftFrontLeg.zRot = Mth.lerp(s, this.leftFrontLeg.zRot, 0.25F);
    }

    private void poseSittingUp() {
        float s = this.sitAmount;
        this.body.xRot = Mth.lerp(s, this.body.xRot, SIT_BODY_PITCH);
        this.head.xRot += -SIT_BODY_PITCH * s;
        float tilt = -SIT_BODY_PITCH * s;
        float yaw = Mth.clamp(this.head.yRot, -SIT_MAX_HEAD_YAW, SIT_MAX_HEAD_YAW);
        this.head.yRot = Mth.lerp(s, this.head.yRot, yaw * Mth.cos(tilt));
        this.head.zRot = Mth.lerp(s, this.head.zRot, yaw * Mth.sin(tilt));
        this.rightFrontLeg.y -= 6.0F * s;
        this.leftFrontLeg.y -= 6.0F * s;
        this.rightFrontLeg.z += 2.5F * s;
        this.leftFrontLeg.z += 2.5F * s;
        this.rightFrontLeg.xRot = Mth.lerp(s, this.rightFrontLeg.xRot, -0.6F);
        this.leftFrontLeg.xRot = Mth.lerp(s, this.leftFrontLeg.xRot, -0.6F);
        this.rightFrontLeg.zRot = Mth.lerp(s, this.rightFrontLeg.zRot, -0.2F);
        this.leftFrontLeg.zRot = Mth.lerp(s, this.leftFrontLeg.zRot, 0.2F);
        this.rightBackLeg.xRot = Mth.lerp(s, this.rightBackLeg.xRot, -1.3F);
        this.leftBackLeg.xRot = Mth.lerp(s, this.leftBackLeg.xRot, -1.3F);
        this.tail.xRot = Mth.lerp(s, this.tail.xRot, 1.0F);
    }

    private void poseHissing(float h) {
        float s = this.hissAmount;
        this.body.xRot = Mth.lerp(s, this.body.xRot, 0.25F);
        this.head.xRot = Mth.lerp(s, this.head.xRot, 0.3F);
        this.head.y += 1.0F * s;
        this.leftEar.xRot -= 0.7F * s;
        this.rightEar.xRot -= 0.7F * s;
        float puff = 1.0F + HISS_TAIL_PUFF * s;
        for (ModelPart segment : this.tailSegments) {
            segment.xRot += 0.3F * s;
            segment.yRot += Mth.sin(h * 0.9F) * 0.15F * s;
            segment.xScale = puff;
            segment.yScale = puff;
        }
    }

    private void poseResting() {
        float r = this.restAmount;
        float side = this.restVariant == 0 ? 1.0F : -1.0F;
        this.body.y += 3.0F * r;
        this.body.xRot = Mth.lerp(r, this.body.xRot, 0.0F);
        this.body.zRot = Mth.lerp(r, this.body.zRot, 0.15F * side);
        this.head.xRot = Mth.lerp(r, this.head.xRot, 0.3F);
        this.head.yRot = Mth.lerp(r, this.head.yRot, 0.9F * side);
        this.head.y += 1.0F * r;
        for (ModelPart leg : new ModelPart[]{this.leftFrontLeg, this.rightFrontLeg, this.leftBackLeg, this.rightBackLeg}) {
            leg.y += 2.5F * r;
            leg.xRot = Mth.lerp(r, leg.xRot, -1.4F);
        }
        for (int k = 0; k < this.tailSegments.length; k++) {
            ModelPart segment = this.tailSegments[k];
            segment.xRot = Mth.lerp(r, segment.xRot, k == 0 ? -0.6F : 0.0F);
            segment.yRot = Mth.lerp(r, segment.yRot, REST_TAIL_YAW[k] * side);
        }
    }

    public void translateToHead(PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);
        this.head.translateAndRotate(poseStack);
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
