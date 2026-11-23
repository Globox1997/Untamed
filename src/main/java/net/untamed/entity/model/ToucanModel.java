package net.untamed.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.untamed.entity.ToucanEntity;

@Environment(EnvType.CLIENT)
public class ToucanModel<T extends ToucanEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public ToucanModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.tail = this.body.getChild("tail");
        this.leftLeg = this.body.getChild("left_leg");
        this.rightLeg = this.body.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        PartDefinition body = partDefinition.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -3.0F, -3.5F, 5.0F, 5.0F, 7.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-2.0F, -4.0F, -2.5F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -2.5F, -2.5F));

        head.addOrReplaceChild("beak", CubeListBuilder.create()
                .texOffs(16, 12).addBox(-1.5F, -3.5F, -7.5F, 3.0F, 3.0F, 5.0F),
                PartPose.ZERO);

        body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(0, 20).addBox(0.0F, 0.0F, -2.5F, 1.0F, 4.0F, 6.0F),
                PartPose.offset(2.5F, -2.5F, -0.5F));

        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(14, 20).addBox(-1.0F, 0.0F, -2.5F, 1.0F, 4.0F, 6.0F),
                PartPose.offset(-2.5F, -2.5F, -0.5F));

        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 3.5F, -0.2618F, 0.0F, 0.0F));

        body.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(24, 6).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(1.25F, 2.0F, 0.0F));

        body.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(28, 6).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(-1.25F, 2.0F, 0.0F));

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.xRot = headPitch * ((float) Math.PI / 180F);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);

        if (!entity.onGround()) {
            float flap = Mth.cos(ageInTicks * 1.4F) * 1.1F;
            this.leftWing.zRot = -flap - 0.2F;
            this.rightWing.zRot = flap + 0.2F;
            this.leftLeg.xRot = 0.4F;
            this.rightLeg.xRot = 0.4F;
        } else {
            this.leftWing.zRot = 0.0F;
            this.rightWing.zRot = 0.0F;
            this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
