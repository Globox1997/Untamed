package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.HyenaEntity;
import net.untamed.entity.model.HyenaModel;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class HyenaRenderer extends MobRenderer<HyenaEntity, HyenaModel<HyenaEntity>> {

    private static final ResourceLocation HYENA_LOCATION = UntamedMain.identifierOf("textures/entity/hyena.png");

    public HyenaRenderer(EntityRendererProvider.Context context) {
        super(context, new HyenaModel<>(context.bakeLayer(RenderInit.HYENA_LAYER)), 0.9F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(HyenaEntity hyenaEntity) {
        return HYENA_LOCATION;
    }

}
