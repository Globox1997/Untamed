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
import net.untamed.entity.CapybaraEntity;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class CapybaraModel<T extends CapybaraEntity> extends HierarchicalModel<T> {

    private static final float WALK_CYCLE_FREQ = 0.6662F; // vanilla quadruped cycle
    private static final float WALK_CYCLE_AMP = 1.4F;
    private static final float TAU = (float) (Math.PI * 2);
    private static final float SLOW_SHARE = 0.7F;

    private static final int SNIFF_PULSE_TICKS = 10;
    private static final int SNIFF_BURST_TICKS = 20;
    private static final int SNIFF_PERIOD = 60;
    private static final int SNIFF_SKIP_ONE_IN = 3;

    private static final int EAR_FLAP_TOTAL = 18;
    private static final int EAR_FLAP_PER = 3;
    private static final int EAR_PERIOD = 140;
    private static final int EAR_SKIP_ONE_IN = 4;
    private static final float EAR_MAX_AMPL = 0.15F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart ear_left;
    private final ModelPart ear_right;
    private final ModelPart leftFrontLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart rightBackLeg;

    public CapybaraModel(ModelPart modelPart) {
        super();
        this.root = modelPart.getChild("root");
        this.body = this.root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.head = this.body.getChild("head");
        this.ear_left = this.head.getChild("head_r1");
        this.ear_right = this.head.getChild("head_r2");
        this.leftFrontLeg = this.root.getChild("leftFrontLeg");
        this.leftBackLeg = this.root.getChild("leftBackLeg");
        this.rightFrontLeg = this.root.getChild("rightFrontLeg");
        this.rightBackLeg = this.root.getChild("rightBackLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -6.0F, 8.0F, 8.0F, 15.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.0F, -1.0F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(31, 0).addBox(-3.0F, 0.3536F, -0.3536F, 6.0F, 0.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -7.5F, 9.0F, -0.7854F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 23).addBox(-3.0F, -5.0F, -7.0F, 6.0F, 6.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.0F, -5.0F, 0.1745F, 0.0F, 0.0F));

        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(23, 28).mirror().addBox(-1.0F, -3.25F, 6.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(1.05F, 0.0F, -2.0F, 0.3927F, -0.3927F, 0.0F));

        PartDefinition head_r2 = head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(23, 28).addBox(0.0F, -3.25F, 6.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.05F, 0.0F, -2.0F, 0.3927F, 0.3927F, 0.0F));

        PartDefinition leftFrontLeg = root.addOrReplaceChild("leftFrontLeg", CubeListBuilder.create().texOffs(0, 23).addBox(-1.25F, -2.0F, -1.0F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(3.0F, -4.0F, -5.0F));

        PartDefinition leftBackLeg = root.addOrReplaceChild("leftBackLeg", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, 0.0F, -2.0F, 2.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -9.0F, 5.0F));

        PartDefinition rightFrontLeg = root.addOrReplaceChild("rightFrontLeg", CubeListBuilder.create().texOffs(0, 23).mirror().addBox(-0.75F, -2.0F, -1.0F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.0F, -4.0F, -5.0F));

        PartDefinition rightBackLeg = root.addOrReplaceChild("rightBackLeg", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.75F, 0.0F, -2.0F, 2.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-3.75F, -9.0F, 5.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float headYaw = netHeadYaw * ((float) Math.PI / 180.0F);
        float headPitchRad = Mth.clamp(headPitch * ((float) Math.PI / 180.0F), -0.6F, 0.6F);

        this.resetPose();
        this.head.yRot = headYaw;
        int entityId = entity.getId();
        float ear = earFlap(entityId, ageInTicks);
        this.ear_left.yRot = 0.3927F + ear;
        this.ear_right.yRot = -0.3927F - ear;

        if (entity.isDiving() && !entity.isRiverbed()) this.poseDiveDescent(ageInTicks, headPitchRad);
        else if (entity.isDiving()) this.poseRiverbed(limbSwing, limbSwingAmount, ageInTicks, headPitchRad);
        else if (entity.isResting() && !entity.isInWater() && entity.onGround()) this.poseSitting(entityId, ageInTicks, headYaw, headPitchRad);
        else if (entity.isInWater() && limbSwingAmount < 0.05F) this.poseFloating(ageInTicks, headPitchRad);
        else this.poseWalking(limbSwing, limbSwingAmount, headPitchRad);
    }

    private void resetPose() {
        this.head.y = -6.0F;
        this.head.z = -5F;
        this.body.y = -4.0F;
        this.ear_left.yRot = 0.3927F;
        this.ear_right.yRot = -0.3927F;
        this.rightFrontLeg.x = -3.0F; this.rightFrontLeg.y = -4.0F; this.rightFrontLeg.z = -5.0F;
        this.leftFrontLeg.x = 3.0F; this.leftFrontLeg.y = -4.0F; this.leftFrontLeg.z = -5.0F;
        this.rightBackLeg.x = -3.75F; this.rightBackLeg.y = -9.0F; this.rightBackLeg.z = 5F;
        this.leftBackLeg.x = 4F; this.leftBackLeg.y = -9.0F; this.leftBackLeg.z = 5F;
        this.body.yScale = 1; this.body.xScale = 1; this.body.zScale = 1;
        this.head.zScale = 1;
    }

    private void poseDiveDescent(float ageInTicks, float headPitchRad) {
        this.body.xRot = 0.3F;
        this.head.xRot = headPitchRad + 0.6F;
        float kick = Mth.cos(ageInTicks * 0.08F) * 0.25F;
        this.rightFrontLeg.y = -3F; this.rightFrontLeg.z = -4F;
        this.leftFrontLeg.y = -3F; this.leftFrontLeg.z = -4F;
        this.rightFrontLeg.xRot = 0.8F + kick;
        this.leftFrontLeg.xRot = 0.8F - kick;
        this.rightBackLeg.z = 3F; this.rightBackLeg.y = -10F;
        this.leftBackLeg.z = 3F; this.leftBackLeg.y = -10F;
        this.rightBackLeg.xRot = 0.8F - kick;
        this.leftBackLeg.xRot = 0.8F + kick;
    }

    private void poseRiverbed(float limbSwing, float limbSwingAmount, float ageInTicks, float headPitchRad) {
        this.body.xRot = 0.0F;
        this.head.xRot = headPitchRad;
        float blend = Mth.clamp(limbSwingAmount / 0.3F, 0.0F, 1.0F); // 0 = still, 1 = moving
        float sway = Mth.cos(ageInTicks * 0.05F) * 0.05F;
        float amp = 0.45F * Math.max(0.7F, limbSwingAmount);
        float base = limbSwing * 0.75F;
        this.rightFrontLeg.xRot = Mth.lerp(blend, 0.8F + sway, Mth.cos(kickPhase(base)) * amp);
        this.leftFrontLeg.xRot = Mth.lerp(blend, 0.8F - sway, Mth.cos(kickPhase(base + (float) (Math.PI / 2))) * amp);
        this.rightBackLeg.xRot = Mth.lerp(blend, 0.8F - sway, Mth.cos(kickPhase(base + (float) Math.PI)) * amp);
        this.leftBackLeg.xRot = Mth.lerp(blend, 0.8F + sway, Mth.cos(kickPhase(base + (float) (Math.PI * 1.5))) * amp);
    }

    private static float kickPhase(float rawPhase) {
        float t = (rawPhase / TAU) % 1.0F;
        if (t < SLOW_SHARE) return (t / SLOW_SHARE) * Mth.PI;
        return Mth.PI + ((t - SLOW_SHARE) / (1.0F - SLOW_SHARE)) * Mth.PI;
    }

    private void poseSitting(int entityId, float ageInTicks, float headYaw, float headPitchRad) {
        this.body.xRot = -0.50F;
        this.head.xRot = headPitchRad + 0.3F;
        this.head.yRot = Mth.clamp(headYaw, -0.5F, 0.5F);
        this.head.y = -7F;
        this.body.y = -3F;
        this.rightFrontLeg.xRot = 0F;
        this.leftFrontLeg.xRot = 0F;
        this.rightBackLeg.y = -4F; this.rightBackLeg.z = 7F; this.rightBackLeg.xRot = -1.2F;
        this.leftBackLeg.y = -4F; this.leftBackLeg.z = 7F; this.leftBackLeg.xRot = -1.2F;

        this.head.zScale = 1 - 0.1F * sniffPulse(entityId, ageInTicks); // olisqueo
        // this.body.xScale = 1 + Mth.cos(ageInTicks * 0.05F) * 0.05F; // respiración
    }

    private void poseFloating(float ageInTicks, float headPitchRad) {
        this.body.xRot = -0.1F;
        this.head.xRot = headPitchRad - 0.15F;
        float paddle = Mth.cos(ageInTicks * 0.25F) * 0.35F;
        this.leftBackLeg.y = -8.5F; this.leftBackLeg.z = 6F;
        this.rightBackLeg.y = -8.5F; this.rightBackLeg.z = 6F;
        this.rightFrontLeg.xRot = -0.6F + paddle;
        this.leftFrontLeg.xRot = -0.6F - paddle;
        this.rightBackLeg.xRot = -paddle;
        this.leftBackLeg.xRot = paddle;
    }

    private void poseWalking(float limbSwing, float limbSwingAmount, float headPitchRad) {
        this.body.xRot = 0.0F;
        this.head.xRot = headPitchRad;
        this.rightBackLeg.xRot = Mth.cos(limbSwing * WALK_CYCLE_FREQ) * WALK_CYCLE_AMP * limbSwingAmount;
        this.leftBackLeg.xRot = Mth.cos(limbSwing * WALK_CYCLE_FREQ + (float) Math.PI) * WALK_CYCLE_AMP * limbSwingAmount;
        this.rightFrontLeg.xRot = Mth.cos(limbSwing * WALK_CYCLE_FREQ + (float) Math.PI) * WALK_CYCLE_AMP * limbSwingAmount;
        this.leftFrontLeg.xRot = Mth.cos(limbSwing * WALK_CYCLE_FREQ) * WALK_CYCLE_AMP * limbSwingAmount;
    }

    private static float earFlap(int entityId, float ageInTicks) {
        float t = ageInTicks + entityId * 13F; // use number prime and that it does not divide EAR_PERIOD
        float s = t % EAR_PERIOD;
        if (s >= EAR_FLAP_TOTAL) return 0F;
        if (skipThisEvent(t)) return 0F;
        float flap = 1F - (int) (s / EAR_FLAP_PER) / 3F;
        return EAR_MAX_AMPL * flap * Mth.sin((s % EAR_FLAP_PER) / (float) EAR_FLAP_PER * (float) Math.PI);
    }

    private static boolean skipThisEvent(float ageInTicks) {
        int event = (int) (ageInTicks / CapybaraModel.EAR_PERIOD);
        return event * 7919 % CapybaraModel.EAR_SKIP_ONE_IN == 0;
    }

    private static float sniffPulse(int entityId, float ageInTicks) {
        float t = ageInTicks + entityId * 13F; // idem but SNIFF_PERIOD
        float s = t % SNIFF_PERIOD;
        if (s >= SNIFF_BURST_TICKS) return 0F;
        if (skipThisBurst(ageInTicks)) return 0F;
        return triangle(s % SNIFF_PULSE_TICKS);
    }

    private static boolean skipThisBurst(float ageInTicks) {
        int burst = (int) (ageInTicks / SNIFF_PERIOD);
        return burst * 7919 % SNIFF_SKIP_ONE_IN == 0;
    }

    private static float triangle(float t) { return t < 5 ? t / 5F : (10 - t) / 5F; }

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
    public @NotNull ModelPart root() { return this.root; }
}
