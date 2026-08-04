package net.untamed.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "untamed")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class UntamedConfig implements ConfigData {

    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int lionSpawnweight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int lionessSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int rhinoSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int capybaraSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int octopusSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int kiwiSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int blackBearSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int buffaloSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int bisonSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int vultureSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int blackPantherSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int hyenaSpawnWeight = 1;
}