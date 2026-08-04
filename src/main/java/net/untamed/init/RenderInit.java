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
    public static final ModelLayerLocation BLACK_BEAR_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("black_bear_render_layer"), "black_bear_render_layer");
    public static final ModelLayerLocation BUFFALO_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("buffalo_render_layer"), "buffalo_render_layer");
    public static final ModelLayerLocation BISON_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("bison_render_layer"), "bison_render_layer");
    public static final ModelLayerLocation VULTURE_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("vulture_render_layer"), "vulture_render_layer");
    public static final ModelLayerLocation BLACK_PANTHER_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("black_panther_render_layer"), "black_panther_render_layer");
    public static final ModelLayerLocation HYENA_LAYER = new ModelLayerLocation(UntamedMain.identifierOf("hyena_render_layer"), "hyena_render_layer");

    public static void init() {
        // Entity Renderer
        EntityRendererRegistry.register(EntityInit.LION, LionRenderer::new);
        EntityRendererRegistry.register(EntityInit.LIONESS, LionessRenderer::new);
        EntityRendererRegistry.register(EntityInit.RHINO, RhinoRenderer::new);
        EntityRendererRegistry.register(EntityInit.CAPYBARA, CapybaraRenderer::new);
        EntityRendererRegistry.register(EntityInit.OCTOPUS, OctopusRenderer::new);
        EntityRendererRegistry.register(EntityInit.KIWI, KiwiRenderer::new);
        EntityRendererRegistry.register(EntityInit.BLACK_BEAR, BlackBearRenderer::new);
        EntityRendererRegistry.register(EntityInit.BUFFALO, BuffaloRenderer::new);
        EntityRendererRegistry.register(EntityInit.BISON, BisonRenderer::new);
        EntityRendererRegistry.register(EntityInit.VULTURE, VultureRenderer::new);
        EntityRendererRegistry.register(EntityInit.BLACK_PANTHER, BlackPantherRenderer::new);
        EntityRendererRegistry.register(EntityInit.HYENA, HyenaRenderer::new);
        // Entity Layer
        EntityModelLayerRegistry.registerModelLayer(LION_LAYER, LionModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(LIONESS_LAYER, LionessModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(RHINO_LAYER, RhinoModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(CAPYBARA_LAYER, CapybaraModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(OCTOPUS_LAYER, OctopusModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(KIWI_LAYER, KiwiModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(BLACK_BEAR_LAYER, BlackBearModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(BUFFALO_LAYER, BuffaloModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(BISON_LAYER, BisonModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(VULTURE_LAYER, VultureModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(BLACK_PANTHER_LAYER, BlackPantherModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(HYENA_LAYER, HyenaModel::createBodyLayer);
    }
}
