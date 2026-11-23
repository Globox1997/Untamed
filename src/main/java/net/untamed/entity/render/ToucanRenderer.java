package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.ToucanEntity;
import net.untamed.entity.model.ToucanModel;
import net.untamed.init.RenderInit;

@Environment(EnvType.CLIENT)
public class ToucanRenderer extends MobRenderer<ToucanEntity, ToucanModel<ToucanEntity>> {

    private static final ResourceLocation TEXTURE = UntamedMain.identifierOf("textures/entity/toucan.png");

    public ToucanRenderer(EntityRendererProvider.Context context) {
        super(context, new ToucanModel<>(context.bakeLayer(RenderInit.TOUCAN_LAYER)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(ToucanEntity entity) {
        return TEXTURE;
    }
}
