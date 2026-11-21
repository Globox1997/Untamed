package net.untamed.entity.ai.bird;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractBirdEntity;
import net.untamed.entity.ai.LandUtils;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BirdRoostGoal extends Goal {

    private static final int START_CHANCE = 20;
    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_TRIES = 24;
    private static final int MAX_DEPTH = 6;
    private static final int COVER_HEIGHT = 4;
    private static final double ARRIVE_DISTANCE = 1.0D;
    private static final int MAX_APPROACH_TICKS = 400;

    private final AbstractBirdEntity bird;
    @Nullable
    private BlockPos roost;
    private boolean approaching;
    private int ticks;

    public BirdRoostGoal(AbstractBirdEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (!this.bird.isRoostTime() || this.bird.isAlarmed() || this.bird.isInLove() || this.bird.getRandom().nextInt(reducedTickDelay(START_CHANCE)) != 0
                || !(this.bird.level() instanceof ServerLevel level)) {
            return false;
        }
        this.roost = this.findRoost(level);
        return this.roost != null;
    }

    @Nullable
    private BlockPos findRoost(ServerLevel level) {
        BlockPos origin = this.bird.blockPosition();
        for (int i = 0; i < SEARCH_TRIES; i++) {
            int x = origin.getX() + this.bird.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int z = origin.getZ() + this.bird.getRandom().nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            for (int depth = 1; depth <= MAX_DEPTH; depth++) {
                BlockPos pos = new BlockPos(x, top - depth, z);
                if (isCoveredRoost(level, pos)) {
                    return pos;
                }
            }
        }
        if (this.bird.onGround() && isPerchBlock(level.getBlockState(origin.below()))) {
            return origin;
        }
        return LandUtils.findAerialPerch(level, this.bird.getRandom(), origin, SEARCH_RADIUS, SEARCH_TRIES);
    }

    private static boolean isCoveredRoost(Level level, BlockPos pos) {
        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above()) || !isPerchBlock(level.getBlockState(pos.below()))) {
            return false;
        }
        for (int dy = 2; dy <= COVER_HEIGHT; dy++) {
            if (level.getBlockState(pos.above(dy)).is(BlockTags.LEAVES)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPerchBlock(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.roost == null || !this.bird.isRoostTime() || this.bird.isAlarmed()) {
            return false;
        }
        return !this.approaching || this.ticks < MAX_APPROACH_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.approaching = true;
        this.bird.clearBirdPose(AbstractBirdEntity.BirdPose.PERCHED);
        this.bird.getNavigation().moveTo(this.roost.getX() + 0.5D, this.roost.getY(), this.roost.getZ() + 0.5D, 1.0D);
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.approaching) {
            Vec3 target = Vec3.atBottomCenterOf(this.roost);
            double dx = target.x - this.bird.getX();
            double dz = target.z - this.bird.getZ();
            if (dx * dx + dz * dz < ARRIVE_DISTANCE * ARRIVE_DISTANCE && Math.abs(target.y - this.bird.getY()) < 1.5D) {
                this.approaching = false;
                this.bird.getNavigation().stop();
                this.bird.setBirdPose(AbstractBirdEntity.BirdPose.SLEEPING);
            } else if (this.bird.getNavigation().isDone()) {
                this.bird.getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
            }
            return;
        }
        this.bird.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.bird.clearBirdPose(AbstractBirdEntity.BirdPose.SLEEPING);
        this.roost = null;
    }
}
