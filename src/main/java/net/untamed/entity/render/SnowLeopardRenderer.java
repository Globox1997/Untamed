package net.untamed.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.SnowLeopardEntity;
import net.untamed.entity.model.SnowLeopardModel;
import net.untamed.entity.render.feature.SleepingEyesFeatureRenderer;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class SnowLeopardRenderer extends MobRenderer<SnowLeopardEntity, SnowLeopardModel<SnowLeopardEntity>> {

    private static final ResourceLocation SNOW_LEOPARD_LOCATION = UntamedMain.identifierOf("textures/entity/snow_leopard.png");
    private static final float MODEL_SCALE = 0.8F;

    public SnowLeopardRenderer(EntityRendererProvider.Context context) {
        super(context, new SnowLeopardModel<>(context.bakeLayer(RenderInit.SNOW_LEOPARD_LAYER)), 0.7F);
        this.addLayer(new SleepingEyesFeatureRenderer<>(this, SnowLeopardEntity::isResting));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(SnowLeopardEntity snowLeopardEntity) {
        return SNOW_LEOPARD_LOCATION;
    }

    @Override
    protected void scale(SnowLeopardEntity snowLeopardEntity, PoseStack poseStack, float partialTick) {
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }
}
