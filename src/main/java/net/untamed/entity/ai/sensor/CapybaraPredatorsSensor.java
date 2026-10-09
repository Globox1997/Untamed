package net.untamed.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.untamed.entity.CapybaraEntity;
import net.untamed.init.BrainInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

public class CapybaraPredatorsSensor extends Sensor<CapybaraEntity> {

    private static final long PREDATOR_MEMORY_EXPIRY = 200L;
    private static final long PANIC_MEMORY_EXPIRY = 120L;

    public CapybaraPredatorsSensor() {
        super(10);
    }

    @Override
    protected void doTick(ServerLevel serverLevel, CapybaraEntity livingEntity) {
        Brain<?> brain = livingEntity.getBrain();
        Optional<LivingEntity> predator = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .flatMap(visible -> visible.findClosest(capybara -> capybara.getType().is(TagInit.CAPYBARA_PREDATORS)));
        if (predator.isPresent()) {
            brain.setMemoryWithExpiry(BrainInit.NEAREST_VISIBLE_PREDATOR, predator.get(), PREDATOR_MEMORY_EXPIRY);
            brain.setMemoryWithExpiry(MemoryModuleType.IS_PANICKING, true, PANIC_MEMORY_EXPIRY);
        } else {
            brain.eraseMemory(BrainInit.NEAREST_VISIBLE_PREDATOR);
        }
    }


    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(BrainInit.NEAREST_VISIBLE_PREDATOR, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.IS_PANICKING);
    }
}
