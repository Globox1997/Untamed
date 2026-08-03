package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.BlackBearEntity;
import net.untamed.entity.model.BlackBearModel;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BlackBearRenderer extends MobRenderer<BlackBearEntity, BlackBearModel<BlackBearEntity>> {

    private static final ResourceLocation BLACK_BEAR_LOCATION = UntamedMain.identifierOf("textures/entity/black_bear.png");

    public BlackBearRenderer(EntityRendererProvider.Context context) {
        super(context, new BlackBearModel<>(context.bakeLayer(RenderInit.BLACK_BEAR_LAYER)), 0.9F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BlackBearEntity blackBearEntity) {
        return BLACK_BEAR_LOCATION;
    }

}
