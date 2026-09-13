package net.untamed.entity.ai.capybara;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.CapybaraEntity;
import net.untamed.entity.ai.WaterUtils;
import net.untamed.init.BrainInit;
import org.jetbrains.annotations.Nullable;

public class FleeToWater extends Behavior<CapybaraEntity> {

    private static final int WATER_SEARCH_HORIZONTAL_RANGE = 16;
    private static final int WATER_SEARCH_VERTICAL_RANGE = 4;
    private static final float DIVE_WALK_SPEED = 0.8F;

    private static final double LUNGE_SQR_RADIUS = 6.25;
    private static final double LUNGE_VERTICAL = 0.2;
    private static final double LUNGE_HORIZONTAL = 0.3;
    private static final double LUNGE_MIN_HORIZONTAL = 0.5;

    private final float speedModifier;
    @Nullable
    private BlockPos targetWater;

    public FleeToWater(float speedModifier) {
        super(ImmutableMap.of(
                MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), 200, 300);
        this.speedModifier = speedModifier;
    }

    @Override
    protected void start(ServerLevel level, CapybaraEntity capybara, long time) {
        if (!capybara.isFloating()) {
            this.targetWater = WaterUtils.findNearestWater(level, capybara, WATER_SEARCH_HORIZONTAL_RANGE, WATER_SEARCH_VERTICAL_RANGE, WaterUtils.MIN_OPERABLE_WATER_DEPTH);
            if (this.targetWater != null && WaterUtils.isBlockedOff(capybara, this.targetWater))
                this.targetWater = null;
            if (this.targetWater != null)
                BehaviorUtils.setWalkAndLookTargetMemories(capybara, this.targetWater, this.speedModifier, 0);
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, CapybaraEntity capybara, long time) {
        return capybara.isPanicking();
    }

    @Override
    protected void tick(ServerLevel level, CapybaraEntity capybara, long time) {
        Brain<?> brain = capybara.getBrain();
        if (capybara.isFloating()) this.tickInWater(level, capybara);
        else this.tickOnLand(level, capybara, brain);
    }

    private void tickInWater(ServerLevel level, CapybaraEntity capybara) {
        capybara.setDiving(true);
        if (capybara.getNavigation().isDone()) this.moveAlongBottomAway(level, capybara);
    }

    /**
     * Picks escape destinations away from the threat and projects them onto the
     * riverbed. If every escape direction leads to dry land (wall/dead end),
     * stays put at the bottom rather than surfacing into the predator.
     */
    private void moveAlongBottomAway(ServerLevel level, CapybaraEntity capybara) {
        Vec3 threatPos = this.getThreatPos(capybara);
        for (int attempt = 0; attempt < 4; attempt++) {
            Vec3 away = threatPos != null
                    ? LandRandomPos.getPosAway(capybara, 8, 4, threatPos)
                    : LandRandomPos.getPos(capybara, 8, 4);
            if (away == null) return;
            BlockPos bottom = WaterUtils.riverbedAt(level, away.x, away.z);
            if (bottom != null && capybara.distanceToSqr(Vec3.atBottomCenterOf(bottom)) > 2.0) {
                BehaviorUtils.setWalkAndLookTargetMemories(capybara, bottom, DIVE_WALK_SPEED, 1);
                return;
            }
        }
    }

    private void tickOnLand(ServerLevel level, CapybaraEntity capybara, Brain<?> brain) {
        if (level.getFluidState(capybara.blockPosition()).is(FluidTags.WATER)) return;
        boolean validWater = this.targetWater != null && level.getFluidState(this.targetWater).is(FluidTags.WATER);
        if (!validWater) {
            if (capybara.getNavigation().isDone()) {
                Vec3 threatPos = this.getThreatPos(capybara);
                Vec3 away = threatPos != null
                        ? LandRandomPos.getPosAway(capybara, 12, 6, threatPos)
                        : LandRandomPos.getPos(capybara, 10, 7);
                if (away != null) brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(away, this.speedModifier, 0));
            }
            return;
        }

        if (capybara.getNavigation().isDone()) {
            double distSqr = capybara.distanceToSqr(Vec3.atBottomCenterOf(this.targetWater));
            Vec3 dir = Vec3.atBottomCenterOf(this.targetWater).subtract(capybara.position());
            double horizontalDist = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            if (distSqr < LUNGE_SQR_RADIUS && horizontalDist > LUNGE_MIN_HORIZONTAL)
                this.lungeIntoWater(capybara);
            else BehaviorUtils.setWalkAndLookTargetMemories(capybara, this.targetWater, this.speedModifier, 0);
        }
    }

    private void lungeIntoWater(CapybaraEntity capybara) {
        if (this.targetWater == null || this.targetWater.getY() > capybara.blockPosition().getY()) return;
        Vec3 dir = Vec3.atBottomCenterOf(this.targetWater).subtract(capybara.position());
        Vec3 horizontal = new Vec3(dir.x, 0.0, dir.z).normalize();
        capybara.setDeltaMovement(horizontal.scale(LUNGE_HORIZONTAL).add(0.0, LUNGE_VERTICAL, 0.0));
    }

    @Override
    protected void stop(ServerLevel level, CapybaraEntity capybara, long time) {
        capybara.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        capybara.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        capybara.setDiving(false);
        this.targetWater = null;
    }

    @Nullable
    private Vec3 getThreatPos(CapybaraEntity capybara) {
        Brain<?> brain = capybara.getBrain();
        return brain.getMemory(BrainInit.NEAREST_VISIBLE_PREDATOR)
                .filter(LivingEntity::isAlive)
                .map(Entity::position)
                .orElseGet(() -> brain.getMemory(MemoryModuleType.HURT_BY_ENTITY)
                        .filter(LivingEntity::isAlive)
                        .map(LivingEntity::position)
                        .orElse(null));
    }
}