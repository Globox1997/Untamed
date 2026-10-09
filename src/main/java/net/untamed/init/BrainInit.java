package net.untamed.init;

import com.mojang.datafixers.util.Unit;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.sensing.TemptingSensor;
import net.minecraft.world.entity.schedule.Activity;
import net.untamed.UntamedMain;
import net.untamed.entity.ai.capybara.CapybaraAi;
import net.untamed.entity.ai.sensor.HerdSensor;
import net.untamed.entity.ai.sensor.CapybaraPredatorsSensor;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class BrainInit {

    // Custom memories (generic: reusable by other herd/prey mobs)
    public static final MemoryModuleType<LivingEntity> NEAREST_VISIBLE_PREDATOR = registerMemory("nearest_visible_predator");
    public static final MemoryModuleType<List<LivingEntity>> NEAREST_HERD_MEMBERS = registerMemory("nearest_herd_members");
    public static final MemoryModuleType<Unit> LOOK_AT_PLAYER_COOLDOWN = registerMemory("look_at_player_cooldown");
    public static final MemoryModuleType<Unit> IDLE_REST = registerMemory("idle_rest");
    public static final MemoryModuleType<Unit> IS_DAYTIME = registerMemory("is_daytime");

    // Custom sensors
    public static final SensorType<CapybaraPredatorsSensor> CAPYBARA_PREDATORS = registerSensor("capybara_predators", CapybaraPredatorsSensor::new);
    public static final SensorType<HerdSensor> CAPYBARA_HERD = registerSensor("capybara_herd", () -> new HerdSensor(40, 8));
    public static final SensorType<TemptingSensor> CAPYBARA_TEMPTATIONS = registerSensor("capybara_temptations", () -> new TemptingSensor(CapybaraAi.getTemptations()));

    // Custom activities
    public static final Activity NIGHT = registerActivity("night");

    private static <U> MemoryModuleType<U> registerMemory(String name) {
        return Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, UntamedMain.identifierOf(name), new MemoryModuleType<>(Optional.empty()));
    }

    private static <U extends Sensor<?>> SensorType<U> registerSensor(String name, Supplier<U> factory) {
        return Registry.register(BuiltInRegistries.SENSOR_TYPE, UntamedMain.identifierOf(name), new SensorType<>(factory));
    }

    private static Activity registerActivity(String name) {
        return Registry.register(BuiltInRegistries.ACTIVITY, UntamedMain.identifierOf(name), new Activity((name)));
    }

    @SuppressWarnings({"unused", "UnusedAssignment"})
    public static void init() {
        MemoryModuleType<?> memories = NEAREST_VISIBLE_PREDATOR;
        memories = NEAREST_HERD_MEMBERS;
        memories = LOOK_AT_PLAYER_COOLDOWN;
        memories = IDLE_REST;
        SensorType<?> sensors = CAPYBARA_PREDATORS;
        sensors = CAPYBARA_HERD;
        sensors = CAPYBARA_TEMPTATIONS;
        Activity activity = NIGHT;
    }
}