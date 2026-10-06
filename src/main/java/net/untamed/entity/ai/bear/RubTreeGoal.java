package net.untamed.entity.ai.bear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.BlackBearEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class RubTreeGoal extends Goal {

    private static final int SEARCH_RANGE = 8;
    private static final int RUB_CHANCE = 600;
    private static final int COOLDOWN = 2400;
    private static final int MAX_WALK_TICKS = 300;
    private static final double ARRIVE_DISTANCE = 1.2D;

    private final BlackBearEntity bear;
    @Nullable
    private BlockPos trunk;
    @Nullable
    private BlockPos standPos;
    private boolean rubbing;
    private int ticks;
    private int rubTicks;
    private int rubDuration;
    private int repathTicks;
    private long nextAttemptTime;

    public RubTreeGoal(BlackBearEntity bear) {
        this.bear = bear;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.bear.isBaby() || this.bear.isAlarmed() || this.bear.getTarget() != null || this.bear.isResting()
                || this.bear.level().getGameTime() < this.nextAttemptTime || this.bear.getRandom().nextInt(reducedTickDelay(RUB_CHANCE)) != 0) {
            return false;
        }
        BlockPos feet = this.bear.blockPosition();
        this.trunk = BlockPos.findClosestMatch(feet, SEARCH_RANGE, 1, pos -> pos.getY() == feet.getY() && this.isTrunk(pos)).map(BlockPos::immutable).orElse(null);
        this.standPos = this.trunk != null ? this.findStandPos(this.trunk) : null;
        if (this.standPos == null) {
            this.nextAttemptTime = this.bear.level().getGameTime() + COOLDOWN / 4;
            return false;
        }
        return true;
    }

    private boolean isTrunk(BlockPos pos) {
        BlockState state = this.bear.level().getBlockState(pos);
        return state.is(BlockTags.LOGS) && state.hasProperty(RotatedPillarBlock.AXIS) && state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y
                && this.bear.level().getBlockState(pos.above()).is(BlockTags.LOGS);
    }

    @Nullable
    private BlockPos findStandPos(BlockPos trunkPos) {
        Level level = this.bear.level();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos feet = trunkPos.relative(direction);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                    && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                    && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
                return feet;
            }
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.trunk == null || this.bear.isAlarmed() || this.bear.getTarget() != null || !this.isTrunk(this.trunk)) {
            return false;
        }
        return this.rubbing ? this.rubTicks < this.rubDuration : this.ticks < MAX_WALK_TICKS;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.rubbing = false;
        this.ticks = 0;
        this.rubTicks = 0;
        this.rubDuration = 100 + this.bear.getRandom().nextInt(60);
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        if (this.trunk == null || this.standPos == null) {
            return;
        }
        this.ticks++;
        Vec3 stand = Vec3.atBottomCenterOf(this.standPos);
        if (!this.rubbing) {
            if (this.bear.position().distanceToSqr(stand) < ARRIVE_DISTANCE * ARRIVE_DISTANCE) {
                this.rubbing = true;
                this.bear.getNavigation().stop();
            } else if (--this.repathTicks <= 0 || this.bear.getNavigation().isDone()) {
                this.repathTicks = 20;
                this.bear.getNavigation().moveTo(stand.x, stand.y, stand.z, 0.9D);
            }
            return;
        }
        this.bear.getNavigation().stop();
        this.bear.setBearPose(BlackBearEntity.BearPose.RUBBING);
        Vec3 trunkCenter = Vec3.atBottomCenterOf(this.trunk);
        float awayYaw = (float) (Mth.atan2(this.bear.getZ() - trunkCenter.z, this.bear.getX() - trunkCenter.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.bear.setYRot(awayYaw);
        this.bear.setYBodyRot(awayYaw);
        this.bear.setYHeadRot(awayYaw);
        if (++this.rubTicks % 20 == 0 && this.bear.level() instanceof ServerLevel serverLevel) {
            BlockState state = serverLevel.getBlockState(this.trunk);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), trunkCenter.x, this.trunk.getY() + 1.2D, trunkCenter.z, 5, 0.3D, 0.4D, 0.3D, 0.05D);
            this.bear.playSound(SoundEvents.WOOD_HIT, 0.4F, 0.7F);
        }
    }

    @Override
    public void stop() {
        this.bear.clearBearPose(BlackBearEntity.BearPose.RUBBING);
        this.bear.getNavigation().stop();
        this.trunk = null;
        this.standPos = null;
        this.nextAttemptTime = this.bear.level().getGameTime() + COOLDOWN;
    }
}
