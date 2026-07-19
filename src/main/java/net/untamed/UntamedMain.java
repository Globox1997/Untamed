package net.untamed;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import net.untamed.init.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UntamedMain implements ModInitializer {

	public static final String MOD_ID = "untamed";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ConfigInit.init();
		EntityInit.init();
		ItemInit.init();
		SoundInit.init();
		SpawnInit.init();
		TagInit.init();
	}

	public static ResourceLocation identifierOf(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
