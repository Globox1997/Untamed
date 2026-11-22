package net.untamed.init;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.untamed.UntamedMain;

public class CompatInit {

    public static void init() {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("environmentz")) {
            loader.getModContainer(UntamedMain.MOD_ID).ifPresent(container -> ResourceManagerHelper.registerBuiltinResourcePack(UntamedMain.identifierOf("environmentz_compat"),
                    container, Component.translatable("pack.untamed.environmentz_compat"), ResourcePackActivationType.ALWAYS_ENABLED));
        }
    }
}
