package net.untamed.entity.ai.raccoon;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.untamed.entity.RaccoonEntity;
import net.untamed.init.ConfigInit;

public class RaidGoal extends MoveToBlockGoal {

    private static final int RAID_TICKS = 30;
    private static final int CROP_AGE_LOSS = 3;
    private static final double ACCEPTED_DISTANCE = 2.0D;

    private final RaccoonEntity raccoon;
    private int raidTicks;
    private boolean openedContainer;

    public RaidGoal(RaccoonEntity raccoon) {
        super(raccoon, 1.0D, 12, 2);
        this.raccoon = raccoon;
    }

    @Override
    public boolean canUse() {
        return !this.raccoon.isBaby() && !this.raccoon.isRestTime() && this.raccoon.getMainHandItem().isEmpty() && this.raccoon.isHungry()
                && !this.raccoon.isAlarmed() && !this.raccoon.isResting() && (ConfigInit.CONFIG.raccoonsRaidCrops || ConfigInit.CONFIG.raccoonsRaidContainers)
                && this.raccoon.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.raccoon.isAlarmed() && this.raccoon.getMainHandItem().isEmpty() && super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        this.raidTicks = 0;
        this.openedContainer = false;
    }

    @Override
    public double acceptedDistance() {
        return ACCEPTED_DISTANCE;
    }

    @Override
    public boolean shouldRecalculatePath() {
        return this.tryTicks % 100 == 0;
    }

    @Override
    protected boolean isValidTarget(LevelReader levelReader, BlockPos blockPos) {
        BlockState state = levelReader.getBlockState(blockPos);
        if (ConfigInit.CONFIG.raccoonsRaidCrops) {
            if (state.is(Blocks.SWEET_BERRY_BUSH) && state.getValue(SweetBerryBushBlock.AGE) >= 2) {
                return true;
            }
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                return true;
            }
        }
        if (ConfigInit.CONFIG.raccoonsRaidContainers) {
            BlockEntity blockEntity = levelReader.getBlockEntity(blockPos);
            return (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity) && this.findFoodSlot((Container) blockEntity) >= 0;
        }
        return false;
    }

    private int findFoodSlot(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (this.raccoon.isFood(container.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public void tick() {
        super.tick();
        BlockPos target = this.getMoveToTarget().below();
        this.raccoon.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        if (!this.isReachedTarget()) {
            this.raidTicks = 0;
            return;
        }
        this.raccoon.setRaccoonPose(RaccoonEntity.RaccoonPose.FORAGING);
        if (this.raidTicks == 0) {
            this.openContainer(target, true);
        }
        if (++this.raidTicks >= RAID_TICKS) {
            this.raid(target);
            this.openContainer(target, false);
            this.nextStartTick = this.nextStartTick(this.mob);
            this.tryTicks = 1201;
        }
    }

    private void raid(BlockPos pos) {
        Level level = this.raccoon.level();
        BlockState state = level.getBlockState(pos);
        if (!this.isValidTarget(level, pos)) {
            return;
        }
        if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            level.setBlock(pos, state.setValue(SweetBerryBushBlock.AGE, 1), 2);
            this.raccoon.onFed(2.0F);
        } else if (state.getBlock() instanceof CropBlock crop) {
            level.setBlock(pos, crop.getStateForAge(Math.max(0, crop.getAge(state) - CROP_AGE_LOSS)), 2);
            this.raccoon.onFed(2.0F);
        } else if (level.getBlockEntity(pos) instanceof Container container) {
            int slot = this.findFoodSlot(container);
            if (slot >= 0) {
                ItemStack taken = container.getItem(slot).split(1);
                container.setChanged();
                this.raccoon.holdItem(taken);
            }
        }
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(this.raccoon, state));
    }

    private void openContainer(BlockPos pos, boolean open) {
        Level level = this.raccoon.level();
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ChestBlockEntity) {
            if (open || this.openedContainer) {
                level.blockEvent(pos, state.getBlock(), 1, open ? 1 : 0);
                level.playSound(null, pos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.4F, 1.1F);
                this.openedContainer = open;
            }
        } else if (blockEntity instanceof BarrelBlockEntity && state.hasProperty(BarrelBlock.OPEN)) {
            if (open || this.openedContainer) {
                level.setBlock(pos, state.setValue(BarrelBlock.OPEN, open), 3);
                level.playSound(null, pos, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.4F, 1.1F);
                this.openedContainer = open;
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.openedContainer) {
            this.openContainer(this.blockPos, false);
        }
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.FORAGING);
    }
}
