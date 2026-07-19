package net.untamed.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.untamed.UntamedMain;

public class SoundInit {

    private static SoundEvent register(String id) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(UntamedMain.identifierOf(id)));
    }

    public static void init() {
    }

}
