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
import net.untamed.entity.BlackPantherEntity;

@Environment(EnvType.CLIENT)
public class BlackPantherModel<T extends BlackPantherEntity> extends HierarchicalModel<T> {

    private static final float[] TAIL_CURL = {-1.0F, 0.15F, 0.35F, 0.55F};

    private final ModelPart root;
    private final ModelPart waist;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart tailTip;
    private final ModelPart leftLegFront;
    private final ModelPart rightLegFront;
    private final ModelPart leftLegBack;
    private final ModelPart rightLegBack;
    private final ModelPart[] tailSegments;

    private float stalkAmount;
    private float pounceAmount;
    private float restAmount;
    private float snarlAmount;
    private float fishAmount;

    public BlackPantherModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.waist = this.root.getChild("waist");
        this.body = this.waist.getChild("body");
        this.head = this.body.getChild("head");
        this.leftEar = this.head.getChild("leftEar");
        this.rightEar = this.head.getChild("rightEar");
        this.tail = this.body.getChild("tail");
        this.tail2 = this.tail.getChild("tail2");
        this.tail3 = this.tail2.getChild("tail3");
        this.tailTip = this.tail3.getChild("tailTip");
        this.leftLegFront = this.waist.getChild("leftLegFront");
        this.rightLegFront = this.waist.getChild("rightLegFront");
        this.leftLegBack = this.waist.getChild("leftLegBack");
        this.rightLegBack = this.waist.getChild("rightLegBack");
        this.tailSegments = new ModelPart[]{this.tail, this.tail2, this.tail3, this.tailTip};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offset(0.0F, -9.0F, 0.0F));

        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create().texOffs(1, 33).addBox(-6.0F, -7.0F, -12.5F, 12.0F, 12.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.0F, -2.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(1, 1).addBox(-5.0F, -7.0F, -8.5F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(1, 22).addBox(-3.5F, -2.0F, -11.5F, 7.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(22, 23).addBox(-8.0F, -3.5F, -10.5F, 16.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, -12.5F));

        head.addOrReplaceChild("rightEar", CubeListBuilder.create().texOffs(42, 14).addBox(-1.0F, -2.0F, -2.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -7.0F, -1.0F));

        head.addOrReplaceChild("leftEar", CubeListBuilder.create().texOffs(42, 14).mirror().addBox(-1.0F, -2.0F, -2.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.0F, -7.0F, -1.0F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(64, 0).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 8.0F, new CubeDeformation(0.05F)), PartPose.offset(0.0F, -4.0F, 13.0F));

        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(64, 14).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 8.0F));

        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(64, 26).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 7.0F));

        tail3.addOrReplaceChild("tailTip", CubeListBuilder.create().texOffs(64, 37).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 6.0F));

        waist.addOrReplaceChild("leftLegFront", CubeListBuilder.create().texOffs(1, 72).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.5F, -6.0F, -11.0F));

        waist.addOrReplaceChild("rightLegFront", CubeListBuilder.create().texOffs(1, 72).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.5F, -6.0F, -11.0F));

        waist.addOrReplaceChild("leftLegBack", CubeListBuilder.create().texOffs(18, 72).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 17.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.5F, -8.0F, 7.9F));

        waist.addOrReplaceChild("rightLegBack", CubeListBuilder.create().texOffs(18, 72).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 17.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.5F, -8.0F, 7.9F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void prepareMobModel(T entity, float f, float g, float partialTick) {
        this.stalkAmount = entity.getStalkAmount(partialTick);
        this.pounceAmount = entity.getPounceAmount(partialTick);
        this.restAmount = entity.getRestAmount(partialTick);
        this.snarlAmount = entity.getSnarlAmount(partialTick);
        this.fishAmount = entity.getFishAmount(partialTick);
    }

    @Override
    public void setupAnim(T entity, float f, float g, float h, float i, float j) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.head.xRot = j * (float) (Math.PI / 180.0);
        this.head.yRot = i * (float) (Math.PI / 180.0);
        this.rightLegBack.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
        this.leftLegBack.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.rightLegFront.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.leftLegFront.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;

        this.animateBody(h);
        this.animateTail(f, g, h);
    }

    private void animateBody(float ageInTicks) {
        if (this.stalkAmount > 0.0F) {
            float s = this.stalkAmount;
            this.waist.y += 4.0F * s;
            this.leftLegFront.xRot = Mth.lerp(s, this.leftLegFront.xRot, this.leftLegFront.xRot * 0.4F - 0.6F);
            this.rightLegFront.xRot = Mth.lerp(s, this.rightLegFront.xRot, this.rightLegFront.xRot * 0.4F - 0.6F);
            this.leftLegBack.xRot = Mth.lerp(s, this.leftLegBack.xRot, this.leftLegBack.xRot * 0.4F + 0.6F);
            this.rightLegBack.xRot = Mth.lerp(s, this.rightLegBack.xRot, this.rightLegBack.xRot * 0.4F + 0.6F);
            this.head.y += 2.0F * s;
            this.head.xRot = Mth.lerp(s, this.head.xRot, 0.05F);
        }
        if (this.pounceAmount > 0.0F) {
            float p = this.pounceAmount;
            this.body.xRot -= 0.3F * p;
            this.leftLegFront.xRot = Mth.lerp(p, this.leftLegFront.xRot, -1.2F);
            this.rightLegFront.xRot = Mth.lerp(p, this.rightLegFront.xRot, -1.2F);
            this.leftLegBack.xRot = Mth.lerp(p, this.leftLegBack.xRot, 0.8F);
            this.rightLegBack.xRot = Mth.lerp(p, this.rightLegBack.xRot, 0.8F);
        }
        if (this.snarlAmount > 0.0F) {
            float s = this.snarlAmount;
            this.waist.y += 1.5F * s;
            this.head.xRot = Mth.lerp(s, this.head.xRot, 0.25F);
            this.rightEar.zRot += 0.6F * s;
            this.leftEar.zRot -= 0.6F * s;
            this.rightEar.y += 0.5F * s;
            this.leftEar.y += 0.5F * s;
        }
        if (this.fishAmount > 0.0F) {
            float s = this.fishAmount;
            this.head.xRot = Mth.lerp(s, this.head.xRot, 0.5F);
            this.rightLegFront.xRot = Mth.lerp(s, this.rightLegFront.xRot, -1.3F + Mth.sin(ageInTicks * 0.6F) * 0.5F);
        }
        if (this.restAmount > 0.0F) {
            float r = this.restAmount;
            this.waist.y += 8.5F * r;
            this.head.xRot = Mth.lerp(r, this.head.xRot, 0.25F);
            this.head.y = Mth.lerp(r, this.head.y, 0.25F);
            this.leftLegFront.xRot = Mth.lerp(r, this.leftLegFront.xRot, -1.2F);
            this.rightLegFront.xRot = Mth.lerp(r, this.rightLegFront.xRot, -1.2F);
            this.leftLegBack.xRot = Mth.lerp(r, this.leftLegBack.xRot, -1.1F);
            this.rightLegBack.xRot = Mth.lerp(r, this.rightLegBack.xRot, -1.1F);

            this.leftLegFront.yRot = Mth.lerp(this.restAmount, this.leftLegBack.yRot, -0.2F);
            this.rightLegFront.yRot = Mth.lerp(this.restAmount, this.rightLegBack.yRot, 0.2F);
            this.leftLegBack.yRot = Mth.lerp(this.restAmount, this.leftLegBack.yRot, -0.45F);
            this.rightLegBack.yRot = Mth.lerp(this.restAmount, this.rightLegBack.yRot, 0.45F);
        }
    }

    private void animateTail(float limbSwing, float limbSwingAmount, float ageInTicks) {
        float flickWindow = Mth.clamp(Mth.sin(ageInTicks * 0.021F) * 4.0F - 3.0F, 0.0F, 1.0F);
        float calm = 1.0F - 0.8F * this.stalkAmount;
        for (int k = 0; k < this.tailSegments.length; k++) {
            ModelPart segment = this.tailSegments[k];
            float growth = 1.0F + k * 0.6F;
            segment.xRot = TAIL_CURL[k];
            segment.yRot = (Mth.sin(limbSwing * 0.6662F - k * 0.7F) * 0.12F * limbSwingAmount + Mth.sin(ageInTicks * 0.06F - k * 0.6F) * 0.04F) * growth * calm;

            segment.xRot = Mth.lerp(this.stalkAmount, segment.xRot, k == 0 ? -0.35F : 0.05F);
            segment.xRot = Mth.lerp(this.pounceAmount, segment.xRot, k == 0 ? -0.2F : 0.0F);
            segment.yRot += Mth.sin(ageInTicks * 0.6F - k * 0.5F) * 0.35F * (1.0F + k * 0.4F) * this.snarlAmount;

            segment.xRot = Mth.lerp(this.restAmount, segment.xRot, k == 0 ? -1.3F : 0.0F);
            segment.yRot = Mth.lerp(this.restAmount, segment.yRot, 0.45F);
        }
        this.tailTip.yRot += Mth.sin(ageInTicks * 0.6F) * 0.4F * flickWindow * (1.0F - this.restAmount);
        this.tailTip.yRot += Mth.sin(ageInTicks * 0.9F) * 0.3F * this.stalkAmount;
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
