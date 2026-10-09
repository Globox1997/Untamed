package net.untamed.entity.ai.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.untamed.init.BrainInit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class HerdSensor extends Sensor<LivingEntity> {

    private final int maxHerdSize;

    public HerdSensor(int scanRate, int maxHerdSize) {
        super(scanRate);
        this.maxHerdSize = maxHerdSize;
    }

    @Override
    protected void doTick(ServerLevel serverLevel, LivingEntity entity) {
        Brain<?> brain = entity.getBrain();
        List<LivingEntity> herd = brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES)
                .orElse(List.of())
                .stream()
                .filter(livingEntity -> livingEntity.isAlive() && livingEntity.getType() == entity.getType())
                .limit(this.maxHerdSize)
                .toList();
        if (herd.isEmpty()) {
            brain.eraseMemory(BrainInit.NEAREST_HERD_MEMBERS);
        } else {
            brain.setMemory(BrainInit.NEAREST_HERD_MEMBERS, herd);
        }

    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(BrainInit.NEAREST_HERD_MEMBERS, MemoryModuleType.NEAREST_LIVING_ENTITIES);
    }
}
