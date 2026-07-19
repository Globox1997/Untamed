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
import net.untamed.entity.LionEntity;

@Environment(EnvType.CLIENT)
public class LionModel<T extends LionEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart mane;
    private final ModelPart tail;
    private final ModelPart tail2;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart leftBackLeg;

    public LionModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.body = this.root.getChild("body");
        this.head = this.body.getChild("head");
        this.mane = this.head.getChild("mane");
        this.tail = this.body.getChild("tail");
        this.tail2 = this.tail.getChild("tail2");
        this.rightFrontLeg = this.body.getChild("rightFrontLeg");
        this.leftFrontLeg = this.body.getChild("leftFrontLeg");
        this.rightBackLeg = this.body.getChild("rightBackLeg");
        this.leftBackLeg = this.body.getChild("leftBackLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, -6.0F, -11.0F, 11.0F, 12.0F, 21.0F, new CubeDeformation(0.0F))
                .texOffs(43, 0).addBox(-5.5F, -1.0F, -11.0F, 11.0F, 10.0F, 10.0F, new CubeDeformation(0.2F)), PartPose.offset(0.0F, -16.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(43, 4).addBox(5.0F, -10.0F, -5.0F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(43, 0).addBox(-3.0F, -10.0F, -5.0F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(54, 55).addBox(-2.0F, -7.0F, -8.0F, 10.0F, 9.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(0, 14).addBox(-0.5F, -2.0F, -10.0F, 7.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, -1.0F, -12.0F));

        PartDefinition mane = head.addOrReplaceChild("mane", CubeListBuilder.create().texOffs(0, 33).addBox(-4.5F, -10.5F, -3.5F, 15.0F, 15.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(46, 33).addBox(-8.0F, -14.0F, -1.5F, 22.0F, 22.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, -3.0F, 10.0F));

        PartDefinition cube_r1 = tail.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(29, 55).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 3.0F, 17.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 3.0F, -0.3927F, 0.0F, 0.0F));

        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.offset(0.0F, 6.0F, 15.0F));

        PartDefinition cube_r2 = tail2.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(16, 56).addBox(-2.0F, -1.0F, -1.0F, 4.0F, 3.0F, 6.0F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F));

        PartDefinition rightFrontLeg = body.addOrReplaceChild("rightFrontLeg", CubeListBuilder.create().texOffs(0, 56).addBox(-1.75F, 2.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(12, 66).addBox(-1.75F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offset(3.0F, 4.0F, -8.0F));

        PartDefinition leftFrontLeg = body.addOrReplaceChild("leftFrontLeg", CubeListBuilder.create().texOffs(0, 56).mirror().addBox(-2.25F, 2.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(12, 66).mirror().addBox(-2.25F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.2F)).mirror(false), PartPose.offset(-3.0F, 4.0F, -8.0F));

        PartDefinition rightBackLeg = body.addOrReplaceChild("rightBackLeg", CubeListBuilder.create().texOffs(0, 0).addBox(-1.75F, 2.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(64, 20).addBox(-1.75F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.2F)), PartPose.offset(3.0F, 4.0F, 7.0F));

        PartDefinition leftBackLeg = body.addOrReplaceChild("leftBackLeg", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-2.25F, 2.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(64, 20).mirror().addBox(-2.25F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.2F)).mirror(false), PartPose.offset(-3.0F, 4.0F, 7.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
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

        if (entity.isSleeping()) {
            this.body.zRot = -1.52f;
            this.body.y = -5f;
            this.head.zRot = 1.44f;
            this.head.y = -3f;
            this.tail.yRot = -0.174f;
            this.leftFrontLeg.xRot = -0.26f;
            this.leftBackLeg.xRot = 0.17f;
            this.rightFrontLeg.xRot = 0.2f;
            this.rightFrontLeg.yRot = -0.1f;
            this.rightFrontLeg.zRot = 0.2f;
            this.rightBackLeg.xRot = 0.6f;
            this.rightBackLeg.yRot = -0.26f;
            this.rightBackLeg.zRot = 0.48f;
        } else {
            this.body.zRot = 0f;
            this.body.y = -16f;
            this.head.zRot = 0f;
            this.head.y = -1f;
            this.rightFrontLeg.yRot = 0f;
            this.rightFrontLeg.zRot = 0f;
            this.rightBackLeg.yRot = 0f;
            this.rightBackLeg.zRot = 0f;
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
