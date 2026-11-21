package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.QuetzalEntity;
import net.untamed.entity.model.QuetzalModel;
import net.untamed.entity.render.feature.SleepingEyesFeatureRenderer;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class QuetzalRenderer extends MobRenderer<QuetzalEntity, QuetzalModel<QuetzalEntity>> {

    private static final ResourceLocation QUETZAL_LOCATION = UntamedMain.identifierOf("textures/entity/quetzal.png");

    public QuetzalRenderer(EntityRendererProvider.Context context) {
        super(context, new QuetzalModel<>(context.bakeLayer(RenderInit.QUETZAL_LAYER)), 0.3F);
        this.addLayer(new SleepingEyesFeatureRenderer<>(this, QuetzalEntity::isAsleep));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(QuetzalEntity quetzalEntity) {
        return QUETZAL_LOCATION;
    }

}
