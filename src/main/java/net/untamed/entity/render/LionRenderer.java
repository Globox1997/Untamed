package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.LionEntity;
import net.untamed.entity.model.LionModel;
import net.untamed.entity.render.feature.SleepingEyesFeatureRenderer;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class LionRenderer extends MobRenderer<LionEntity, LionModel<LionEntity>> {

    private static final ResourceLocation LION_LOCATION = UntamedMain.identifierOf("textures/entity/lion.png");

    public LionRenderer(EntityRendererProvider.Context context) {
        super(context, new LionModel<>(context.bakeLayer(RenderInit.LION_LAYER)), 0.9F);
        this.addLayer(new SleepingEyesFeatureRenderer<>(this, LionEntity::isSleeping));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(LionEntity lionEntity) {
        return LION_LOCATION;
    }

}
