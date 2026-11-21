package net.untamed.entity.ai.quetzal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractBirdEntity;
import net.untamed.entity.QuetzalEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class HoverPluckGoal extends Goal {

    private static final int START_CHANCE = 100;
    private static final int SEARCH_HORIZONTAL = 10;
    private static final int SEARCH_VERTICAL = 5;
    private static final double HOVER_OFFSET = 1.0D;
    private static final double HOVER_REACHED = 0.8D;
    private static final double APPROACH_SPEED = 1.0D;
    private static final int HOVER_TICKS = 25;
    private static final int MAX_TICKS = 300;

    private final QuetzalEntity quetzal;
    @Nullable
    private BlockPos fruit;
    @Nullable
    private Vec3 hoverPoint;
    private int ticks;
    private int hoverTicks;

    public HoverPluckGoal(QuetzalEntity quetzal) {
        this.quetzal = quetzal;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        Level level = this.quetzal.level();
        if (!this.quetzal.isHungry() || this.quetzal.isAlarmed() || this.quetzal.isBusy() || this.quetzal.isRoostTime()
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) || this.quetzal.getRandom().nextInt(reducedTickDelay(START_CHANCE)) != 0) {
            return false;
        }
        this.fruit = BlockPos.findClosestMatch(this.quetzal.blockPosition(), SEARCH_HORIZONTAL, SEARCH_VERTICAL, pos -> isRipeFruit(level.getBlockState(pos))).orElse(null);
        if (this.fruit == null) {
            return false;
        }
        Vec3 center = Vec3.atCenterOf(this.fruit);
        Vec3 toBird = this.quetzal.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
        Vec3 offset = toBird.lengthSqr() > 1.0E-4D ? toBird.normalize().scale(HOVER_OFFSET) : new Vec3(HOVER_OFFSET, 0.0D, 0.0D);
        this.hoverPoint = level.getBlockState(this.fruit).is(Blocks.SWEET_BERRY_BUSH) ? center.add(0.0D, 1.0D, 0.0D) : center.add(offset).subtract(0.0D, 0.3D, 0.0D);
        return true;
    }

    private static boolean isRipeFruit(BlockState state) {
        if (state.is(Blocks.COCOA)) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        return (state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT)) && CaveVines.hasGlowBerries(state);
    }

    @Override
    public boolean canContinueToUse() {
        return this.fruit != null && this.hoverPoint != null && this.ticks < MAX_TICKS && !this.quetzal.isAlarmed()
                && isRipeFruit(this.quetzal.level().getBlockState(this.fruit));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.ticks = 0;
        this.hoverTicks = 0;
        this.quetzal.clearBirdPose(AbstractBirdEntity.BirdPose.PERCHED);
        this.quetzal.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.fruit == null || this.hoverPoint == null) {
            return;
        }
        this.ticks++;
        this.quetzal.getMoveControl().setWantedPosition(this.hoverPoint.x, this.hoverPoint.y, this.hoverPoint.z, APPROACH_SPEED);
        this.quetzal.getLookControl().setLookAt(Vec3.atCenterOf(this.fruit));
        if (this.quetzal.position().distanceToSqr(this.hoverPoint) > HOVER_REACHED * HOVER_REACHED) {
            return;
        }
        this.quetzal.setBirdPose(AbstractBirdEntity.BirdPose.HOVERING);
        if (++this.hoverTicks >= HOVER_TICKS) {
            this.pluck();
        }
    }

    private void pluck() {
        Level level = this.quetzal.level();
        BlockState state = level.getBlockState(this.fruit);
        ItemStack seed;
        if (state.is(Blocks.COCOA)) {
            level.setBlock(this.fruit, state.setValue(CocoaBlock.AGE, 0), 2);
            seed = new ItemStack(Items.COCOA_BEANS);
        } else if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            level.setBlock(this.fruit, state.setValue(SweetBerryBushBlock.AGE, 1), 2);
            seed = new ItemStack(Items.SWEET_BERRIES);
        } else {
            level.setBlock(this.fruit, state.setValue(CaveVines.BERRIES, false), 2);
            seed = new ItemStack(Items.GLOW_BERRIES);
        }
        level.gameEvent(GameEvent.BLOCK_CHANGE, this.fruit, GameEvent.Context.of(this.quetzal, state));
        this.quetzal.onFed(2.0F);
        this.quetzal.carrySeed(seed);
        this.fruit = null;
    }

    @Override
    public void stop() {
        this.quetzal.clearBirdPose(AbstractBirdEntity.BirdPose.HOVERING);
        this.fruit = null;
        this.hoverPoint = null;
    }
}
