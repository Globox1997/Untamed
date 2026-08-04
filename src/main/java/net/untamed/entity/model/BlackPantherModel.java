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

    private final ModelPart root;
    private final ModelPart waist;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart tail2;
    private final ModelPart leftLegFront;
    private final ModelPart rightLegFront;
    private final ModelPart leftLegBack;
    private final ModelPart rightLegBack;

    public BlackPantherModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.waist = this.root.getChild("waist");
        this.body = this.waist.getChild("body");
        this.head = this.body.getChild("head");
        this.tail = this.body.getChild("tail");
        this.tail2 = this.tail.getChild("tail2");
        this.leftLegFront = this.waist.getChild("leftLegFront");
        this.rightLegFront = this.waist.getChild("rightLegFront");
        this.leftLegBack = this.waist.getChild("leftLegBack");
        this.rightLegBack = this.waist.getChild("rightLegBack");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offset(0.0F, -9.0F, 0.0F));

        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create().texOffs(1, 33).addBox(-6.0F, -7.0F, -12.5F, 12.0F, 12.0F, 26.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.0F, -2.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(1, 1).addBox(-5.0F, -7.0F, -8.5F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(42, 14).addBox(2.0F, -9.0F, -3.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(42, 14).mirror().addBox(-4.0F, -9.0F, -3.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(1, 22).addBox(-3.5F, -2.0F, -11.5F, 7.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(22, 23).addBox(-8.0F, -3.5F, -10.5F, 16.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, -12.5F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(78, 53).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 14.0F, new CubeDeformation(0.05F)), PartPose.offset(0.0F, -4.0F, 13.0F));

        PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(78, 72).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 14.0F));

        PartDefinition leftLegFront = waist.addOrReplaceChild("leftLegFront", CubeListBuilder.create().texOffs(1, 72).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.5F, -6.0F, -11.0F));

        PartDefinition rightLegFront = waist.addOrReplaceChild("rightLegFront", CubeListBuilder.create().texOffs(1, 72).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 15.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.5F, -6.0F, -11.0F));

        PartDefinition leftLegBack = waist.addOrReplaceChild("leftLegBack", CubeListBuilder.create().texOffs(18, 72).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 17.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.5F, -8.0F, 7.9F));

        PartDefinition rightLegBack = waist.addOrReplaceChild("rightLegBack", CubeListBuilder.create().texOffs(18, 72).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 17.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-4.5F, -8.0F, 7.9F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float f, float g, float h, float i, float j) {
        this.head.xRot = j * (float) (Math.PI / 180.0);
        this.head.yRot = i * (float) (Math.PI / 180.0);
        this.rightLegBack.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
        this.leftLegBack.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.rightLegFront.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.leftLegFront.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
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
