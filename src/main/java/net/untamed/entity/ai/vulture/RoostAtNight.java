package net.untamed.entity.ai.vulture;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.untamed.UntamedMain;
import net.untamed.entity.VultureEntity;
import net.untamed.entity.ai.LandUtils;
import net.untamed.entity.ai.WaterUtils;
import org.jetbrains.annotations.Nullable;

public class RoostAtNight extends Behavior<VultureEntity> {

    private static final int WANDER_CHANCE = 40;
    private static final int WANDER_RADIUS = 8;
    private static final int SEARCH_DEPTH = 4;
    private static final float WALK_SPEED = 1.0F;
    private static final int MIN_RELIEF = 2;
    private static final double FINAL_APPROACH_DIST_SQR = 4.0;
    private static final double PERCH_CENTER_DIST_SQR = 0.36;
    private static final double PERCH_RELEASE_DIST_SQR = 2.0;

    @Nullable
    private BlockPos perchPos;
    @Nullable
    private BlockPos reliefAnchor;
    @Nullable
    private BlockPos destination;
    private int approachTicks;

    public RoostAtNight() {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), 10000, 11000);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity vulture) {
        return true;
    }

    @Override
    protected void tick(ServerLevel level, VultureEntity vulture, long time) {
        if (vulture.isPerched()) {
            if (vulture.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY) || this.perchPos == null
                    || vulture.distanceToSqr(Vec3.atBottomCenterOf(this.perchPos)) > PERCH_RELEASE_DIST_SQR) {
                vulture.setPerched(false);
                this.destination = null;
            }
            return;
        }
        if (!vulture.onGround() || vulture.isInWater()) return;
        if (this.destination != null
                && vulture.distanceToSqr(Vec3.atBottomCenterOf(this.destination)) < PERCH_CENTER_DIST_SQR
                && LandUtils.isRoost(level, vulture.blockPosition())) {
            this.perch(vulture);
            return;
        }
        if (!vulture.getNavigation().isDone()) return;
        if (this.destination != null) {
            Vec3 center = Vec3.atBottomCenterOf(this.destination);
            double distSqr = vulture.distanceToSqr(center);
            if (distSqr < 0.5) {
                this.destination = null;
                return;
            }
            if (distSqr < FINAL_APPROACH_DIST_SQR) {
                if (++this.approachTicks > 100) {
                    this.destination = null;
                    this.approachTicks = 0;
                    return;
                }
                vulture.getMoveControl().setWantedPosition(center.x, center.y, center.z, WALK_SPEED);
                return;
            }
            this.destination = null;
        }
        if (vulture.getRandom().nextInt(WANDER_CHANCE) != 0) return;
        this.pickDestination(level, vulture);
    }

    private void pickDestination(ServerLevel level, VultureEntity vulture) {
        BlockPos roost = LandUtils.findBestRoost(level, vulture, WANDER_RADIUS, SEARCH_DEPTH);
        if (roost != null && !WaterUtils.isBlockedOff(vulture, roost)) {
            int bestScore = LandUtils.roostScore(level, roost);
            UntamedMain.LOGGER.info("[Untamed] roost: {} score={}", roost, bestScore);
            int currentScore = LandUtils.roostScore(level, vulture.blockPosition());
            if (currentScore > 0 && currentScore >= bestScore) {
                this.perch(vulture);
                return;
            }
            this.destination = roost;
            vulture.getNavigation().moveTo(roost.getX() + 0.5, roost.getY(), roost.getZ() + 0.5, WALK_SPEED);
            return;
        }
        BlockPos wander = this.wanderPos(level, vulture);
        if (wander != null && !WaterUtils.isBlockedOff(vulture, wander)) {
            this.destination = wander;
            UntamedMain.LOGGER.info("[Untamed] wander: {}", wander);
            vulture.getNavigation().moveTo(wander.getX() + 0.5, wander.getY(), wander.getZ() + 0.5, WALK_SPEED);
        }
    }

    private void perch(VultureEntity vulture) {
        vulture.setPerched(true);
        vulture.getNavigation().stop();
        vulture.setDeltaMovement(Vec3.ZERO);
        vulture.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        vulture.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        this.perchPos = vulture.blockPosition();
        UntamedMain.LOGGER.info("[Untamed] night: PERCHED at {}", this.perchPos); // TEMPORAL
    }

    @Nullable
    private BlockPos wanderPos(ServerLevel level, VultureEntity vulture) {
        boolean anchored = this.reliefAnchor != null;
        int cx = anchored ? this.reliefAnchor.getX() : vulture.blockPosition().getX();
        int cz = anchored ? this.reliefAnchor.getZ() : vulture.blockPosition().getZ();
        BlockPos fallback = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            float angle = vulture.getRandom().nextFloat() * Mth.TWO_PI;
            int radius = anchored ? 3 + vulture.getRandom().nextInt(4) : 4 + vulture.getRandom().nextInt(8);
            int x = cx + Mth.floor(Mth.cos(angle) * radius);
            int z = cz + Mth.floor(Mth.sin(angle) * radius);
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
            if (!level.isEmptyBlock(pos) || !level.getFluidState(pos.below()).isEmpty()) continue;
            if (LandUtils.terrainRoughness(level, x, z) >= MIN_RELIEF) return pos;
            fallback = pos;
        }
        return fallback;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, VultureEntity vulture, long time) {
        return !vulture.isDaytime();
    }

    @Override
    protected void stop(ServerLevel level, VultureEntity vulture, long time) {
        vulture.setPerched(false);
        vulture.getNavigation().stop();
        vulture.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        vulture.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        this.reliefAnchor = null;
        this.destination = null;
        this.approachTicks = 0;
        this.perchPos = null;
    }
}