package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.KiwiEntity;
import net.untamed.entity.model.KiwiModel;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class KiwiRenderer extends MobRenderer<KiwiEntity, KiwiModel<KiwiEntity>> {

    private static final ResourceLocation KIWI_LOCATION = UntamedMain.identifierOf("textures/entity/kiwi.png");

    public KiwiRenderer(EntityRendererProvider.Context context) {
        super(context, new KiwiModel<>(context.bakeLayer(RenderInit.KIWI_LAYER)), 0.4F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(KiwiEntity kiwiEntity) {
        return KIWI_LOCATION;
    }

}
