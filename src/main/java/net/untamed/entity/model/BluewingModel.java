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
import net.untamed.entity.BluewingEntity;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BluewingModel<T extends BluewingEntity> extends HierarchicalModel<T> {

    private static final float WING_FOLD_YAW = 1.45F;
    private static final float WINGTIP_FOLD_YAW = 2.99F;
    private static final float WINGTIP_FOLD_DROP = 0.6F;
    private static final float WING_FLAP = 0.8F;
    private static final float WINGTIP_FLAP = 0.5F;
    private static final float LEG_TUCK = 0.9F;
    private static final float SLEEP_HEAD_YAW = 2.6F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart right_wing;
    private final ModelPart right_wingtip;
    private final ModelPart left_leg;
    private final ModelPart right_leg;
    private final ModelPart left_wing;
    private final ModelPart left_wingtip;

    private float flyAmount;
    private float sleepAmount;
    private float beatAmount;
    private float frozenAmount;

    public BluewingModel(ModelPart root) {
        super();
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.head = this.body.getChild("head");
        this.right_wing = this.body.getChild("right_wing");
        this.right_wingtip = this.right_wing.getChild("right_wingtip");
        this.left_leg = this.body.getChild("left_leg");
        this.right_leg = this.body.getChild("right_leg");
        this.left_wing = this.body.getChild("left_wing");
        this.left_wingtip = this.left_wing.getChild("left_wingtip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 21.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 21).addBox(-2.0F, -2.6667F, 2.3333F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 6).addBox(-2.0F, -2.6667F, -3.6667F, 4.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.3333F, 2.6667F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 6).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -1.6667F, 6.3333F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(26, 0).addBox(-1.5F, -0.5F, -6.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(16, 21).addBox(-2.0F, -1.5F, -3.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, -1.1667F, -3.6667F));

        PartDefinition right_wing = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -0.5F, -1.0F, 8.0F, 1.0F, 5.0F, new CubeDeformation(0.022F)), PartPose.offset(2.0F, -2.1667F, -2.6667F));

        PartDefinition right_wingtip = right_wing.addOrReplaceChild("right_wingtip", CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, -0.5F, -1.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.012F)), PartPose.offset(8.0F, 0.0F, 0.0F));

        PartDefinition left_leg = body.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(23, 11).addBox(-1.5F, -0.1F, -3.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.5F, 1.3333F, 2.3333F));

        PartDefinition right_leg = body.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(23, 11).mirror().addBox(-1.5F, -0.1F, -3.0F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(1.5F, 1.3333F, 2.3333F));

        PartDefinition left_wing = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-8.0F, -0.5F, -1.0F, 8.0F, 1.0F, 5.0F, new CubeDeformation(0.02F)).mirror(false), PartPose.offset(-2.0F, -2.1667F, -2.6667F));

        PartDefinition left_wingtip = left_wing.addOrReplaceChild("left_wingtip", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-8.0F, -0.5F, -1.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.01F)).mirror(false), PartPose.offset(-8.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.flyAmount = entity.getFlyAmount(partialTick);
        this.sleepAmount = entity.getSleepAmount(partialTick);
        this.beatAmount = entity.getBeatAmount(partialTick);
        this.frozenAmount = entity.getFrozenAmount(partialTick);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float headPitchRad = Mth.clamp(headPitch * ((float) Math.PI / 180.0F), -0.6F, 0.6F);
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        this.head.xRot = headPitchRad;
        this.body.xScale = 1.0F;
        this.body.yScale = 1.0F;

        float fold = 1.0F - this.flyAmount;
        float flap = Mth.sin(ageInTicks * 1.1F) * WING_FLAP * this.flyAmount;
        float tipFlap = Mth.sin(ageInTicks * 1.1F - 0.6F) * WINGTIP_FLAP * this.flyAmount;
        this.right_wing.yRot = -WING_FOLD_YAW * fold;
        this.left_wing.yRot = WING_FOLD_YAW * fold;
        this.right_wing.zRot = flap;
        this.left_wing.zRot = -flap;
        this.right_wingtip.yRot = WINGTIP_FOLD_YAW * fold;
        this.left_wingtip.yRot = -WINGTIP_FOLD_YAW * fold;
        this.right_wingtip.y += WINGTIP_FOLD_DROP * fold;
        this.left_wingtip.y += WINGTIP_FOLD_DROP * fold;
        this.right_wingtip.zRot = tipFlap;
        this.left_wingtip.zRot = -tipFlap;

        this.left_leg.xRot = Mth.cos(limbSwing * 1.2F) * 0.8F * limbSwingAmount * fold + LEG_TUCK * this.flyAmount;
        this.right_leg.xRot = Mth.cos(limbSwing * 1.2F + (float) Math.PI) * 0.8F * limbSwingAmount * fold + LEG_TUCK * this.flyAmount;
        this.tail.xRot = (Mth.sin(ageInTicks * 0.1F) * 0.05F - 0.15F) * fold * (1.0F - this.frozenAmount);

        if (this.beatAmount > 0.0F) {
            float b = this.beatAmount;
            this.head.yRot = Mth.lerp(b, this.head.yRot, Mth.sin(ageInTicks * 1.2F) * 0.9F);
            this.head.xRot = Mth.lerp(b, this.head.xRot, 0.3F);
            this.body.yRot = Mth.sin(ageInTicks * 1.2F) * 0.15F * b;
        }
        if (this.sleepAmount > 0.0F) {
            float s = this.sleepAmount;
            this.head.yRot = Mth.lerp(s, this.head.yRot, SLEEP_HEAD_YAW);
            this.head.xRot = Mth.lerp(s, this.head.xRot, 0.3F);
            float fluff = 1.0F + 0.08F * s;
            this.body.xScale = fluff;
            this.body.yScale = fluff;
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
    public @NotNull ModelPart root() {
        return this.root;
    }

}
