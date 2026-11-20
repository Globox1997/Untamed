package net.untamed.entity.ai.raccoon;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.untamed.entity.RaccoonEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class SitUpGoal extends Goal {

    private static final double CURIOUS_RANGE = 10.0D;
    private static final int FOOD_CHANCE = 20;
    private static final int PLAYER_CHANCE = 80;
    private static final int RANDOM_CHANCE = 600;
    private static final int MIN_TICKS = 40;
    private static final int TICKS_VARIATION = 40;

    private final RaccoonEntity raccoon;
    @Nullable
    private Player player;
    private int ticks;
    private float lookYaw;

    public SitUpGoal(RaccoonEntity raccoon) {
        this.raccoon = raccoon;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.raccoon.getRaccoonPose() != RaccoonEntity.RaccoonPose.NONE || !this.raccoon.onGround() || this.raccoon.isInWater() || this.raccoon.isAlarmed()
                || !this.raccoon.getNavigation().isDone()) {
            return false;
        }
        this.player = this.raccoon.level().getNearestPlayer(this.raccoon, CURIOUS_RANGE);
        if (this.player != null && this.player.isSpectator()) {
            this.player = null;
        }
        int chance = this.player == null ? RANDOM_CHANCE
                : this.raccoon.isFood(this.player.getMainHandItem()) || this.raccoon.isFood(this.player.getOffhandItem()) ? FOOD_CHANCE : PLAYER_CHANCE;
        return this.raccoon.getRandom().nextInt(reducedTickDelay(chance)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.ticks > 0 && !this.raccoon.isAlarmed() && this.raccoon.getRaccoonPose() == RaccoonEntity.RaccoonPose.SITTING_UP;
    }

    @Override
    public void start() {
        this.ticks = this.adjustedTickDelay(MIN_TICKS + this.raccoon.getRandom().nextInt(TICKS_VARIATION));
        this.lookYaw = this.raccoon.getRandom().nextFloat() * Mth.TWO_PI;
        this.raccoon.getNavigation().stop();
        this.raccoon.setRaccoonPose(RaccoonEntity.RaccoonPose.SITTING_UP);
    }

    @Override
    public void tick() {
        this.ticks--;
        if (this.player != null && this.player.isAlive()) {
            this.raccoon.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
        } else {
            this.raccoon.getLookControl().setLookAt(this.raccoon.getX() - Mth.sin(this.lookYaw) * 6.0D, this.raccoon.getEyeY() + 1.0D, this.raccoon.getZ() + Mth.cos(this.lookYaw) * 6.0D);
        }
    }

    @Override
    public void stop() {
        this.raccoon.clearRaccoonPose(RaccoonEntity.RaccoonPose.SITTING_UP);
        this.player = null;
    }
}
