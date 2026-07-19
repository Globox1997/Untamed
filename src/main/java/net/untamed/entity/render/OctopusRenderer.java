package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.OctopusEntity;
import net.untamed.entity.model.OctopusModel;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class OctopusRenderer extends MobRenderer<OctopusEntity, OctopusModel<OctopusEntity>> {

    private static final ResourceLocation OCTOPUS_LOCATION = UntamedMain.identifierOf("textures/entity/octopus.png");

    public OctopusRenderer(EntityRendererProvider.Context context) {
        super(context, new OctopusModel<>(context.bakeLayer(RenderInit.OCTOPUS_LAYER)), 0.9F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(OctopusEntity octopusEntity) {
        return OCTOPUS_LOCATION;
    }

}
