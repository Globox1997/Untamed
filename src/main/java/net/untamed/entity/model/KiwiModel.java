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
import net.untamed.entity.KiwiEntity;

@Environment(EnvType.CLIENT)
public class KiwiModel<T extends KiwiEntity> extends HierarchicalModel<T> {

    private final ModelPart root;
    private final ModelPart waist;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private float sleepAmount;
    private int restVariant;
    private float probeAmount;
    private float defendAmount;

    public KiwiModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.waist = this.root.getChild("waist");
        this.body = this.waist.getChild("body");
        this.head = this.body.getChild("head");
        this.leftLeg = this.waist.getChild("leftLeg");
        this.rightLeg = this.waist.getChild("rightLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offset(0.0F, -4.0F, 0.0F));

        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -3.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 14).addBox(-2.0F, -3.0F, -4.0F, 4.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(18, 20).addBox(-0.5F, 0.0F, -7.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -2.0F, -4.0F));

        PartDefinition leftLeg = waist.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(18, 14).addBox(-1.0F, 2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(27, 15).addBox(-1.0F, -1.0F, 0.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(1.5F, 0.0F, 0.0F));

        PartDefinition rightLeg = waist.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(18, 14).mirror().addBox(-1.0F, 2.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(27, 15).mirror().addBox(0.0F, -1.0F, 0.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-1.5F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void prepareMobModel(T entity, float f, float g, float partialTick) {
        this.sleepAmount = entity.getSleepAmount(partialTick);
        this.restVariant = entity.getRestVariant();
        this.probeAmount = entity.getProbeAmount(partialTick);
        this.defendAmount = entity.getDefendAmount(partialTick);
    }

    @Override
    public void setupAnim(T entity, float f, float g, float h, float i, float j) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.head.xRot = j * (float) (Math.PI / 180.0);
        this.head.yRot = i * (float) (Math.PI / 180.0);
        this.leftLeg.xRot = Mth.cos(f * 0.6662F * 1.5F) * 1.2F * g;
        this.rightLeg.xRot = Mth.cos(f * 0.6662F * 1.5F + (float) Math.PI) * 1.2F * g;
        this.body.y -= Math.abs(Mth.cos(f * 0.6662F * 1.5F)) * 0.6F * g;

        if (this.probeAmount > 0.0F) {
            this.body.xRot += 0.25F * this.probeAmount;
            this.head.y += 1.0F * this.probeAmount;
            this.head.xRot = Mth.lerp(this.probeAmount, this.head.xRot, 1.3F + Mth.sin(h * 1.6F) * 0.1F);
        }
        if (this.defendAmount > 0.0F) {
            this.waist.y -= 1.0F * this.defendAmount;
            this.head.xRot = Mth.lerp(this.defendAmount, this.head.xRot, -0.4F);
            this.rightLeg.xRot = Mth.lerp(this.defendAmount, this.rightLeg.xRot, -0.9F + Mth.sin(h * 0.8F) * 0.4F);
        }
        float fluff = 1.0F + 0.1F * this.sleepAmount;
        this.body.xScale = fluff;
        this.body.yScale = fluff;
        this.body.zScale = fluff;
        if (this.sleepAmount > 0.0F) {
            this.waist.y += 3.5F * this.sleepAmount;
            this.head.xRot = Mth.lerp(this.sleepAmount, this.head.xRot, 0.4F);
            this.head.yRot = Mth.lerp(this.sleepAmount, this.head.yRot, this.restVariant == 0 ? 2.3F : -2.3F);
            this.leftLeg.xRot = Mth.lerp(this.sleepAmount, this.leftLeg.xRot, 1.5F);
            this.rightLeg.xRot = Mth.lerp(this.sleepAmount, this.rightLeg.xRot, 1.5F);
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
