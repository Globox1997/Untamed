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
    public int lion_spawn_weight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int lioness_spawn_weight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int rhino_spawn_weight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int capybara_spawn_weight = 1;
    @ConfigEntry.Category("spawn_setting")
    @ConfigEntry.Gui.RequiresRestart
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int octopus_spawn_weight = 1;
}