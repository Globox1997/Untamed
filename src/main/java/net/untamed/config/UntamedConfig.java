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
    public int lionessSpawnWeight = 3;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int rhinoSpawnWeight = 2;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int capybaraSpawnWeight = 8;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int octopusSpawnWeight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int kiwiSpawnWeight = 4;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int blackBearSpawnWeight = 3;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int buffaloSpawnWeight = 6;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int bisonSpawnWeight = 6;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int vultureSpawnWeight = 3;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int blackPantherSpawnWeight = 2;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int hyenaSpawnWeight = 3;

    @ConfigEntry.Category("behavior_setting")
    public boolean lionsHuntLivestock = true;
    @ConfigEntry.Category("behavior_setting")
    public boolean hyenasHuntLivestock = true;
    @ConfigEntry.Category("behavior_setting")
    public boolean panthersHuntLivestock = true;
}