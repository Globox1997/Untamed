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

    public static SoundEvent BUFFALO_IDLE_EVENT = register("buffalo_idle");
    public static SoundEvent BUFFALO_HURT_EVENT = register("buffalo_hurt");
    public static SoundEvent BUFFALO_DEATH_EVENT = register("buffalo_death");
    public static SoundEvent BUFFALO_STEP_EVENT = register("buffalo_step");
    public static SoundEvent BUFFALO_WARNING_EVENT = register("buffalo_warning");

    public static SoundEvent LION_IDLE_EVENT = register("lion_idle");
    public static SoundEvent LION_HURT_EVENT = register("lion_hurt");
    public static SoundEvent LION_DEATH_EVENT = register("lion_death");
    public static SoundEvent LION_STEP_EVENT = register("lion_step");
    public static SoundEvent LION_ROARING_EVENT = register("lion_roaring");
    public static SoundEvent LION_SLEEPING_EVENT = register("lion_sleeping");

    public static SoundEvent OCTOPUS_IDLE_EVENT = register("octopus_idle");
    public static SoundEvent OCTOPUS_HURT_EVENT = register("octopus_hurt");
    public static SoundEvent OCTOPUS_DEATH_EVENT = register("octopus_death");
    public static SoundEvent OCTOPUS_STEP_EVENT = register("octopus_step");

    public static SoundEvent BLACK_BEAR_IDLE_EVENT = register("black_bear_idle");
    public static SoundEvent BLACK_BEAR_HURT_EVENT = register("black_bear_hurt");
    public static SoundEvent BLACK_BEAR_DEATH_EVENT = register("black_bear_death");
    public static SoundEvent BLACK_BEAR_STEP_EVENT = register("black_bear_step");
    public static SoundEvent BLACK_BEAR_ATTACK_EVENT = register("black_bear_attack");

    public static SoundEvent BLACK_PANTHER_IDLE_EVENT = register("black_panther_idle");
    public static SoundEvent BLACK_PANTHER_HURT_EVENT = register("black_panther_hurt");
    public static SoundEvent BLACK_PANTHER_DEATH_EVENT = register("black_panther_death");
    public static SoundEvent BLACK_PANTHER_STEP_EVENT = register("black_panther_step");
    public static SoundEvent BLACK_PANTHER_ATTACK_EVENT = register("black_panther_attack");

    public static SoundEvent CAPYBARA_IDLE_EVENT = register("capybara_idle");
    public static SoundEvent CAPYBARA_HURT_EVENT = register("capybara_hurt");
    public static SoundEvent CAPYBARA_DEATH_EVENT = register("capybara_death");
    public static SoundEvent CAPYBARA_STEP_EVENT = register("capybara_step");

    public static SoundEvent RHINO_IDLE_EVENT = register("rhino_idle");
    public static SoundEvent RHINO_HURT_EVENT = register("rhino_hurt");
    public static SoundEvent RHINO_DEATH_EVENT = register("rhino_death");
    public static SoundEvent RHINO_STEP_EVENT = register("rhino_step");
    public static SoundEvent RHINO_ATTACK_EVENT = register("rhino_attack");

    public static SoundEvent VULTURE_IDLE_EVENT = register("vulture_idle");
    public static SoundEvent VULTURE_HURT_EVENT = register("vulture_hurt");
    public static SoundEvent VULTURE_DEATH_EVENT = register("vulture_death");
    public static SoundEvent VULTURE_FLAP_EVENT = register("vulture_flap");

    public static SoundEvent HYENA_IDLE_EVENT = register("hyena_idle");
    public static SoundEvent HYENA_HURT_EVENT = register("hyena_hurt");
    public static SoundEvent HYENA_DEATH_EVENT = register("hyena_death");
    public static SoundEvent HYENA_STEP_EVENT = register("hyena_step");
    public static SoundEvent HYENA_ATTACK_EVENT = register("hyena_attack");
    public static SoundEvent HYENA_WHOOP_EVENT = register("hyena_whoop");

    public static SoundEvent GAZELLE_IDLE_EVENT = register("gazelle_idle");
    public static SoundEvent GAZELLE_HURT_EVENT = register("gazelle_hurt");
    public static SoundEvent GAZELLE_DEATH_EVENT = register("gazelle_death");
    public static SoundEvent GAZELLE_STEP_EVENT = register("gazelle_step");

    public static SoundEvent RACCOON_IDLE_EVENT = register("raccoon_idle");
    public static SoundEvent RACCOON_HURT_EVENT = register("raccoon_hurt");
    public static SoundEvent RACCOON_DEATH_EVENT = register("raccoon_death");
    public static SoundEvent RACCOON_STEP_EVENT = register("raccoon_step");

    public static SoundEvent SNOW_LEOPARD_IDLE_EVENT = register("snow_leopard_idle");
    public static SoundEvent SNOW_LEOPARD_HURT_EVENT = register("snow_leopard_hurt");
    public static SoundEvent SNOW_LEOPARD_DEATH_EVENT = register("snow_leopard_death");
    public static SoundEvent SNOW_LEOPARD_STEP_EVENT = register("snow_leopard_step");
    public static SoundEvent SNOW_LEOPARD_ATTACK_EVENT = register("snow_leopard_attack");
    public static SoundEvent SNOW_LEOPARD_PRUSTEN_EVENT = register("snow_leopard_prusten");
    public static SoundEvent SNOW_LEOPARD_YOWL_EVENT = register("snow_leopard_yowl");

    private static SoundEvent register(String id) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, UntamedMain.identifierOf(id), SoundEvent.createVariableRangeEvent(UntamedMain.identifierOf(id)));
    }

    public static void init() {
    }

}
