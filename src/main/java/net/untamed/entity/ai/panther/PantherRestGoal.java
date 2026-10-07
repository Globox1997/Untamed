package net.untamed.entity.ai.panther;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.BlackPantherEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class PantherRestGoal extends Goal {

    private static final int SEARCH_RANGE = 16;
    private static final double AT_SPOT = 1.5D;

    private final BlackPantherEntity panther;
    private boolean walking;

    public PantherRestGoal(BlackPantherEntity panther) {
        this.panther = panther;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.canRest() && this.panther.onGround() && !this.panther.isInWater() && this.panther.getRandom().nextInt(reducedTickDelay(40)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canRest() && (this.walking || this.panther.isResting());
    }

    private boolean canRest() {
        return this.panther.isRestTime() && !this.panther.isAlarmed() && this.panther.getTarget() == null && !this.panther.isHunting() && !this.panther.isInLove();
    }

    @Override
    public void start() {
        this.walking = false;
        BlockPos spot = this.findRestSpot();
        if (spot != null && this.panther.position().distanceToSqr(Vec3.atBottomCenterOf(spot)) > AT_SPOT * AT_SPOT) {
            this.walking = this.panther.getNavigation().moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, 0.9D);
        }
        if (!this.walking) {
            this.lieDown();
        }
    }

    @Nullable
    private BlockPos findRestSpot() {
        if (this.panther.isBaby()) {
            return this.panther.getDenPos();
        }
        for (BlackPantherEntity cub : this.panther.getCubs()) {
            if (cub.isAlive() && cub.isYoungCub() && cub.getDenPos() != null) {
                return cub.getDenPos();
            }
        }
        if (this.panther.scoreCoverSpot(this.panther.blockPosition()) > 0.0D) {
            return null;
        }
        Vec3 cover = LandRandomPos.getPos(this.panther, SEARCH_RANGE, 6, this.panther::scoreCoverSpot);
        return cover != null && this.panther.scoreCoverSpot(BlockPos.containing(cover)) > 0.0D ? BlockPos.containing(cover) : null;
    }

    @Override
    public void tick() {
        if (this.walking) {
            if (this.panther.getNavigation().isDone()) {
                this.walking = false;
                this.lieDown();
            }
        } else {
            this.panther.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.walking = false;
        this.panther.clearPantherPose(BlackPantherEntity.PantherPose.RESTING);
    }

    private void lieDown() {
        this.panther.getNavigation().stop();
        this.panther.setPantherPose(BlackPantherEntity.PantherPose.RESTING);
    }
}
