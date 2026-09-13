package net.untamed.entity.ai;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.untamed.init.BrainInit;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Long calm pauses: stands still on land, floats still in water, occasionally
 * glancing around. While active, the IDLE_REST memory gates strolling and diving.
 * Starts only inside stroll-gaps (WALK_TARGET absent), so it never fights an
 * in-progress movement; if something legitimate writes a walk target (herd,
 * temptation, breeding), the rest simply ends early.
 */
public class RestIdle<E extends PathfinderMob> extends Behavior<E> {

    @Nullable
    private final Function<E, Optional<PositionTracker>> lookAroundTarget;
    private final Predicate<E> canRestHere;

    private long nextLookAroundTime;

    public RestIdle(int minDuration, int maxDuration) {
        this(minDuration, maxDuration, mob -> true, null);
    }

    public RestIdle(int minDuration, int maxDuration, Predicate<E> canRestHere) {
        this(minDuration, maxDuration, canRestHere, null);
    }

    public RestIdle(int minDuration, int maxDuration, Predicate<E> canRestHere,
                    @Nullable Function<E, Optional<PositionTracker>> lookAroundTarget) {
        super(ImmutableMap.of(
                MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                BrainInit.IDLE_REST, MemoryStatus.VALUE_ABSENT), minDuration, maxDuration);
        this.canRestHere = canRestHere;
        this.lookAroundTarget = lookAroundTarget;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E mob) {
        return mob.getRandom().nextInt(60) == 0 && this.canRestHere.test(mob);
    }

    @Override
    protected void start(ServerLevel level, E mob, long time) {
        this.nextLookAroundTime = time + 20 + mob.getRandom().nextInt(40);
    }

    @Override
    protected void stop(ServerLevel level, E mob, long time) {
        mob.getBrain().eraseMemory(BrainInit.IDLE_REST);
        mob.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, E mob, long time) {
        return !mob.isPanicking() && !mob.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
                && this.canRestHere.test(mob);
    }

    @Override
    protected void tick(ServerLevel level, E mob, long time) {
        mob.getBrain().setMemoryWithExpiry(BrainInit.IDLE_REST, Unit.INSTANCE, 20L);
        if (time >= this.nextLookAroundTime) this.glanceAround(mob, time);
    }

    private void glanceAround(E mob, long time) {
        this.nextLookAroundTime = time + 40 + mob.getRandom().nextInt(40);
        Optional<PositionTracker> entityLook = this.lookAroundTarget != null
                ? this.lookAroundTarget.apply(mob) : Optional.empty();
        if (entityLook.isPresent() && mob.getRandom().nextInt(4) == 0)
            mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, entityLook.get());
        else if (mob.getRandom().nextInt(3) != 0) {
            BlockPos pos = new BlockPos(
                    Mth.floor(mob.getX()) + mob.getRandom().nextInt(7) - 3,
                    Mth.floor(mob.getY()) + mob.getRandom().nextInt(3) - 1,
                    Mth.floor(mob.getZ()) + mob.getRandom().nextInt(7) - 3);
            mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
        } else mob.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }
}