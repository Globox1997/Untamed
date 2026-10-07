package net.untamed.entity.ai.capybara;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.CapybaraEntity;
import net.untamed.init.BrainInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

public class GrazeGrass extends Behavior<CapybaraEntity> {

    private static final int START_CHANCE = 120;
    private static final int MIN_BITE_INTERVAL = 40;
    private static final int BITE_INTERVAL_VARIATION = 40;
    private static final double STEP_DISTANCE = 1.0D;
    private static final double STEP_SPEED = 0.4D;

    private long nextBiteTime;

    public GrazeGrass() {
        super(ImmutableMap.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT, MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT,
                BrainInit.IDLE_REST, MemoryStatus.VALUE_ABSENT), 100, 260);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CapybaraEntity capybara) {
        return capybara.getRandom().nextInt(START_CHANCE) == 0 && this.canGrazeHere(level, capybara);
    }

    private boolean canGrazeHere(ServerLevel level, CapybaraEntity capybara) {
        if (capybara.isInWater() || !capybara.onGround()) {
            return false;
        }
        BlockPos feet = capybara.blockPosition();
        return level.getBlockState(feet.below()).is(TagInit.HERD_GRAZEABLE) || this.findSugarCane(level, feet) != null;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, CapybaraEntity capybara, long time) {
        return !capybara.isPanicking() && !capybara.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                && !capybara.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET) && this.canGrazeHere(level, capybara);
    }

    @Override
    protected void start(ServerLevel level, CapybaraEntity capybara, long time) {
        capybara.getNavigation().stop();
        capybara.setGrazing(true);
        this.nextBiteTime = time + MIN_BITE_INTERVAL + capybara.getRandom().nextInt(BITE_INTERVAL_VARIATION);
    }

    @Override
    protected void tick(ServerLevel level, CapybaraEntity capybara, long time) {
        if (time < this.nextBiteTime) {
            return;
        }
        this.nextBiteTime = time + MIN_BITE_INTERVAL + capybara.getRandom().nextInt(BITE_INTERVAL_VARIATION);
        this.bite(level, capybara);
        float yaw = (capybara.getYRot() + (capybara.getRandom().nextFloat() - 0.5F) * 60.0F) * Mth.DEG_TO_RAD;
        Vec3 step = capybara.position().add(-Mth.sin(yaw) * STEP_DISTANCE, 0.0D, Mth.cos(yaw) * STEP_DISTANCE);
        capybara.getNavigation().moveTo(step.x, step.y, step.z, STEP_SPEED);
    }

    private void bite(ServerLevel level, CapybaraEntity capybara) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        BlockPos feet = capybara.blockPosition();
        BlockPos cane = this.findSugarCane(level, feet);
        if (cane != null) {
            BlockPos top = cane;
            while (level.getBlockState(top.above()).is(Blocks.SUGAR_CANE)) {
                top = top.above();
            }
            if (top.getY() > cane.getY()) {
                capybara.getLookControl().setLookAt(Vec3.atCenterOf(cane));
                level.destroyBlock(top, false);
                capybara.playSound(SoundEvents.GENERIC_EAT, 0.6F, 1.0F);
            }
        } else if (level.getBlockState(feet).is(Blocks.SHORT_GRASS) && capybara.getRandom().nextInt(3) == 0) {
            level.destroyBlock(feet, false);
        }
    }

    @Nullable
    private BlockPos findSugarCane(ServerLevel level, BlockPos feet) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = feet.relative(direction);
            if (level.getBlockState(side).is(Blocks.SUGAR_CANE)) {
                return side;
            }
        }
        return null;
    }

    @Override
    protected void stop(ServerLevel level, CapybaraEntity capybara, long time) {
        capybara.setGrazing(false);
        capybara.getNavigation().stop();
    }
}
