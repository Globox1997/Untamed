package net.untamed.entity.render.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.BlackPantherEntity;
import net.untamed.entity.model.BlackPantherModel;

// Eye shine: in the dark the eyes reflect light and glow
@Environment(EnvType.CLIENT)
public class PantherEyesLayer extends RenderLayer<BlackPantherEntity, BlackPantherModel<BlackPantherEntity>> {

    private static final ResourceLocation EYES_TEXTURE = UntamedMain.identifierOf("textures/entity/black_panther_eyes.png");
    private static final int MAX_LIGHT_FOR_SHINE = 6;

    public PantherEyesLayer(RenderLayerParent<BlackPantherEntity, BlackPantherModel<BlackPantherEntity>> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, BlackPantherEntity panther, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (panther.isInvisible() || panther.isResting() || panther.level().getMaxLocalRawBrightness(panther.blockPosition().above()) > MAX_LIGHT_FOR_SHINE) {
            return;
        }
        this.getParentModel().renderToBuffer(poseStack, multiBufferSource.getBuffer(RenderType.eyes(EYES_TEXTURE)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
    }
}
