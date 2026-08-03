package net.untamed.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.untamed.UntamedMain;
import net.untamed.entity.model.*;
import net.untamed.entity.render.*;

@Environment(EnvType.CLIENT)
public class RenderInit {

    public static final ModelLayerLocation LION_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("lion_render_layer"), "lion_render_layer");
    public static final ModelLayerLocation LIONESS_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("lioness_render_layer"), "lioness_render_layer");
    public static final ModelLayerLocation RHINO_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("rhino_render_layer"), "rhino_render_layer");
    public static final ModelLayerLocation CAPYBARA_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("capybara_render_layer"), "capybara_render_layer");
    public static final ModelLayerLocation OCTOPUS_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("octopus_render_layer"), "octopus_render_layer");
    public static final ModelLayerLocation KIWI_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("kiwi_render_layer"), "kiwi_render_layer");

    public static void init() {
        // Entity Renderer
        EntityRendererRegistry.register(EntityInit.LION, LionRenderer::new);
        EntityRendererRegistry.register(EntityInit.LIONESS, LionessRenderer::new);
        EntityRendererRegistry.register(EntityInit.RHINO, RhinoRenderer::new);
        EntityRendererRegistry.register(EntityInit.CAPYBARA, CapybaraRenderer::new);
        EntityRendererRegistry.register(EntityInit.OCTOPUS, OctopusRenderer::new);
        EntityRendererRegistry.register(EntityInit.KIWI, KiwiRenderer::new);
        // Entity Layer
        EntityModelLayerRegistry.registerModelLayer(LION_LAYER, LionModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(LIONESS_LAYER, LionessModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(RHINO_LAYER, RhinoModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(CAPYBARA_LAYER, CapybaraModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(OCTOPUS_LAYER, OctopusModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(KIWI_LAYER, KiwiModel::createBodyLayer);
    }
}
