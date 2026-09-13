package net.untamed.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class WaterUtils {

    private static final int MAX_WATER_SCAN_DEPTH = 8;
    public static final int MIN_OPERABLE_WATER_DEPTH = 2;

    private WaterUtils() {}

    /**
     * Valid escape/leisure water: actual water, surface at or below the entity's feet
     * at least minDepth deep, one block of headroom above the surface.
     */
    @Nullable
    public static BlockPos findNearestWater(ServerLevel level, Entity entity, int horizontalRange, int verticalRange, int minDepth) {
        BlockPos origin = entity.blockPosition();
        return BlockPos.findClosestMatch(origin, horizontalRange, verticalRange,
                pos -> pos.getY() <= origin.getY()
                        && level.getFluidState(pos).is(FluidTags.WATER)
                        && getWaterColumnDepth(level, pos) >= minDepth
                        && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
        ).orElse(null);
    }

    /**
     * Whether the mob can actually walk/swim to pos.
     */
    public static boolean isBlockedOff(PathfinderMob mob, BlockPos pos) {
        Path path = mob.getNavigation().createPath(pos, 0);
        return path == null || !path.canReach();
    }

    /**
     * Total depth of the water column at pos, counting pos itself.
     */
    public static int getWaterColumnDepth(ServerLevel level, BlockPos pos) {
        int depth = 0;
        BlockPos current = pos;
        for (int i = 0; i < MAX_WATER_SCAN_DEPTH; i++) {
            if (!level.getFluidState(current).is(FluidTags.WATER)) break;
            depth++;
            current = current.below();
        }
        return depth;
    }

    /**
     * Depth of the water the entity is currently in, counting its own block.
     */
    public static int getWaterDepth(ServerLevel level, Entity entity) {
        return getWaterColumnDepth(level, entity.blockPosition());
    }

    /**
     * Bottom of the water column at pos, or null if pos itself is not water.
     * Used to project escape destinations down to the riverbed.
     */
    @Nullable
    public static BlockPos findBottomAt(ServerLevel level, BlockPos pos) {
        if (!level.getFluidState(pos).is(FluidTags.WATER)) return null;
        BlockPos current = pos.immutable();
        return descendToRiverbed(level, current);
    }

    /**
     * First water block above the bed at (x, z), via the OCEAN_FLOOR heightmap.
     */
    @Nullable
    public static BlockPos riverbedAt(ServerLevel level, double x, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        BlockPos pos = new BlockPos(bx, level.getHeight(Heightmap.Types.OCEAN_FLOOR, bx, bz), bz);
        return level.getFluidState(pos).is(FluidTags.WATER) ? pos : null;
    }

    @NotNull
    private static BlockPos descendToRiverbed(ServerLevel level, BlockPos from) {
        BlockPos current = from;
        for (int i = 0; i < MAX_WATER_SCAN_DEPTH; i++) {
            BlockPos below = current.below();
            if (!level.getFluidState(below).is(FluidTags.WATER)) return current;
            current = below;
        }
        return current;
    }

    /**
     * Y-height of the seabed beneath the entity's XZ position (OCEAN_FLOOR heightmap).
     */
    @SuppressWarnings("resource")
    public static double oceanFloorBelow(Entity entity) {
        return entity.level().getHeight(Heightmap.Types.OCEAN_FLOOR,
                entity.blockPosition().getX(), entity.blockPosition().getZ());
    }
}