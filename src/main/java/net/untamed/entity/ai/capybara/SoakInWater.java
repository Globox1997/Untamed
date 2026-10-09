package net.untamed.entity.ai.capybara;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.untamed.entity.CapybaraEntity;
import net.untamed.entity.ai.WaterUtils;
import net.untamed.init.BrainInit;

public class SoakInWater extends Behavior<CapybaraEntity> {

    private static final int SOAK_CHANCE = 300;
    private static final int MIN_WATER_DEPTH = 2;

    public SoakInWater() {
        super(ImmutableMap.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT, MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT, MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT,
                BrainInit.IDLE_REST, MemoryStatus.VALUE_PRESENT), 10, 30);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CapybaraEntity capybara) {
        return capybara.isFloating() && !capybara.isDiving() && capybara.getRandom().nextInt(SOAK_CHANCE) == 0 && WaterUtils.getWaterDepth(level, capybara) >= MIN_WATER_DEPTH;
    }

    @Override
    protected void start(ServerLevel level, CapybaraEntity capybara, long time) {
        capybara.startSoak();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, CapybaraEntity capybara, long time) {
        return capybara.isDiving() && !capybara.isPanicking();
    }

    @Override
    protected void stop(ServerLevel level, CapybaraEntity capybara, long time) {
        capybara.endSoak();
    }
}
