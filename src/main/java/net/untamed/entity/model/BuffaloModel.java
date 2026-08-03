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
import net.untamed.entity.BuffaloEntity;

@Environment(EnvType.CLIENT)
public class BuffaloModel<T extends BuffaloEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart waist;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart horns;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;

    public BuffaloModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.waist = this.root.getChild("waist");
        this.body = this.waist.getChild("body");
        this.head = this.body.getChild("head");
        this.leftEar = this.head.getChild("leftEar");
        this.rightEar = this.head.getChild("rightEar");
        this.horns = this.head.getChild("horns");
        this.tail = this.body.getChild("tail");
        this.leftFrontLeg = this.waist.getChild("leftFrontLeg");
        this.rightFrontLeg = this.waist.getChild("rightFrontLeg");
        this.leftBackLeg = this.waist.getChild("leftBackLeg");
        this.rightBackLeg = this.waist.getChild("rightBackLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -6.0F, -11.0F, 14.0F, 12.0F, 21.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -4.0F, -11.0F));

        PartDefinition cube_r1 = head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 33).addBox(-4.0F, -8.0F, -11.0F, 8.0F, 8.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 2.0F, 0.5672F, 0.0F, 0.0F));

        PartDefinition leftEar = head.addOrReplaceChild("leftEar", CubeListBuilder.create().texOffs(22, 54).addBox(0.0F, -2.0F, -1.0F, 7.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -3.0F, -2.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition rightEar = head.addOrReplaceChild("rightEar", CubeListBuilder.create().texOffs(22, 54).mirror().addBox(-7.0F, -2.0F, -1.0F, 7.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-3.0F, -3.0F, -2.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create().texOffs(0, 54).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(58, 33).addBox(4.0F, -3.0F, -2.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(58, 33).mirror().addBox(-7.0F, -3.0F, -2.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, -7.0F, 0.0F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(58, 42).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 11.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, 10.0F, 0.1309F, 0.0F, 0.0F));

        PartDefinition leftFrontLeg = waist.addOrReplaceChild("leftFrontLeg", CubeListBuilder.create().texOffs(42, 33).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -1.0F, -8.0F));

        PartDefinition rightFrontLeg = waist.addOrReplaceChild("rightFrontLeg", CubeListBuilder.create().texOffs(42, 33).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.0F, -1.0F, -8.0F));

        PartDefinition leftBackLeg = waist.addOrReplaceChild("leftBackLeg", CubeListBuilder.create().texOffs(42, 50).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -1.0F, 7.0F));

        PartDefinition rightBackLeg = waist.addOrReplaceChild("rightBackLeg", CubeListBuilder.create().texOffs(42, 50).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.0F, -1.0F, 7.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float f, float g, float h, float i, float j) {
        this.head.xRot = j * (float) (Math.PI / 180.0);
        this.head.yRot = i * (float) (Math.PI / 180.0);
        this.rightBackLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
        this.leftBackLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.rightFrontLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.leftFrontLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
        this.horns.visible = !entity.isBaby();
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
