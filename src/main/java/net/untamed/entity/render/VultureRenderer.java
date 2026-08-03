package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.VultureEntity;
import net.untamed.entity.model.VultureModel;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class VultureRenderer extends MobRenderer<VultureEntity, VultureModel<VultureEntity>> {

    private static final ResourceLocation VULTURE_LOCATION = UntamedMain.identifierOf("textures/entity/vulture.png");

    public VultureRenderer(EntityRendererProvider.Context context) {
        super(context, new VultureModel<>(context.bakeLayer(RenderInit.VULTURE_LAYER)), 0.4F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(VultureEntity buffaloEntity) {
        return VULTURE_LOCATION;
    }

}
