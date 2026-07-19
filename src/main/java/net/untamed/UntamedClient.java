package net.untamed;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.untamed.init.RenderInit;

@Environment(EnvType.CLIENT)
public class UntamedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        RenderInit.init();
    }
}
