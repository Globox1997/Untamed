package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.BisonEntity;
import net.untamed.entity.model.BisonModel;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BisonRenderer extends MobRenderer<BisonEntity, BisonModel<BisonEntity>> {

    private static final ResourceLocation BISON_LOCATION = UntamedMain.identifierOf("textures/entity/bison.png");

    public BisonRenderer(EntityRendererProvider.Context context) {
        super(context, new BisonModel<>(context.bakeLayer(RenderInit.BISON_LAYER)), 0.9F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BisonEntity bisonEntity) {
        return BISON_LOCATION;
    }

}
