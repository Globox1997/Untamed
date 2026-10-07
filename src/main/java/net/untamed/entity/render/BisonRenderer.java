package net.untamed.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.BisonEntity;
import net.untamed.entity.model.BisonModel;
import net.untamed.entity.render.feature.SleepingEyesFeatureRenderer;
import net.untamed.init.RenderInit;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class BisonRenderer extends MobRenderer<BisonEntity, BisonModel<BisonEntity>> {

    private static final float ROAMER_SCALE = 1.1F;
    private static final ResourceLocation BISON_LOCATION = UntamedMain.identifierOf("textures/entity/bison.png");

    public BisonRenderer(EntityRendererProvider.Context context) {
        super(context, new BisonModel<>(context.bakeLayer(RenderInit.BISON_LAYER)), 0.9F);
        this.addLayer(new SleepingEyesFeatureRenderer<>(this, BisonEntity::isResting));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(BisonEntity bisonEntity) {
        return BISON_LOCATION;
    }

    @Override
    protected void scale(BisonEntity bisonEntity, PoseStack poseStack, float partialTick) {
        if (bisonEntity.isRoamer()) {
            poseStack.scale(ROAMER_SCALE, ROAMER_SCALE, ROAMER_SCALE);
        }
    }

}
