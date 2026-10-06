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
import net.untamed.entity.BisonEntity;

@Environment(EnvType.CLIENT)
public class BisonModel<T extends BisonEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart horns;
    private final ModelPart back;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private float grazeAmount;
    private float restAmount;
    private float warnAmount;
    private float chargeAmount;

    public BisonModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.body = this.root.getChild("body");
        this.head = this.body.getChild("head");
        this.horns = this.head.getChild("horns");
        this.back = this.root.getChild("back");
        this.tail = this.back.getChild("tail");
        this.leftFrontLeg = this.root.getChild("leftFrontLeg");
        this.rightFrontLeg = this.root.getChild("rightFrontLeg");
        this.leftBackLeg = this.root.getChild("leftBackLeg");
        this.rightBackLeg = this.root.getChild("rightBackLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 35).addBox(-7.5F, 4.0F, -7.0F, 15.0F, 6.0F, 20.0F, new CubeDeformation(0.1F))
                .texOffs(0, 0).addBox(-7.5F, -7.0F, -7.0F, 15.0F, 15.0F, 20.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -17.0F, -9.5F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 61).addBox(-3.5F, 1.0F, -6.0F, 7.0F, 12.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.0F, -7.0F));

        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(57, 48).addBox(-5.0F, -7.0F, -8.0F, 8.0F, 8.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 1.0F, 1.0F, 1.0472F, 0.0F, 0.0F));

        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create().texOffs(50, 0).addBox(-6.0F, -2.0F, -5.0F, 12.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, 0.0F));

        PartDefinition horns_r1 = horns.addOrReplaceChild("horns_r1", CubeListBuilder.create().texOffs(54, 69).mirror().addBox(-8.0F, -9.0F, -5.0F, 2.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(54, 69).addBox(6.0F, -9.0F, -5.0F, 2.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.2217F, 0.0F, 0.0F));

        PartDefinition back = root.addOrReplaceChild("back", CubeListBuilder.create().texOffs(56, 21).addBox(-6.0F, -2.948F, -0.0721F, 12.0F, 13.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -19.0F, 2.5F));

        PartDefinition tail = back.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, -1.0F, 14.0F));

        PartDefinition tail_r1 = tail.addOrReplaceChild("tail_r1", CubeListBuilder.create().texOffs(0, 35).addBox(-4.0F, -1.0F, 0.0F, 7.0F, 15.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 0.0F, 0.0F, 0.0436F, 0.0F, 0.0F));

        PartDefinition leftFrontLeg = root.addOrReplaceChild("leftFrontLeg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 11.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(66, 69).addBox(-2.0F, -1.0F, 2.5F, 4.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -10.0F, -9.5F));

        PartDefinition rightFrontLeg = root.addOrReplaceChild("rightFrontLeg", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-2.0F, -1.0F, -2.5F, 4.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(66, 69).mirror().addBox(-2.0F, -1.0F, 2.5F, 4.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-5.0F, -10.0F, -9.5F));

        PartDefinition leftBackLeg = root.addOrReplaceChild("leftBackLeg", CubeListBuilder.create().texOffs(34, 61).addBox(-2.1571F, -2.948F, -3.0721F, 4.0F, 17.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, -14.0F, 11.0F));

        PartDefinition rightBackLeg = root.addOrReplaceChild("rightBackLeg", CubeListBuilder.create().texOffs(34, 61).mirror().addBox(-1.8429F, -2.948F, -3.0721F, 4.0F, 17.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-5.0F, -14.0F, 11.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void prepareMobModel(T entity, float f, float g, float partialTick) {
        this.grazeAmount = entity.getGrazeAmount(partialTick);
        this.restAmount = entity.getRestAmount(partialTick);
        this.warnAmount = entity.getWarnAmount(partialTick);
        this.chargeAmount = entity.getChargeAmount(partialTick);
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

        if (this.grazeAmount > 0.0F) {
            this.head.y += 4.5F * this.grazeAmount;
            this.head.xRot = Mth.lerp(this.grazeAmount, this.head.xRot, 0.15F + Mth.sin(h * 0.3F) * 0.05F);
        }
        if (this.warnAmount > 0.0F) {
            this.head.y += 2.0F * this.warnAmount;
            this.head.xRot = Mth.lerp(this.warnAmount, this.head.xRot, 0.3F);
            this.head.yRot += Mth.sin(h * 0.8F) * 0.12F * this.warnAmount;
            this.tail.xRot += 2.0F * this.warnAmount;
            this.rightFrontLeg.xRot = Mth.lerp(this.warnAmount, this.rightFrontLeg.xRot, -0.4F + Mth.sin(h * 0.6F) * 0.4F);
        }
        if (this.chargeAmount > 0.0F) {
            this.head.y += 2.5F * this.chargeAmount;
            this.head.xRot = Mth.lerp(this.chargeAmount, this.head.xRot, 0.5F);
            this.tail.xRot += 2.2F * this.chargeAmount;
        }
        if (this.restAmount > 0.0F) {
            float drop = 7.0F * this.restAmount;
            this.body.y += drop;
            this.back.y += drop;
            this.leftFrontLeg.y += drop;
            this.rightFrontLeg.y += drop;
            this.leftBackLeg.y += drop;
            this.rightBackLeg.y += drop;

            this.leftFrontLeg.xRot = Mth.lerp(this.restAmount, this.leftFrontLeg.xRot, -1.0F);
            this.rightFrontLeg.xRot = Mth.lerp(this.restAmount, this.rightFrontLeg.xRot, -1.0F);
            this.leftBackLeg.xRot = Mth.lerp(this.restAmount, this.leftBackLeg.xRot, -0.7F);
            this.rightBackLeg.xRot = Mth.lerp(this.restAmount, this.rightBackLeg.xRot, -0.7F);

            this.leftFrontLeg.zRot = Mth.lerp(this.restAmount, this.leftFrontLeg.yRot, -1.45F);
            this.rightFrontLeg.zRot = Mth.lerp(this.restAmount, this.rightFrontLeg.yRot, 1.45F);
            this.leftBackLeg.zRot = Mth.lerp(this.restAmount, this.leftBackLeg.yRot, -1.45F);
            this.rightBackLeg.zRot = Mth.lerp(this.restAmount, this.rightBackLeg.yRot, 1.45F);

            this.leftBackLeg.yRot = Mth.lerp(this.restAmount, this.leftBackLeg.yRot, 0.45F);
            this.rightBackLeg.yRot = Mth.lerp(this.restAmount, this.rightBackLeg.yRot, -0.45F);
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
