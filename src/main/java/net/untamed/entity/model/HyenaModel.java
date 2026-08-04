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
    public void setupAnim(T entity, float f, float g, float h, float i, float j) {
        this.head.xRot = j * (float) (Math.PI / 180.0);
        this.head.yRot = i * (float) (Math.PI / 180.0);
        this.tail.yRot = Mth.cos(f * 0.6662F) * 0.3F * g;
        this.rightBackLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
        this.leftBackLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.rightFrontLeg.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.leftFrontLeg.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
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
