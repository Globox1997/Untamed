package net.untamed.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.BuffaloEntity;
import net.untamed.entity.model.BuffaloModel;
import net.untamed.entity.render.feature.SleepingEyesFeatureRenderer;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BuffaloRenderer extends MobRenderer<BuffaloEntity, BuffaloModel<BuffaloEntity>> {

    private static final float ROAMER_SCALE = 1.1F;
    private static final ResourceLocation BUFFALO_LOCATION = UntamedMain.identifierOf("textures/entity/buffalo.png");

    public BuffaloRenderer(EntityRendererProvider.Context context) {
        super(context, new BuffaloModel<>(context.bakeLayer(RenderInit.BUFFALO_LAYER)), 0.4F);
        this.addLayer(new SleepingEyesFeatureRenderer<>(this, BuffaloEntity::isResting));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BuffaloEntity buffaloEntity) {
        return BUFFALO_LOCATION;
    }

    @Override
    protected void scale(BuffaloEntity buffaloEntity, PoseStack poseStack, float partialTick) {
        if (buffaloEntity.isRoamer()) {
            poseStack.scale(ROAMER_SCALE, ROAMER_SCALE, ROAMER_SCALE);
        }
    }

}
