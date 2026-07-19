package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.CapybaraEntity;
import net.untamed.entity.model.CapybaraModel;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class CapybaraRenderer extends MobRenderer<CapybaraEntity, CapybaraModel<CapybaraEntity>> {

    private static final ResourceLocation CAPYBARA_LOCATION = UntamedMain.identifierOf("textures/entity/capybara.png");

    public CapybaraRenderer(EntityRendererProvider.Context context) {
        super(context, new CapybaraModel<>(context.bakeLayer(RenderInit.CAPYBARA_LAYER)), 0.9F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(CapybaraEntity capybaraEntity) {
        return CAPYBARA_LOCATION;
    }

}
