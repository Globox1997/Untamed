package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.BuffaloEntity;
import net.untamed.entity.model.BuffaloModel;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BuffaloRenderer extends MobRenderer<BuffaloEntity, BuffaloModel<BuffaloEntity>> {

    private static final ResourceLocation BUFFALO_LOCATION = UntamedMain.identifierOf("textures/entity/buffalo.png");

    public BuffaloRenderer(EntityRendererProvider.Context context) {
        super(context, new BuffaloModel<>(context.bakeLayer(RenderInit.BUFFALO_LAYER)), 0.4F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BuffaloEntity buffaloEntity) {
        return BUFFALO_LOCATION;
    }

}
