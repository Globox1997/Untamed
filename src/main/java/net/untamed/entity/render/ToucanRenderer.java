package net.untamed.entity.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;
import net.untamed.UntamedMain;
import net.untamed.entity.ToucanEntity;
import net.untamed.entity.model.ToucanEntityModel;
import net.untamed.init.RenderInit;

@Environment(EnvType.CLIENT)
public class ToucanRenderer extends MobEntityRenderer<ToucanEntity, ToucanEntityModel<ToucanEntity>> {

    private static final Identifier TEXTURE = Identifier.of(UntamedMain.MOD_ID, "textures/entity/toucan.png");

    public ToucanRenderer(EntityRendererFactory.Context context) {
        super(context, new ToucanEntityModel<>(context.getPart(RenderInit.TOUCAN_LAYER)), 0.3F);
    }

    @Override
    public Identifier getTexture(ToucanEntity entity) {
        return TEXTURE;
    }
}
