package net.untamed.entity.render.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.untamed.entity.RaccoonEntity;
import net.untamed.entity.model.RaccoonModel;

@Environment(EnvType.CLIENT)
public class RaccoonHeldItemLayer extends RenderLayer<RaccoonEntity, RaccoonModel<RaccoonEntity>> {

    private static final float ITEM_SCALE = 0.5F;

    private final ItemInHandRenderer itemInHandRenderer;

    public RaccoonHeldItemLayer(RenderLayerParent<RaccoonEntity, RaccoonModel<RaccoonEntity>> renderLayerParent, ItemInHandRenderer itemInHandRenderer) {
        super(renderLayerParent);
        this.itemInHandRenderer = itemInHandRenderer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, RaccoonEntity raccoon, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack stack = raccoon.getItemBySlot(EquipmentSlot.MAINHAND);
        if (stack.isEmpty() || raccoon.isInvisible()) {
            return;
        }
        poseStack.pushPose();
        if (raccoon.isBaby()) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(0.0F, 1.5F, 0.0F);
        }
        this.getParentModel().translateToHead(poseStack);
        poseStack.translate(0.0F, 1.5F / 16.0F, -6.5F / 16.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        this.itemInHandRenderer.renderItem(raccoon, stack, ItemDisplayContext.GROUND, false, poseStack, multiBufferSource, light);
        poseStack.popPose();
    }
}
