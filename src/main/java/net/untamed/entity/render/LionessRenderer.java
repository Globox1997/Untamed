package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.LionessEntity;
import net.untamed.entity.model.LionessModel;
import net.untamed.entity.render.feature.SleepingEyesFeatureRenderer;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class LionessRenderer extends MobRenderer<LionessEntity, LionessModel<LionessEntity>> {

    private static final ResourceLocation LIONESS_LOCATION = UntamedMain.identifierOf("textures/entity/lioness.png");

    public LionessRenderer(EntityRendererProvider.Context context) {
        super(context, new LionessModel<>(context.bakeLayer(RenderInit.LIONESS_LAYER)), 0.9F);
        this.addLayer(new SleepingEyesFeatureRenderer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(LionessEntity lionessEntity) {
        return LIONESS_LOCATION;
    }

}
