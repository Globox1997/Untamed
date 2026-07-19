package net.untamed.entity.render.feature;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.untamed.UntamedMain;
import net.untamed.entity.LionEntity;
import net.untamed.entity.LionessEntity;

@Environment(EnvType.CLIENT)
public class SleepingEyesFeatureRenderer extends RenderLayer {

    public SleepingEyesFeatureRenderer(RenderLayerParent renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, Entity entity, float f, float g, float h, float j, float k, float l) {
        if ((entity instanceof LionEntity lionEntity && lionEntity.isSleeping())||(entity instanceof LionessEntity lionessEntity && lionessEntity.isSleeping())) {
                VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(UntamedMain.identifierOf("textures/entity/" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath() + "_sleeping_eyes.png")));
                RenderSystem.enableBlend();
                this.getParentModel().renderToBuffer(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY, -1);
                RenderSystem.disableBlend();

        }
    }
}

