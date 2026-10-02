package net.untamed.entity.ai.capybara;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.untamed.entity.CapybaraEntity;
import net.untamed.entity.ai.WaterUtils;
import org.jetbrains.annotations.Nullable;

public class SeekWater extends Behavior<CapybaraEntity> {

    private static final int SEARCH_HORIZONTAL_RANGE = 12;
    private static final int SEARCH_VERTICAL_RANGE = 4;
    private static final int START_CHANCE = 200;

    private static final double ANCHOR_RETURN_DIST_SQR = 144;
    private static final double NEARBY_RECHECK_SQR = 225;

    private final float speedModifier;
    @Nullable
    private BlockPos targetWater;

    public SeekWater(float speedModifier) {
        super(ImmutableMap.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT, MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT, MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), 100, 200);
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CapybaraEntity capybara) {
        if (capybara.isInWater() || capybara.getRandom().nextInt(START_CHANCE) != 0) {
            return false;
        }
        this.targetWater = WaterUtils.findNearestWater(level, capybara, SEARCH_HORIZONTAL_RANGE, SEARCH_VERTICAL_RANGE, WaterUtils.MIN_OPERABLE_WATER_DEPTH);
        if (this.targetWater == null) {
            BlockPos anchor = capybara.getWaterAnchor();
            if (anchor != null && level.getFluidState(anchor).is(FluidTags.WATER) && capybara.blockPosition().distSqr(anchor) > ANCHOR_RETURN_DIST_SQR) {
                this.targetWater = anchor;
            }
        }
        if (this.targetWater != null && capybara.blockPosition().distSqr(this.targetWater) < NEARBY_RECHECK_SQR && WaterUtils.isBlockedOff(capybara, this.targetWater)) {
            this.targetWater = null;
        }
        return this.targetWater != null;
    }

    @Override
    protected void start(ServerLevel level, CapybaraEntity capybara, long time) {
        if (this.targetWater != null) {
            BehaviorUtils.setWalkAndLookTargetMemories(capybara, this.targetWater, this.speedModifier, 0);
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, CapybaraEntity capybara, long time) {
        return !level.getFluidState(capybara.blockPosition()).is(FluidTags.WATER) && this.targetWater != null && level.getFluidState(this.targetWater).is(FluidTags.WATER);
    }

    @Override
    protected void stop(ServerLevel level, CapybaraEntity capybara, long time) {
        capybara.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        capybara.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        this.targetWater = null;
    }
}
