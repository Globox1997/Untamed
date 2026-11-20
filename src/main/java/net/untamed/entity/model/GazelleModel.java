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
import net.untamed.entity.GazelleEntity;

@Environment(EnvType.CLIENT)
public class GazelleModel<T extends GazelleEntity> extends HierarchicalModel<T> {

    private static final float NECK_GRAZE = 1.9F;
    private static final float HEAD_GRAZE = -0.3F;
    private static final float TAIL_REST = 0.3F;
    private static final float TAIL_RAISED = 1.5F;
    private static final float GALLOP_FREQ = 0.5F;
    private static final float GALLOP_AMPLITUDE = 1.1F;
    private static final float GALLOP_LEAD = 0.4F;

    private final ModelPart root;
    private final ModelPart tail;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart horns;
    private final ModelPart rightEar;

    private float grazeAmount;
    private float restAmount;
    private float alertAmount;
    private int restVariant;
    private float scanAmount;
    private float stotAmount;
    private float alarmAmount;
    private float fleeAmount;

    public GazelleModel(ModelPart root) {
        super();
        this.root = root.getChild("root");
        this.tail = this.root.getChild("tail");
        this.leftBackLeg = this.root.getChild("leftBackLeg");
        this.rightBackLeg = this.root.getChild("rightBackLeg");
        this.leftFrontLeg = this.root.getChild("leftFrontLeg");
        this.rightFrontLeg = this.root.getChild("rightFrontLeg");
        this.neck = this.root.getChild("neck");
        this.head = this.neck.getChild("head");
        this.leftEar = this.head.getChild("leftEar");
        this.horns = this.head.getChild("horns");
        this.rightEar = this.head.getChild("rightEar");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -15.0F, -6.0F, 6.0F, 5.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(34, 31).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -14.0F, 7.0F));

        PartDefinition leftBackLeg = root.addOrReplaceChild("leftBackLeg", CubeListBuilder.create().texOffs(0, 27).addBox(-1.0F, 0.0F, -1.5F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(32, 18).addBox(-1.0F, 4.0F, 0.5F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -10.0F, 5.5F));

        PartDefinition rightBackLeg = root.addOrReplaceChild("rightBackLeg", CubeListBuilder.create().texOffs(32, 18).mirror().addBox(-1.0F, 4.0F, 0.5F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(0, 27).mirror().addBox(-1.0F, 0.0F, -1.5F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.0F, -10.0F, 5.5F));

        PartDefinition leftFrontLeg = root.addOrReplaceChild("leftFrontLeg", CubeListBuilder.create().texOffs(26, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -10.0F, -5.0F));

        PartDefinition rightFrontLeg = root.addOrReplaceChild("rightFrontLeg", CubeListBuilder.create().texOffs(26, 26).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.0F, -10.0F, -5.0F));

        PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(18, 18).addBox(-2.0F, -5.0F, -1.5F, 4.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -15.0F, -4.5F));

        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-2.0F, -4.0F, -3.5F, 4.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(34, 26).addBox(-1.0F, -2.0F, -6.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.0F, 0.0F));

        PartDefinition leftEar = head.addOrReplaceChild("leftEar", CubeListBuilder.create().texOffs(0, 35).addBox(0.0F, -1.5F, -1.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, -3.5F, 1.5F));

        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create().texOffs(12, 27).addBox(1.5F, -6.0F, -0.5F, 0.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(8, 35).mirror().addBox(-2.0F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(8, 35).addBox(1.0F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 27).mirror().addBox(-1.5F, -6.0F, -0.5F, 0.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, -4.0F, 0.0F));

        PartDefinition rightEar = head.addOrReplaceChild("rightEar", CubeListBuilder.create().texOffs(0, 35).mirror().addBox(-3.0F, -1.5F, -1.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-2.0F, -3.5F, 1.5F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void prepareMobModel(T entity, float f, float g, float partialTick) {
        this.grazeAmount = entity.getGrazeAmount(partialTick);
        this.restAmount = entity.getRestAmount(partialTick);
        this.alertAmount = entity.getAlertAmount(partialTick);
        this.restVariant = entity.getRestVariant();
        this.scanAmount = entity.getScanAmount(partialTick);
        this.stotAmount = entity.getStotAmount(partialTick);
        this.alarmAmount = entity.getAlarmAmount(partialTick);
        this.fleeAmount = entity.getFleeAmount(partialTick);
    }

    @Override
    public void setupAnim(T entity, float f, float g, float h, float i, float j) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.horns.visible = !entity.isBaby();
        this.head.xRot = j * (float) (Math.PI / 180.0);
        this.head.yRot = i * (float) (Math.PI / 180.0);

        this.animateLegs(f, g);
        this.animateTail(f, g, h);
        this.animateEars(entity, h);

        float graze = this.grazeAmount * (1.0F - this.scanAmount);
        if (graze > 0.0F) {
            this.neck.xRot = Mth.lerp(graze, this.neck.xRot, NECK_GRAZE);
            this.neck.y += 3.0F * graze;
            this.neck.z -= 1.0F * graze;
            this.head.xRot = Mth.lerp(graze, this.head.xRot, HEAD_GRAZE + Mth.sin(h * 0.4F) * 0.05F);
            this.head.yRot = Mth.lerp(graze, this.head.yRot, 0.0F);
        }
        float alert = Math.max(this.alertAmount, Math.max(this.scanAmount * this.grazeAmount, this.alarmAmount));
        if (alert > 0.0F) {
            this.neck.xRot -= 0.2F * alert;
            this.head.xRot = Mth.lerp(alert, this.head.xRot, Math.min(this.head.xRot, -0.1F));
        }
        if (this.stotAmount > 0.0F) {
            float s = this.stotAmount;
            this.leftFrontLeg.xRot = Mth.lerp(s, this.leftFrontLeg.xRot, -0.05F);
            this.rightFrontLeg.xRot = Mth.lerp(s, this.rightFrontLeg.xRot, -0.05F);
            this.leftBackLeg.xRot = Mth.lerp(s, this.leftBackLeg.xRot, 0.05F);
            this.rightBackLeg.xRot = Mth.lerp(s, this.rightBackLeg.xRot, 0.05F);
            this.neck.xRot = Mth.lerp(s, this.neck.xRot, -0.15F);
            this.head.xRot = Mth.lerp(s, this.head.xRot, 0.1F);
        }
        if (this.restAmount > 0.0F) {
            this.poseResting();
        }
    }

    private void animateLegs(float f, float g) {
        float walkA = Mth.cos(f * 0.6662F) * 1.4F * g;
        float walkB = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        float gallop = Math.max(this.fleeAmount, Mth.clamp((g - 0.6F) * 2.5F, 0.0F, 1.0F));
        float frontPhase = f * GALLOP_FREQ;
        float backPhase = frontPhase + (float) Math.PI * 0.8F;
        this.rightFrontLeg.xRot = Mth.lerp(gallop, walkB, Mth.cos(frontPhase) * GALLOP_AMPLITUDE * g);
        this.leftFrontLeg.xRot = Mth.lerp(gallop, walkA, Mth.cos(frontPhase + GALLOP_LEAD) * GALLOP_AMPLITUDE * g);
        this.rightBackLeg.xRot = Mth.lerp(gallop, walkA, Mth.cos(backPhase) * GALLOP_AMPLITUDE * g);
        this.leftBackLeg.xRot = Mth.lerp(gallop, walkB, Mth.cos(backPhase + GALLOP_LEAD) * GALLOP_AMPLITUDE * g);
    }

    private void animateTail(float f, float g, float h) {
        float flickWindow = Mth.clamp(Mth.sin(h * 0.05F) * 4.0F - 3.0F, 0.0F, 1.0F);
        this.tail.xRot = TAIL_REST;
        this.tail.yRot = Mth.sin(h * 1.6F) * 0.6F * flickWindow + Mth.cos(f * 0.6662F) * 0.2F * g;
        float raised = Math.max(this.alarmAmount, Math.max(this.fleeAmount, this.stotAmount));
        if (raised > 0.0F) {
            this.tail.xRot = Mth.lerp(raised, this.tail.xRot, TAIL_RAISED);
            this.tail.yRot += Mth.sin(h * 2.4F) * 0.5F * this.alarmAmount;
        }
    }

    private void animateEars(T entity, float h) {
        float phase = entity.getId() * 0.7F;
        float leftFlick = Mth.clamp(Mth.sin(h * 0.09F + phase) * 6.0F - 5.0F, 0.0F, 1.0F);
        float rightFlick = Mth.clamp(Mth.sin(h * 0.09F + phase + 2.0F) * 6.0F - 5.0F, 0.0F, 1.0F);
        this.leftEar.zRot += 0.4F * leftFlick * Mth.sin(h * 1.5F);
        this.rightEar.zRot -= 0.4F * rightFlick * Mth.sin(h * 1.5F);
        float alert = Math.max(this.alertAmount, this.alarmAmount);
        this.leftEar.zRot -= 0.3F * alert;
        this.rightEar.zRot += 0.3F * alert;
    }

    private void poseResting() {
        float r = this.restAmount;
        float side = this.restVariant == 0 ? 1.0F : -1.0F;
        this.root.y += 7.5F * r;
        this.root.zRot = Mth.lerp(r, this.root.zRot, 0.08F * side);
        this.leftFrontLeg.xRot = Mth.lerp(r, this.leftFrontLeg.xRot, 1.5F);
        this.rightFrontLeg.xRot = Mth.lerp(r, this.rightFrontLeg.xRot, 1.5F);
        this.leftBackLeg.xRot = Mth.lerp(r, this.leftBackLeg.xRot, -1.5F);
        this.rightBackLeg.xRot = Mth.lerp(r, this.rightBackLeg.xRot, -1.5F);
        this.neck.xRot = Mth.lerp(r, this.neck.xRot, 0.3F);
        this.neck.yRot = Mth.lerp(r, this.neck.yRot, 0.5F * side);
        this.head.xRot = Mth.lerp(r, this.head.xRot, 0.2F);
        this.head.yRot = Mth.lerp(r, this.head.yRot, 0.0F);
        this.tail.xRot = Mth.lerp(r, this.tail.xRot, 0.2F);
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
