package net.untamed.entity.render.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.untamed.UntamedMain;

import java.util.function.Function;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public class SleepingEyesFeatureRenderer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final Function<EntityType<?>, ResourceLocation> TEXTURES = Util.memoize(
            type -> UntamedMain.identifierOf("textures/entity/" + BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + "_sleeping_eyes.png"));

    private final Predicate<T> eyesClosed;

    public SleepingEyesFeatureRenderer(RenderLayerParent<T, M> renderLayerParent, Predicate<T> eyesClosed) {
        super(renderLayerParent);
        this.eyesClosed = eyesClosed;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || !this.eyesClosed.test(entity)) {
            return;
        }
        this.getParentModel().renderToBuffer(poseStack, multiBufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURES.apply(entity.getType()))),
                light, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), -1);
    }
}
