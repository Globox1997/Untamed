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
import net.untamed.entity.RhinoEntity;

@Environment(EnvType.CLIENT)
public class RhinoModel<T extends RhinoEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart horns;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart body;
    private final ModelPart rightBackLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;

    public RhinoModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.tail = this.root.getChild("tail");
        this.head = this.root.getChild("head");
        this.horns = this.head.getChild("horns");
        this.leftEar = this.head.getChild("leftEar");
        this.rightEar = this.head.getChild("rightEar");
        this.body = this.root.getChild("body");
        this.rightBackLeg = this.root.getChild("rightBackLeg");
        this.leftBackLeg = this.root.getChild("leftBackLeg");
        this.leftFrontLeg = this.root.getChild("leftFrontLeg");
        this.rightFrontLeg = this.root.getChild("rightFrontLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offsetAndRotation(14.0F, 24.0F, 2.0F, 0.0F, -1.5708F, 0.0F));

        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, -9.0F, 11.5F));

        PartDefinition cube_r1 = tail.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 109).mirror().addBox(0.3233F, -2.5433F, -3.5F, 0.0F, 10.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(8.75F, -10.0F, 3.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-15.0F, -15.0F, 14.5F));

        PartDefinition cube_r2 = head.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 65).addBox(-10.2982F, -5.4735F, -5.0F, 13.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.75F, 0.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create(), PartPose.offset(-8.0F, -2.25F, -0.5F));

        PartDefinition cube_r3 = horns.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(64, 59).addBox(-2.0F, -6.0F, 0.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -0.75F, -0.5F, 0.0F, 0.0F, -0.3927F));

        PartDefinition cube_r4 = horns.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(72, 57).mirror().addBox(-2.0F, -6.0F, -1.0F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition leftEar = head.addOrReplaceChild("leftEar", CubeListBuilder.create(), PartPose.offset(-2.0F, -7.0F, 4.5F));

        PartDefinition cube_r5 = leftEar.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(58, 22).addBox(-5.0F, -4.0F, 2.0F, 5.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 0.25F, -1.5F, 0.0F, 0.0F, -0.3927F));

        PartDefinition rightEar = head.addOrReplaceChild("rightEar", CubeListBuilder.create(), PartPose.offset(-2.0F, -7.0F, -5.5F));

        PartDefinition cube_r6 = rightEar.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(58, 26).addBox(-5.0F, -4.0F, 2.0F, 5.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 0.25F, -1.5F, 0.0F, 0.0F, -0.3927F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 30).addBox(-12.0F, -8.0F, -10.5F, 12.0F, 15.0F, 20.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(0.0F, -6.0F, -9.0F, 12.0F, 13.0F, 17.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, -16.0F, 15.0F));

        PartDefinition rightBackLeg = root.addOrReplaceChild("rightBackLeg", CubeListBuilder.create().texOffs(0, 85).mirror().addBox(-3.5F, 0.0F, -3.0F, 7.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(5.5F, -9.0F, 9.0F));

        PartDefinition leftBackLeg = root.addOrReplaceChild("leftBackLeg", CubeListBuilder.create().texOffs(0, 100).addBox(-3.5F, 0.0F, -3.0F, 7.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(5.5F, -9.0F, 20.0F));

        PartDefinition leftFrontLeg = root.addOrReplaceChild("leftFrontLeg", CubeListBuilder.create().texOffs(0, 100).addBox(-3.5F, 0.0F, -3.0F, 7.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(-9.5F, -9.0F, 20.0F));

        PartDefinition rightFrontLeg = root.addOrReplaceChild("rightFrontLeg", CubeListBuilder.create().texOffs(0, 85).mirror().addBox(-3.5F, 0.0F, -3.0F, 7.0F, 9.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-9.5F, -9.0F, 9.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float f, float g, float h, float i, float j) {
        this.head.xRot = j * (float) (Math.PI / 180.0);
        this.head.yRot = i * (float) (Math.PI / 180.0);
        this.tail.yRot = Mth.cos(f * 0.6662F) * 0.3F * g;
        this.rightBackLeg.zRot = Mth.cos(f * 0.6662F) * 1.4F * g;
        this.leftBackLeg.zRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.rightFrontLeg.zRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.leftFrontLeg.zRot = Mth.cos(f * 0.6662F) * 1.4F * g;
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
