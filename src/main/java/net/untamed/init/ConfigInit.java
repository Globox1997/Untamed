package net.untamed.init;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.untamed.config.UntamedConfig;

public class ConfigInit {

    public static UntamedConfig CONFIG = new UntamedConfig();

    public static void init() {
        AutoConfig.register(UntamedConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(UntamedConfig.class).getConfig();
    }
}
