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
import net.untamed.entity.HyenaEntity;

@Environment(EnvType.CLIENT)
public class HyenaModel<T extends HyenaEntity> extends HierarchicalModel<T> {

    private static final float SIDE_ROLL = 1.5F;
    private static final float HEAD_COUNTER_ROLL = 0.75F;
    private static final float SIDE_LEG_SPREAD_LOWER = 0.15F;
    private static final float SIDE_LEG_SPREAD_UPPER = 0.55F;
    private static final float UPPER_FRONT_LEG_DROP = 0.8F;
    private static final float UPPER_BACK_LEG_DROP = 0.95F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart body2;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private float restAmount;
    private int restVariant;
    private float sniffAmount;
    private float alertAmount;
    private float excitedAmount;
    private float whoopAmount;
    private float eatAmount;

    public HyenaModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.body = this.root.getChild("body");
        this.body2 = this.body.getChild("body2");
        this.leftBackLeg = this.body2.getChild("leftBackLeg");
        this.rightBackLeg = this.body2.getChild("rightBackLeg");
        this.tail = this.body2.getChild("tail");
        this.head = this.body.getChild("head");
        this.rightEar = this.head.getChild("rightEar");
        this.leftEar = this.head.getChild("leftEar");
        this.leftFrontLeg = this.body.getChild("leftFrontLeg");
        this.rightFrontLeg = this.body.getChild("rightFrontLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 10.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(34, 6).addBox(0.0F, -11.0F, -5.0F, 0.0F, 3.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -11.0F, -3.0F));

        PartDefinition body2 = body.addOrReplaceChild("body2", CubeListBuilder.create().texOffs(0, 19).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 7.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 5.0F));

        PartDefinition leftBackLeg = body2.addOrReplaceChild("leftBackLeg", CubeListBuilder.create().texOffs(0, 41).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(30, 30).addBox(-1.0F, 6.0F, -3.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 3.0F, 5.0F));

        PartDefinition rightBackLeg = body2.addOrReplaceChild("rightBackLeg", CubeListBuilder.create().texOffs(0, 41).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(30, 30).mirror().addBox(-3.0F, 6.0F, -3.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.0F, 3.0F, 5.0F));

