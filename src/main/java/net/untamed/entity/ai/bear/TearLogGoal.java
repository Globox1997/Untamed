package net.untamed.entity.ai.bear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.BlackBearEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class TearLogGoal extends Goal {

    private static final int SEARCH_RANGE = 8;
    private static final double WORK_DISTANCE = 2.2D;
    private static final int TEAR_TICKS = 80;
    private static final int MAX_TICKS = 400;
    private static final int GRUB_FOOD = 2000;
    private static final int COOLDOWN = 1200;

    private final BlackBearEntity bear;
    @Nullable
    private BlockPos log;
    private int ticks;
    private int tearTicks;
    private int repathTicks;
    private long nextAttemptTime;

    public TearLogGoal(BlackBearEntity bear) {
        this.bear = bear;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.bear.isPeckish() || this.bear.isAlarmed() || this.bear.getTarget() != null || this.bear.isResting()
                || this.bear.level().getGameTime() < this.nextAttemptTime || this.bear.getRandom().nextInt(reducedTickDelay(200)) != 0) {
            return false;
        }
        BlockPos feet = this.bear.blockPosition();
        this.log = BlockPos.findClosestMatch(feet, SEARCH_RANGE, 1, pos -> pos.getY() <= feet.getY() && this.bear.level().getBlockState(pos).is(BlockTags.LOGS))
                .map(BlockPos::immutable).orElse(null);
        if (this.log == null) {
            this.nextAttemptTime = this.bear.level().getGameTime() + COOLDOWN / 4;
        }
        return this.log != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.log != null && this.ticks < MAX_TICKS && this.tearTicks < TEAR_TICKS && !this.bear.isAlarmed() && this.bear.getTarget() == null
                && this.bear.level().getBlockState(this.log).is(BlockTags.LOGS);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.tearTicks = 0;
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        if (this.log == null) {
            return;
        }
        this.ticks++;
        Vec3 center = Vec3.atCenterOf(this.log);
        this.bear.getLookControl().setLookAt(center.x, center.y, center.z);
        if (this.bear.position().distanceToSqr(center.x, this.bear.getY(), center.z) > WORK_DISTANCE * WORK_DISTANCE) {
            this.bear.clearBearPose(BlackBearEntity.BearPose.FORAGING);
            if (--this.repathTicks <= 0 || this.bear.getNavigation().isDone()) {
                this.repathTicks = 20;
                this.bear.getNavigation().moveTo(center.x, this.log.getY(), center.z, 1.0D);
            }
            return;
        }
        this.bear.getNavigation().stop();
        this.bear.setBearPose(BlackBearEntity.BearPose.FORAGING);
        if (++this.tearTicks % 10 == 0 && this.bear.level() instanceof ServerLevel serverLevel) {
            BlockState state = serverLevel.getBlockState(this.log);
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), center.x, center.y + 0.4D, center.z, 6, 0.3D, 0.2D, 0.3D, 0.1D);
            this.bear.playSound(SoundEvents.WOOD_HIT, 0.6F, 0.8F);
        }
        if (this.tearTicks >= TEAR_TICKS) {
            this.bear.feed(GRUB_FOOD);
        }
    }

    @Override
    public void stop() {
        this.bear.clearBearPose(BlackBearEntity.BearPose.FORAGING);
        this.bear.getNavigation().stop();
        this.log = null;
        this.nextAttemptTime = this.bear.level().getGameTime() + COOLDOWN;
    }
}
