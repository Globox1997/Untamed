package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.BluewingEntity;
import net.untamed.entity.model.BluewingModel;
import net.untamed.entity.render.feature.SleepingEyesFeatureRenderer;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BluewingRenderer extends MobRenderer<BluewingEntity, BluewingModel<BluewingEntity>> {

    private static final ResourceLocation BLUEWING_LOCATION = UntamedMain.identifierOf("textures/entity/bluewing.png");

    public BluewingRenderer(EntityRendererProvider.Context context) {
        super(context, new BluewingModel<>(context.bakeLayer(RenderInit.BLUEWING_LAYER)), 0.3F);
        this.addLayer(new SleepingEyesFeatureRenderer<>(this, BluewingEntity::isAsleep));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BluewingEntity bluewingEntity) {
        return BLUEWING_LOCATION;
    }

}