        PartDefinition tail = body2.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 42).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.5F, 6.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(30, 19).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 6.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 35).addBox(-3.0F, 0.0F, -7.0F, 6.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.0F, -4.0F));

        PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(34, 11).addBox(-3.5F, -2.0F, 0.0F, 7.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, -2.5F, 0.0F, 1.5708F, 0.0F));

        PartDefinition rightEar = head.addOrReplaceChild("rightEar", CubeListBuilder.create().texOffs(16, 35).addBox(-1.0F, -3.0F, -0.5F, 5.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -2.0F, -0.5F));

        PartDefinition leftEar = head.addOrReplaceChild("leftEar", CubeListBuilder.create().texOffs(16, 35).mirror().addBox(-4.0F, -3.0F, -0.5F, 5.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.0F, -2.0F, -0.5F));

        PartDefinition leftFrontLeg = body.addOrReplaceChild("leftFrontLeg", CubeListBuilder.create().texOffs(0, 40).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(30, 30).addBox(-1.0F, 7.0F, -3.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 2.0F, 1.0F));

        PartDefinition rightFrontLeg = body.addOrReplaceChild("rightFrontLeg", CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(30, 30).mirror().addBox(-3.0F, 7.0F, -3.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.0F, 2.0F, 1.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void prepareMobModel(T entity, float f, float g, float partialTick) {
        this.restAmount = entity.getRestAmount(partialTick);
        this.restVariant = entity.getRestVariant();
        this.sniffAmount = entity.getSniffAmount(partialTick);
        this.alertAmount = entity.getAlertAmount(partialTick);
        this.excitedAmount = entity.getExcitedAmount(partialTick);
        this.whoopAmount = entity.getWhoopAmount(partialTick);
        this.eatAmount = entity.getEatAmount(partialTick);
    }

    @Override
    public void setupAnim(T entity, float f, float g, float h, float i, float j) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.head.xRot = j * (float) (Math.PI / 180.0);
        this.head.yRot = i * (float) (Math.PI / 180.0);
        this.tail.yRot = Mth.cos(f * 0.6662F) * 0.3F * g;
        this.rightBackLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
        this.leftBackLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.rightFrontLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.leftFrontLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;

        if (this.sniffAmount > 0.0F) {
            this.body.xRot += 0.08F * this.sniffAmount;
            this.head.y += 1.0F * this.sniffAmount;
            this.head.xRot = Mth.lerp(this.sniffAmount, this.head.xRot, 0.8F + Mth.sin(h * 1.2F) * 0.06F);
        }
        if (this.eatAmount > 0.0F) {
            this.body.xRot += 0.15F * this.eatAmount;
            this.head.y += 3.0F * this.eatAmount;
            this.head.xRot = Mth.lerp(this.eatAmount, this.head.xRot, 1.0F + Mth.sin(h * 0.8F) * 0.08F);
        }
        if (this.alertAmount > 0.0F) {
            this.head.xRot = Mth.lerp(this.alertAmount, this.head.xRot, -0.25F);
            this.rightEar.zRot -= 0.35F * this.alertAmount;
            this.leftEar.zRot += 0.35F * this.alertAmount;
        }
        if (this.excitedAmount > 0.0F) {
            this.head.xRot = Mth.lerp(this.excitedAmount, this.head.xRot, 0.2F);
            this.tail.xRot = Mth.lerp(this.excitedAmount, this.tail.xRot, 2.3F);
            this.rightEar.xRot -= 0.6F * this.excitedAmount;
            this.leftEar.xRot -= 0.6F * this.excitedAmount;
        }
        if (this.whoopAmount > 0.0F) {
            this.body.xRot += 0.15F * this.whoopAmount;
            this.head.y += 2.0F * this.whoopAmount;
            this.head.xRot = Mth.lerp(this.whoopAmount, this.head.xRot, 0.7F);
        }
        if (this.restAmount > 0.0F) {
            if (this.restVariant == 0) {
                this.poseRestBelly();
            } else {
                this.poseRestSide(this.restVariant == 1 ? 1.0F : -1.0F);
            }
        }
    }

    private void poseRestBelly() {
        float drop = 7.0F * this.restAmount;
        this.body.y += drop;
        this.head.xRot = Mth.lerp(this.restAmount, this.head.xRot, 0.2F);
        this.leftFrontLeg.xRot = Mth.lerp(this.restAmount, this.leftFrontLeg.xRot, -1.45F);
        this.rightFrontLeg.xRot = Mth.lerp(this.restAmount, this.rightFrontLeg.xRot, -1.45F);
        this.leftBackLeg.xRot = Mth.lerp(this.restAmount, this.leftBackLeg.xRot, -1.45F);
        this.rightBackLeg.xRot = Mth.lerp(this.restAmount, this.rightBackLeg.xRot, -1.45F);

        this.leftBackLeg.yRot = Mth.lerp(this.restAmount, this.leftBackLeg.yRot, -0.3F);
        this.rightBackLeg.yRot = Mth.lerp(this.restAmount, this.rightBackLeg.yRot, 0.3F);
        this.tail.xRot = Mth.lerp(this.restAmount, this.tail.xRot, 1.3F);
    }

    private void poseRestSide(float side) {
        float r = this.restAmount;
        float roll = SIDE_ROLL * side;
        this.body.zRot = Mth.lerp(r, this.body.zRot, roll);
        this.body.y += 7.0F * r;
        this.body.x -= 3.0F * Mth.sin(roll) * r;
        this.head.xRot = Mth.lerp(r, this.head.xRot, 0.1F);
        this.head.yRot = Mth.lerp(r, this.head.yRot, 0.0F);
        this.head.zRot = Mth.lerp(r, this.head.zRot, -roll * HEAD_COUNTER_ROLL);
        ModelPart lowerFront = side > 0.0F ? this.leftFrontLeg : this.rightFrontLeg;
        ModelPart upperFront = side > 0.0F ? this.rightFrontLeg : this.leftFrontLeg;
        ModelPart lowerBack = side > 0.0F ? this.leftBackLeg : this.rightBackLeg;
        ModelPart upperBack = side > 0.0F ? this.rightBackLeg : this.leftBackLeg;
        lowerFront.xRot = Mth.lerp(r, lowerFront.xRot, -SIDE_LEG_SPREAD_LOWER);
        upperFront.xRot = Mth.lerp(r, upperFront.xRot, -SIDE_LEG_SPREAD_UPPER);
        lowerBack.xRot = Mth.lerp(r, lowerBack.xRot, SIDE_LEG_SPREAD_LOWER);
        upperBack.xRot = Mth.lerp(r, upperBack.xRot, SIDE_LEG_SPREAD_UPPER);
        upperFront.zRot = Mth.lerp(r, upperFront.zRot, -UPPER_FRONT_LEG_DROP * side);
        upperBack.zRot = Mth.lerp(r, upperBack.zRot, -UPPER_BACK_LEG_DROP * side);
        this.tail.xRot = Mth.lerp(r, this.tail.xRot, 1.0F);
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
