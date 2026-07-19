package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.RhinoEntity;
import net.untamed.entity.model.RhinoModel;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class RhinoRenderer extends MobRenderer<RhinoEntity, RhinoModel<RhinoEntity>> {

    private static final ResourceLocation RHINO_LOCATION = UntamedMain.identifierOf("textures/entity/rhino.png");

    public RhinoRenderer(EntityRendererProvider.Context context) {
        super(context, new RhinoModel<>(context.bakeLayer(RenderInit.RHINO_LAYER)), 0.9F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(RhinoEntity rhinoEntity) {
        return RHINO_LOCATION;
    }

}
