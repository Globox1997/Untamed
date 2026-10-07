package net.untamed.entity.ai.kiwi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.untamed.entity.KiwiEntity;
import net.untamed.init.TagInit;

import java.util.EnumSet;

public class ProbeGoal extends Goal {

    private static final int START_CHANCE = 100;
    private static final double STEP_DISTANCE = 1.2D;
    private static final double STEP_SPEED = 0.5D;

    private final KiwiEntity kiwi;
    private int ticks;
    private int probeTicks;
    private int nextProbe;
    private boolean effectsPlayed;

    public ProbeGoal(KiwiEntity kiwi) {
        this.kiwi = kiwi;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.canProbe() && this.kiwi.getRandom().nextInt(reducedTickDelay(START_CHANCE)) == 0
                && this.kiwi.level().getBlockState(this.kiwi.blockPosition().below()).is(TagInit.KIWI_PROBE_BLOCKS);
    }

    @Override
    public boolean canContinueToUse() {
        return this.ticks > 0 && this.canProbe();
    }

    private boolean canProbe() {
        return !this.kiwi.isRestTime() && !this.kiwi.isAlarmed() && !this.kiwi.isSleeping() && this.kiwi.onGround() && !this.kiwi.isInWater();
    }

    @Override
    public void start() {
        this.ticks = this.adjustedTickDelay(200 + this.kiwi.getRandom().nextInt(200));
        this.probeTicks = 0;
        this.nextProbe = 0;
        this.kiwi.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.ticks--;
        if (this.probeTicks > 0) {
            this.probeTicks--;
            if (!this.effectsPlayed && this.probeTicks <= this.adjustedTickDelay(15)) {
                this.effectsPlayed = true;
                this.probeEffects();
            }
            if (this.probeTicks == 0) {
                this.kiwi.clearKiwiPose(KiwiEntity.KiwiPose.PROBING);
                this.nextProbe = this.adjustedTickDelay(20 + this.kiwi.getRandom().nextInt(20));
                this.stepForward();
            }
            return;
        }
        if (--this.nextProbe <= 0) {
            this.probeTicks = this.adjustedTickDelay(30 + this.kiwi.getRandom().nextInt(20));
            this.effectsPlayed = false;
            this.kiwi.getNavigation().stop();
            this.kiwi.setKiwiPose(KiwiEntity.KiwiPose.PROBING);
        }
    }

    private void stepForward() {
        float yaw = (this.kiwi.getYRot() + (this.kiwi.getRandom().nextFloat() - 0.5F) * 90.0F) * Mth.DEG_TO_RAD;
        this.kiwi.getNavigation().moveTo(this.kiwi.getX() - Mth.sin(yaw) * STEP_DISTANCE, this.kiwi.getY(), this.kiwi.getZ() + Mth.cos(yaw) * STEP_DISTANCE, STEP_SPEED);
    }

    private void probeEffects() {
        BlockPos ground = this.kiwi.blockPosition().below();
        BlockState state = this.kiwi.level().getBlockState(ground);
        if (!state.is(TagInit.KIWI_PROBE_BLOCKS) || !(this.kiwi.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float yaw = this.kiwi.getYRot() * Mth.DEG_TO_RAD;
        double tipX = this.kiwi.getX() - Mth.sin(yaw) * 0.45D;
        double tipZ = this.kiwi.getZ() + Mth.cos(yaw) * 0.45D;
        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), tipX, this.kiwi.getY() + 0.05D, tipZ, 4, 0.05D, 0.02D, 0.05D, 0.05D);
        this.kiwi.playSnuffle();
    }

    @Override
    public void stop() {
        this.kiwi.clearKiwiPose(KiwiEntity.KiwiPose.PROBING);
    }
}
