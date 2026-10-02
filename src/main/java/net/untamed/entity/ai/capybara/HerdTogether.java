package net.untamed.entity.ai.capybara;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.pathfinder.Path;
import net.untamed.entity.CapybaraEntity;
import net.untamed.init.BrainInit;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HerdTogether extends Behavior<CapybaraEntity> {

    private static final double START_DISTANCE = 8.0;
    private static final double STOP_DISTANCE = 4.0;
    private static final long RETRY_COOLDOWN_TICKS = 100;
    private static final int MEMBER_PATH_RANGE = 1;
    private static final double MIN_PROGRESS = 1.0;

    private final float speedModifier;
    @Nullable
    private LivingEntity herdMember;
    private long retryAfter = Long.MIN_VALUE;
    private double distanceAtStart;

    public HerdTogether(float speedModifier) {
        super(ImmutableMap.of(BrainInit.NEAREST_HERD_MEMBERS, MemoryStatus.VALUE_PRESENT, MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT, MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT), 80, 160);
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CapybaraEntity capybara) {
        if (capybara.isBaby() || level.getGameTime() < this.retryAfter) {
            return false;
        }
        List<LivingEntity> members = capybara.getBrain().getMemory(BrainInit.NEAREST_HERD_MEMBERS).orElse(List.of());
        for (LivingEntity member : members) {
            if (!member.isAlive() || capybara.distanceTo(member) <= START_DISTANCE) {
                continue;
            }
            Path path = capybara.getNavigation().createPath(member.blockPosition(), MEMBER_PATH_RANGE);
            if (path != null && path.canReach()) {
                this.herdMember = member;
                return true;
            }
        }
        this.retryAfter = level.getGameTime() + RETRY_COOLDOWN_TICKS;
        return false;
    }

    @Override
    protected void start(ServerLevel level, CapybaraEntity capybara, long time) {
        this.distanceAtStart = capybara.distanceTo(this.herdMember);
        capybara.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        BehaviorUtils.setWalkAndLookTargetMemories(capybara, this.herdMember, this.speedModifier, 3);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, CapybaraEntity capybara, long time) {
        return this.herdMember != null && this.herdMember.isAlive() && capybara.distanceTo(this.herdMember) > STOP_DISTANCE && !capybara.isPanicking();
    }

    @Override
    protected void stop(ServerLevel level, CapybaraEntity capybara, long time) {
        if (this.distanceAtStart - capybara.distanceTo(this.herdMember) < MIN_PROGRESS) {
            this.retryAfter = time + RETRY_COOLDOWN_TICKS;
        }
        capybara.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        capybara.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        this.herdMember = null;
    }
}
