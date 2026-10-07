package net.untamed.entity.render.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.untamed.UntamedMain;
import net.untamed.entity.OctopusEntity;
import net.untamed.entity.model.OctopusModel;

@Environment(EnvType.CLIENT)
public class OctopusCamoLayer extends RenderLayer<OctopusEntity, OctopusModel<OctopusEntity>> {

    private static final ResourceLocation CAMO_TEXTURE = UntamedMain.identifierOf("textures/entity/octopus_camo.png");

    public OctopusCamoLayer(RenderLayerParent<OctopusEntity, OctopusModel<OctopusEntity>> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, OctopusEntity octopus, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        float amount = octopus.getCamoAmount(partialTick);
        if (amount < 0.01F || octopus.isInvisible()) {
            return;
        }
        int color = FastColor.ARGB32.colorFromFloat(amount, octopus.getCamoRed(partialTick), octopus.getCamoGreen(partialTick), octopus.getCamoBlue(partialTick));
        this.getParentModel().renderOverlay(poseStack, multiBufferSource.getBuffer(RenderType.entityTranslucent(CAMO_TEXTURE)), light,
                LivingEntityRenderer.getOverlayCoords(octopus, 0.0F), color);
    }
}
