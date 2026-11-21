package net.untamed.entity.ai.bird;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractBirdEntity;
import net.untamed.entity.ai.LandUtils;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class BirdPerchGoal extends Goal {

    private static final int START_CHANCE = 40;
    private static final int SEARCH_RADIUS = 16;
    private static final int GROUP_SEARCH_RADIUS = 4;
    private static final int SEARCH_TRIES = 24;
    private static final double ARRIVE_DISTANCE = 1.0D;
    private static final int MAX_APPROACH_TICKS = 300;
    private static final int MIN_COOLDOWN = 60;
    private static final int COOLDOWN_VARIATION = 100;

    private final AbstractBirdEntity bird;
    @Nullable
    private BlockPos perch;
    private boolean approaching;
    private int ticks;
    private int perchTicks;
    private long nextPerchTime;

    public BirdPerchGoal(AbstractBirdEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.bird.isBusy() || this.bird.isAlarmed() || this.bird.isRoostTime() || this.bird.level().getGameTime() < this.nextPerchTime
                || this.bird.getRandom().nextInt(reducedTickDelay(START_CHANCE)) != 0 || !(this.bird.level() instanceof ServerLevel level)) {
            return false;
        }
        if (this.bird.onGround() && isPerchBlock(this.bird.level().getBlockState(this.bird.blockPosition().below()))) {
            this.perch = this.bird.blockPosition();
            return true;
        }
        BlockPos preferred = this.bird.getPreferredPerchCenter();
        BlockPos center = preferred != null ? preferred : this.bird.blockPosition();
        this.perch = LandUtils.findAerialPerch(level, this.bird.getRandom(), center, preferred != null ? GROUP_SEARCH_RADIUS : SEARCH_RADIUS, SEARCH_TRIES);
        return this.perch != null;
    }

    private static boolean isPerchBlock(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS);
    }

    @Override
    public boolean canContinueToUse() {
        if (this.perch == null || this.bird.isAlarmed() || this.bird.isRoostTime() || this.bird.isBusy()) {
            return false;
        }
        return this.approaching ? this.ticks < MAX_APPROACH_TICKS : this.perchTicks > 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.approaching = true;
        this.bird.getNavigation().moveTo(this.perch.getX() + 0.5D, this.perch.getY(), this.perch.getZ() + 0.5D, 1.0D);
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.approaching) {
            Vec3 target = Vec3.atBottomCenterOf(this.perch);
            double dx = target.x - this.bird.getX();
            double dz = target.z - this.bird.getZ();
            if (dx * dx + dz * dz < ARRIVE_DISTANCE * ARRIVE_DISTANCE && Math.abs(target.y - this.bird.getY()) < 1.5D) {
                this.land();
            } else if (this.bird.getNavigation().isDone()) {
                this.bird.getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
            }
            return;
        }
        this.perchTicks--;
        this.bird.getNavigation().stop();
    }

    private void land() {
        this.approaching = false;
        this.perchTicks = this.bird.getMinPerchTicks() + this.bird.getRandom().nextInt(this.bird.getPerchTicksVariation());
        this.bird.getNavigation().stop();
        this.bird.setBirdPose(AbstractBirdEntity.BirdPose.PERCHED);
    }

    @Override
    public void stop() {
        this.bird.clearBirdPose(AbstractBirdEntity.BirdPose.PERCHED);
        this.nextPerchTime = this.bird.level().getGameTime() + MIN_COOLDOWN + this.bird.getRandom().nextInt(COOLDOWN_VARIATION);
        this.perch = null;
    }
}
