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
import net.untamed.entity.BlackBearEntity;

@Environment(EnvType.CLIENT)
public class BlackBearModel<T extends BlackBearEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftLegBack;
    private final ModelPart rightLegBack;
    private final ModelPart leftLegFront;
    private final ModelPart rightLegFront;
    private static final float STAND_ANGLE = 1.2F;
    private float standAmount;
    private float restAmount;
    private float forageAmount;
    private float eatAmount;
    private float huffAmount;
    private float rubAmount;

    public BlackBearModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.body = this.root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftEar = this.head.getChild("leftEar");
        this.rightEar = this.head.getChild("rightEar");
        this.leftLegBack = this.root.getChild("leftLegBack");
        this.rightLegBack = this.root.getChild("rightLegBack");
        this.leftLegFront = this.root.getChild("leftLegFront");
        this.rightLegFront = this.root.getChild("rightLegFront");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -10.0F, 2.0F));

        PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(76, 0).addBox(-8.0F, -28.0F, 5.0F, 14.0F, 14.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 9.0F, 14.0F, 1.5708F, 0.0F, 0.0F));

        PartDefinition body_r2 = body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(2, 1).addBox(-6.0F, -25.0F, 5.0F, 12.0F, 10.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 9.0F, 25.0F, 1.5708F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(40, 24).addBox(-3.5F, -4.0F, -7.0F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(18, 43).addBox(-2.5F, 0.0F, -9.0F, 5.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, -13.0F));

        PartDefinition leftEar = head.addOrReplaceChild("leftEar", CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, -2.0F, -1.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -3.0F, -4.0F));

        PartDefinition rightEar = head.addOrReplaceChild("rightEar", CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-2.5F, -2.0F, -1.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.0F, -3.0F, -4.0F));

        PartDefinition leftLegBack = root.addOrReplaceChild("leftLegBack", CubeListBuilder.create().texOffs(40, 38).addBox(-2.0F, 0.0F, -3.5F, 4.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -8.0F, 7.5F));

        PartDefinition rightLegBack = root.addOrReplaceChild("rightLegBack", CubeListBuilder.create().texOffs(40, 38).mirror().addBox(-2.0F, 0.0F, -3.5F, 4.0F, 8.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.0F, -8.0F, 7.5F));

        PartDefinition leftLegFront = root.addOrReplaceChild("leftLegFront", CubeListBuilder.create().texOffs(0, 43).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(4.5F, -8.0F, -6.5F));

        PartDefinition rightLegFront = root.addOrReplaceChild("rightLegFront", CubeListBuilder.create().texOffs(0, 43).mirror().addBox(-2.0F, 0.0F, -2.5F, 4.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.5F, -8.0F, -6.5F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void prepareMobModel(T entity, float f, float g, float partialTick) {
        this.standAmount = entity.getStandAmount(partialTick);
        this.restAmount = entity.getRestAmount(partialTick);
        this.forageAmount = entity.getForageAmount(partialTick);
        this.eatAmount = entity.getEatAmount(partialTick);
        this.huffAmount = entity.getHuffAmount(partialTick);
        this.rubAmount = entity.getRubAmount(partialTick);
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

        if (this.forageAmount > 0.0F) {
            this.head.y += 2.0F * this.forageAmount;
            this.head.xRot = Mth.lerp(this.forageAmount, this.head.xRot, 0.9F);
            this.rightLegFront.xRot = Mth.lerp(this.forageAmount, this.rightLegFront.xRot, -0.5F + Mth.sin(h * 0.5F) * 0.5F);
        }
        if (this.eatAmount > 0.0F) {
            this.head.y += 2.0F * this.eatAmount;
            this.head.xRot = Mth.lerp(this.eatAmount, this.head.xRot, 0.7F + Mth.sin(h * 0.6F) * 0.08F);
        }
        if (this.huffAmount > 0.0F) {
            float slap = Math.max(0.0F, Mth.sin(h * 0.5F));
            this.head.xRot = Mth.lerp(this.huffAmount, this.head.xRot, 0.35F);
            this.head.z -= 1.0F * this.huffAmount;
            this.leftLegFront.xRot = Mth.lerp(this.huffAmount, this.leftLegFront.xRot, -0.6F * slap);
            this.rightLegFront.xRot = Mth.lerp(this.huffAmount, this.rightLegFront.xRot, -0.6F * slap);
        }
        if (this.standAmount > 0.0F) {
            this.applyStanding(h);
        }
        if (this.restAmount > 0.0F) {
            float drop = 6.0F * this.restAmount;
            this.body.y += drop;
            this.leftLegFront.y += drop;
            this.rightLegFront.y += drop;
            this.leftLegBack.y += drop;
            this.rightLegBack.y += drop;
            this.head.xRot = Mth.lerp(this.restAmount, this.head.xRot, 0.3F);
            this.leftLegFront.xRot = Mth.lerp(this.restAmount, this.leftLegFront.xRot, -1.5F);
            this.rightLegFront.xRot = Mth.lerp(this.restAmount, this.rightLegFront.xRot, -1.5F);
            this.leftLegBack.xRot = Mth.lerp(this.restAmount, this.leftLegBack.xRot, 1.5F);
            this.rightLegBack.xRot = Mth.lerp(this.restAmount, this.rightLegBack.xRot, 1.5F);
        }
    }

    private void applyStanding(float ageInTicks) {
        float angle = -STAND_ANGLE * this.standAmount;
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        float hipY = this.leftLegBack.y;
        float hipZ = this.leftLegBack.z;

        float bodyY = this.body.y - hipY;
        float bodyZ = this.body.z - hipZ;
        this.body.y = hipY + bodyY * cos - bodyZ * sin;
        this.body.z = hipZ + bodyY * sin + bodyZ * cos;
        this.body.xRot += angle;
        this.head.xRot -= angle;

        for (ModelPart leg : new ModelPart[]{this.leftLegFront, this.rightLegFront}) {
            float legY = leg.y - hipY;
            float legZ = leg.z - hipZ;
            leg.y = hipY + legY * cos - legZ * sin;
            leg.z = hipZ + legY * sin + legZ * cos;
            leg.xRot = Mth.lerp(this.standAmount, leg.xRot, -0.4F);
        }
        this.leftLegBack.xRot = Mth.lerp(this.standAmount, this.leftLegBack.xRot, 0.0F);
        this.rightLegBack.xRot = Mth.lerp(this.standAmount, this.rightLegBack.xRot, 0.0F);

        if (this.rubAmount > 0.0F) {
            this.body.zRot += Mth.sin(ageInTicks * 0.25F) * 0.12F * this.rubAmount;
            this.body.y += Mth.sin(ageInTicks * 0.5F) * 0.4F * this.rubAmount;
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
    public ModelPart root() {
        return this.root;
    }

}
