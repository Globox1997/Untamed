package net.untamed.entity.ai.bear;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.untamed.entity.BlackBearEntity;

public class EatBerriesGoal extends MoveToBlockGoal {

    private static final int EAT_TICKS = 40;
    private static final int RIPE_FOOD = 4000;
    private static final int FULLY_RIPE_FOOD = 6000;

    private final BlackBearEntity bear;
    private int eatTicks;

    public EatBerriesGoal(BlackBearEntity bear) {
        super(bear, 1.0D, 12, 2);
        this.bear = bear;
    }

    @Override
    public boolean canUse() {
        return this.bear.isPeckish() && !this.bear.isAlarmed() && !this.bear.isResting() && this.bear.getTarget() == null
                && this.bear.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.bear.isAlarmed() && this.bear.getTarget() == null && super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        this.eatTicks = 0;
    }

    @Override
    public double acceptedDistance() {
        return 2.0D;
    }

    @Override
    public boolean shouldRecalculatePath() {
        return this.tryTicks % 100 == 0;
    }

    @Override
    protected boolean isValidTarget(LevelReader levelReader, BlockPos blockPos) {
        BlockState blockState = levelReader.getBlockState(blockPos);
        return blockState.is(Blocks.SWEET_BERRY_BUSH) && blockState.getValue(SweetBerryBushBlock.AGE) >= 2;
    }

    @Override
    public void tick() {
        if (this.isReachedTarget()) {
            this.bear.setBearPose(BlackBearEntity.BearPose.EATING);
            this.bear.getLookControl().setLookAt(this.blockPos.getX() + 0.5D, this.blockPos.getY() + 0.5D, this.blockPos.getZ() + 0.5D);
            if (++this.eatTicks >= EAT_TICKS) {
                this.eatBerries();
            }
        } else {
            this.bear.clearBearPose(BlackBearEntity.BearPose.EATING);
        }
        super.tick();
    }

    private void eatBerries() {
        BlockState blockState = this.bear.level().getBlockState(this.blockPos);
        if (!blockState.is(Blocks.SWEET_BERRY_BUSH) || !this.bear.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        int age = blockState.getValue(SweetBerryBushBlock.AGE);
        this.bear.feed(age == SweetBerryBushBlock.MAX_AGE ? FULLY_RIPE_FOOD : RIPE_FOOD);
        this.bear.playSound(SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1.0F, 0.9F);
        this.bear.level().setBlock(this.blockPos, blockState.setValue(SweetBerryBushBlock.AGE, 1), 2);
        this.bear.level().gameEvent(GameEvent.BLOCK_CHANGE, this.blockPos, GameEvent.Context.of(this.bear));
    }

    @Override
    public void stop() {
        super.stop();
        this.bear.clearBearPose(BlackBearEntity.BearPose.EATING);
    }
}
