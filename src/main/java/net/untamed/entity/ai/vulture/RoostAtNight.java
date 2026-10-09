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
import net.untamed.entity.VultureEntity;
import net.untamed.entity.ai.LandUtils;
import net.untamed.entity.ai.WaterUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RoostAtNight extends Behavior<VultureEntity> {

    private static final int MIN_SEARCH_INTERVAL = 60;
    private static final int SEARCH_INTERVAL_VARIATION = 40;
    private static final int WANDER_RADIUS = 8;
    private static final int SEARCH_DEPTH = 4;
    private static final float WALK_SPEED = 1.0F;
    private static final int MIN_RELIEF = 2;
    private static final int MAX_FAILED_SEARCHES = 6;
    private static final double FLOCK_RANGE = 16.0D;
    private static final int FLOCK_SPREAD = 2;
    private static final double FINAL_APPROACH_DIST_SQR = 4.0;
    private static final double PERCH_CENTER_DIST_SQR = 0.36;
    private static final double FLOCK_ARRIVE_DIST_SQR = 2.25;
    private static final double PERCH_RELEASE_DIST_SQR = 2.0;
    private static final int MAX_APPROACH_TICKS = 100;

    @Nullable
    private BlockPos destination;
    private boolean joiningFlock;
    private int approachTicks;
    private int failedSearches;
    private long nextSearchTime;

    public RoostAtNight() {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), 24000);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity vulture) {
        return !vulture.isActiveTime();
    }

    @Override
    protected void tick(ServerLevel level, VultureEntity vulture, long time) {
        if (vulture.isPerched()) {
            BlockPos perchPos = vulture.getPerchPos();
            if (vulture.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY) || perchPos == null
                    || vulture.distanceToSqr(Vec3.atBottomCenterOf(perchPos)) > PERCH_RELEASE_DIST_SQR) {
                vulture.releasePerch();
                this.destination = null;
            }
            return;
        }
        if (!vulture.onGround() || vulture.isInWater() || vulture.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)) return;
        if (this.destination != null) {
            double distSqr = vulture.distanceToSqr(Vec3.atBottomCenterOf(this.destination));
            if (this.joiningFlock ? distSqr < FLOCK_ARRIVE_DIST_SQR : distSqr < PERCH_CENTER_DIST_SQR && LandUtils.isRoost(level, vulture.blockPosition())) {
                this.perch(vulture);
                return;
            }
        }
        if (!vulture.getNavigation().isDone()) return;
        if (this.destination != null) {
            Vec3 center = Vec3.atBottomCenterOf(this.destination);
            double distSqr = vulture.distanceToSqr(center);
            if (distSqr < FINAL_APPROACH_DIST_SQR && ++this.approachTicks <= MAX_APPROACH_TICKS) {
                vulture.getMoveControl().setWantedPosition(center.x, center.y, center.z, WALK_SPEED);
                return;
            }
            this.destination = null;
            this.approachTicks = 0;
        }
        if (time < this.nextSearchTime) return;
        this.nextSearchTime = time + MIN_SEARCH_INTERVAL + vulture.getRandom().nextInt(SEARCH_INTERVAL_VARIATION);
        this.pickDestination(level, vulture);
    }

    private void pickDestination(ServerLevel level, VultureEntity vulture) {
        if (++this.failedSearches > MAX_FAILED_SEARCHES) {
            this.perch(vulture);
            return;
        }
        this.joiningFlock = false;
        BlockPos flock = this.findFlockSpot(level, vulture);
        if (flock != null) {
            this.joiningFlock = true;
            this.moveTo(vulture, flock);
            return;
        }
        BlockPos roost = LandUtils.findBestRoost(level, vulture, WANDER_RADIUS, SEARCH_DEPTH);
        if (roost != null && !WaterUtils.isBlockedOff(vulture, roost)) {
            int currentScore = LandUtils.roostScore(level, vulture.blockPosition());
            if (currentScore > 0 && currentScore >= LandUtils.roostScore(level, roost)) {
                this.perch(vulture);
                return;
            }
            this.moveTo(vulture, roost);
            return;
        }
        BlockPos wander = this.wanderPos(level, vulture);
        if (wander != null && !WaterUtils.isBlockedOff(vulture, wander)) {
            this.moveTo(vulture, wander);
        }
    }

    private void moveTo(VultureEntity vulture, BlockPos pos) {
        this.destination = pos;
        this.approachTicks = 0;
        vulture.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, WALK_SPEED);
    }

    @Nullable
    private BlockPos findFlockSpot(ServerLevel level, VultureEntity vulture) {
        List<VultureEntity> perched = level.getEntitiesOfClass(VultureEntity.class, vulture.getBoundingBox().inflate(FLOCK_RANGE),
                other -> other != vulture && other.isPerched() && other.getPerchPos() != null);
        if (perched.isEmpty()) {
            return null;
        }
        BlockPos anchor = perched.get(vulture.getRandom().nextInt(perched.size())).getPerchPos();
        int x = anchor.getX() + vulture.getRandom().nextInt(FLOCK_SPREAD * 2 + 1) - FLOCK_SPREAD;
        int z = anchor.getZ() + vulture.getRandom().nextInt(FLOCK_SPREAD * 2 + 1) - FLOCK_SPREAD;
        BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        return level.isEmptyBlock(spot) && level.getFluidState(spot.below()).isEmpty() && !WaterUtils.isBlockedOff(vulture, spot) ? spot : null;
    }

    private void perch(VultureEntity vulture) {
        vulture.perchAt(vulture.blockPosition());
        this.destination = null;
        this.failedSearches = 0;
    }

    @Nullable
    private BlockPos wanderPos(ServerLevel level, VultureEntity vulture) {
        int cx = vulture.blockPosition().getX();
        int cz = vulture.blockPosition().getZ();
        BlockPos fallback = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            float angle = vulture.getRandom().nextFloat() * Mth.TWO_PI;
            int radius = 4 + vulture.getRandom().nextInt(8);
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
        return !vulture.isActiveTime();
    }

    @Override
    protected void stop(ServerLevel level, VultureEntity vulture, long time) {
        vulture.releasePerch();
        vulture.getNavigation().stop();
        vulture.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        vulture.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        this.destination = null;
        this.joiningFlock = false;
        this.approachTicks = 0;
        this.failedSearches = 0;
        this.nextSearchTime = 0L;
    }
}
