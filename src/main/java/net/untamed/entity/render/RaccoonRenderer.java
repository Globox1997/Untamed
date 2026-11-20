package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.RaccoonEntity;
import net.untamed.entity.model.RaccoonModel;
import net.untamed.entity.render.feature.RaccoonHeldItemLayer;
import net.untamed.entity.render.feature.SleepingEyesFeatureRenderer;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class RaccoonRenderer extends MobRenderer<RaccoonEntity, RaccoonModel<RaccoonEntity>> {

    private static final ResourceLocation RACCOON_LOCATION = UntamedMain.identifierOf("textures/entity/raccoon.png");

    public RaccoonRenderer(EntityRendererProvider.Context context) {
        super(context, new RaccoonModel<>(context.bakeLayer(RenderInit.RACCOON_LAYER)), 0.4F);
        this.addLayer(new SleepingEyesFeatureRenderer<>(this, RaccoonEntity::isResting));
        this.addLayer(new RaccoonHeldItemLayer(this, context.getItemInHandRenderer()));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(RaccoonEntity raccoonEntity) {
        return RACCOON_LOCATION;
    }

}
