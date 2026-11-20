package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.GazelleEntity;
import net.untamed.entity.model.GazelleModel;
import net.untamed.entity.render.feature.SleepingEyesFeatureRenderer;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class GazelleRenderer extends MobRenderer<GazelleEntity, GazelleModel<GazelleEntity>> {

    private static final ResourceLocation GAZELLE_LOCATION = UntamedMain.identifierOf("textures/entity/gazelle.png");

    public GazelleRenderer(EntityRendererProvider.Context context) {
        super(context, new GazelleModel<>(context.bakeLayer(RenderInit.GAZELLE_LAYER)), 0.4F);
        this.addLayer(new SleepingEyesFeatureRenderer<>(this, GazelleEntity::isResting));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(GazelleEntity gazelleEntity) {
        return GAZELLE_LOCATION;
    }

}
