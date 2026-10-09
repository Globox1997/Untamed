package net.untamed.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

public final class LandUtils {

    private LandUtils() {}

    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private static final int WALL_SEARCH_DISTANCE = 3;
    private static final int WALL_MIN_DISTANCE = 2;
    private static final int MIN_WALL_HEIGHT = 3;
    private static final int MIN_WALL_WIDTH = 3;
    private static final int MAX_WALL_WIDTH_CHECK = 4;
    private static final int MAX_COVER_HEIGHT = 4;

    private static final int SCORE_WALL = 10;
    private static final int SCORE_HEIGHT_BONUS = 2;
    private static final int SCORE_WIDTH_BONUS = 1;
    private static final int SCORE_ENCLOSURE = 5;
    private static final int SCORE_COVER = 8;

    public static int roostScore(ServerLevel level, BlockPos pos) {
        if (!level.isEmptyBlock(pos)) return 0;
        if (level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) return 0;
        int feetY = pos.getY();

        int score = 0;
        int walls = 0;
        for (Direction dir : HORIZONTALS) {
            int wall = wallScore(level, pos, dir, feetY);
            if (wall > 0) {
                walls++;
                score += wall;
            }
        }
        boolean covered = hasCoverAbove(level, pos);
        if (walls < 2 && !(walls == 1 && covered)) return 0; // expuesto: ni 2 paredes ni tejadito
        if (walls >= 3) score += SCORE_ENCLOSURE;
        if (covered) score += SCORE_COVER;
        return score;
    }

    public static boolean isRoost(ServerLevel level, BlockPos pos) { return roostScore(level, pos) > 0; }

    private static int wallScore(ServerLevel level, BlockPos pos, Direction dir, int feetY) {
        int best = 0;
        for (int dist = WALL_MIN_DISTANCE; dist <= WALL_SEARCH_DISTANCE; dist++) {
            int cx = pos.getX() + dir.getStepX() * dist;
            int cz = pos.getZ() + dir.getStepZ() * dist;
            int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz) - feetY;
            if (height < MIN_WALL_HEIGHT) continue;
            int width = wallWidth(level, feetY, cx, cz, dir);
            if (width < MIN_WALL_WIDTH) continue;
            best = Math.max(best, SCORE_WALL + (height - MIN_WALL_HEIGHT) * SCORE_HEIGHT_BONUS + (width - MIN_WALL_WIDTH) * SCORE_WIDTH_BONUS);
        }
        return best;
    }

    private static int wallWidth(ServerLevel level, int feetY, int cx, int cz, Direction dir) {
        int px = dir.getStepX() != 0 ? 0 : 1;
        int pz = dir.getStepZ() != 0 ? 0 : 1;
        int width = 1;
        for (int side = 1; side <= MAX_WALL_WIDTH_CHECK; side++) {
            if (columnHeight(level, feetY, cx + px * side, cz + pz * side) < MIN_WALL_HEIGHT - 1) break;
            width++;
        }
        for (int side = 1; side <= MAX_WALL_WIDTH_CHECK; side++) {
            if (columnHeight(level, feetY, cx - px * side, cz - pz * side) < MIN_WALL_HEIGHT - 1) break;
            width++;
        }
        return width;
    }

    private static int columnHeight(ServerLevel level, int feetY, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - feetY;
    }

    private static boolean hasCoverAbove(ServerLevel level, BlockPos pos) {
        for (int i = 1; i <= MAX_COVER_HEIGHT; i++) {
            BlockPos up = pos.above(i);
            if (!level.getBlockState(up).getCollisionShape(level, up).isEmpty()) return true;
        }
        return false;
    }

    @Nullable
    public static BlockPos findBestRoost(ServerLevel level, Entity entity, int horizontalRange, int depthBelowSurface) {
        BlockPos origin = entity.blockPosition();
        int ox = origin.getX(), oz = origin.getZ();
        BlockPos best = null;
        int bestScore = 0;
        double bestDistSqr = Double.MAX_VALUE;
        for (int dx = -horizontalRange; dx <= horizontalRange; dx++)
            for (int dz = -horizontalRange; dz <= horizontalRange; dz++) {
                int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, ox + dx, oz + dz);
                for (int y = surfaceY; y >= surfaceY - depthBelowSurface; y--) {
                    BlockPos pos = new BlockPos(ox + dx, y, oz + dz);
                    int score = roostScore(level, pos);
                    if (score > 0) {
                        double distSqr = dx * dx + dz * dz;
                        if (score > bestScore || (score == bestScore && distSqr < bestDistSqr)) {
                            bestScore = score;
                            bestDistSqr = distSqr;
                            best = pos;
                        }
                    }
                }
            }
        return best;
    }

    public static float nearestDryYaw(Level level, Entity entity, int maxRange) {
        BlockPos origin = entity.blockPosition();
        for (int radius = 4; radius <= maxRange; radius += 4)
            for (int i = 0; i < 8; i++) {
                float angle = i * Mth.TWO_PI / 8.0F;
                int x = origin.getX() + Mth.floor(Mth.cos(angle) * radius);
                int z = origin.getZ() + Mth.floor(Mth.sin(angle) * radius);
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                    double dx = x + 0.5 - entity.getX();
                    double dz = z + 0.5 - entity.getZ();
                    return (float) (Mth.atan2(-dx, dz) * (180F / Math.PI));
                }
            }
        return Float.NaN;
    }

    public static int terrainRoughness(Level level, int x, int z) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < 8; i++) {
            float angle = i * Mth.TWO_PI / 8.0F;
            int sx = x + Mth.floor(Mth.cos(angle) * 3);
            int sz = z + Mth.floor(Mth.sin(angle) * 3);
            int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, sx, sz);
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        return max - min;
    }
}