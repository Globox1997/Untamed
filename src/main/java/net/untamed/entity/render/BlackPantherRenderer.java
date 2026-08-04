package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.BlackPantherEntity;
import net.untamed.entity.model.BlackPantherModel;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BlackPantherRenderer extends MobRenderer<BlackPantherEntity, BlackPantherModel<BlackPantherEntity>> {

    private static final ResourceLocation BLACK_PANTHER_LOCATION = UntamedMain.identifierOf("textures/entity/black_panther.png");

    public BlackPantherRenderer(EntityRendererProvider.Context context) {
        super(context, new BlackPantherModel<>(context.bakeLayer(RenderInit.BLACK_PANTHER_LAYER)), 0.9F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BlackPantherEntity blackPantherEntity) {
        return BLACK_PANTHER_LOCATION;
    }

}
