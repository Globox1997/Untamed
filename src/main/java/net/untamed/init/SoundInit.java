package net.untamed.init;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.untamed.UntamedMain;

public class SoundInit {

    public static SoundEvent KIWI_IDLE_EVENT = register("kiwi_idle");
    public static SoundEvent KIWI_HURT_EVENT = register("kiwi_hurt");
    public static SoundEvent KIWI_DEATH_EVENT = register("kiwi_death");
    public static SoundEvent KIWI_STEP_EVENT = register("kiwi_step");

    public static SoundEvent BISON_IDLE_EVENT = register("bison_idle");
    public static SoundEvent BISON_HURT_EVENT = register("bison_hurt");
    public static SoundEvent BISON_DEATH_EVENT = register("bison_death");
    public static SoundEvent BISON_STEP_EVENT = register("bison_step");
    public static SoundEvent BISON_WARNING_EVENT = register("bison_warning");

    private static SoundEvent register(String id) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(UntamedMain.identifierOf(id)));
    }

    public static void init() {
    }

}
