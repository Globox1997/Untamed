package net.untamed.entity.ai.capybara;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.CapybaraEntity;
import net.untamed.entity.ai.WaterUtils;
import org.jetbrains.annotations.Nullable;

public class SeekLand extends Behavior<CapybaraEntity> {

    private static final int START_CHANCE = 200;

    private final float speedModifier;
    @Nullable
    private BlockPos targetLand;

    public SeekLand(float speedModifier) {
        super(ImmutableMap.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT, MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT, MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), 100, 200);
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CapybaraEntity capybara) {
        if (!capybara.isFloating() || capybara.isDiving() || capybara.getRandom().nextInt(START_CHANCE) != 0) {
            return false;
        }
        Vec3 candidate = LandRandomPos.getPos(capybara, 10, 7);
        if (candidate == null) {
            return false;
        }
        BlockPos pos = BlockPos.containing(candidate.x, candidate.y, candidate.z);
        if (!level.getFluidState(pos).isEmpty() || WaterUtils.isBlockedOff(capybara, pos)) {
            return false;
        }
        this.targetLand = pos;
        return true;
    }

    @Override
    protected void start(ServerLevel level, CapybaraEntity capybara, long time) {
        if (this.targetLand != null) {
            BehaviorUtils.setWalkAndLookTargetMemories(capybara, this.targetLand, this.speedModifier, 0);
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, CapybaraEntity capybara, long time) {
        return this.targetLand != null && capybara.isInWater();
    }

    @Override
    protected void stop(ServerLevel level, CapybaraEntity capybara, long time) {
        if (capybara.isInWater()) {
            capybara.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
        capybara.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        this.targetLand = null;
    }
}
