package net.untamed.entity.ai.octopus;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.OctopusEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class OctopusDenGoal extends Goal {

    private static final int SEARCH_TRIES = 32;
    private static final int SEARCH_HORIZONTAL = 12;
    private static final int SEARCH_VERTICAL = 4;
    private static final double MAX_DEN_DISTANCE = 32.0D;
    private static final double AT_DEN = 1.2D;

    private final OctopusEntity octopus;
    private boolean walking;

    public OctopusDenGoal(OctopusEntity octopus) {
        this.octopus = octopus;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.octopus.isRestTime() && this.octopus.isInWater() && !this.octopus.isThreatened() && this.octopus.getTarget() == null
                && this.octopus.getRandom().nextInt(reducedTickDelay(20)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.octopus.isRestTime() && this.octopus.isInWater() && !this.octopus.isThreatened() && (this.walking || this.octopus.isResting());
    }

    @Override
    public void start() {
        this.walking = false;
        BlockPos den = this.octopus.getDenPos();
        if (den == null || !this.octopus.blockPosition().closerThan(den, MAX_DEN_DISTANCE) || !this.octopus.isDenSpot(den)) {
            den = findDen(this.octopus);
            this.octopus.setDenPos(den);
        }
        if (den != null && this.octopus.position().distanceToSqr(Vec3.atBottomCenterOf(den)) > AT_DEN * AT_DEN) {
            this.walking = this.octopus.getNavigation().moveTo(den.getX() + 0.5D, den.getY(), den.getZ() + 0.5D, 1.0D);
        }
        if (!this.walking) {
            this.rest();
        }
    }

    @Override
    public void tick() {
        if (this.walking) {
            if (this.octopus.getNavigation().isDone()) {
                this.walking = false;
                this.rest();
            }
        } else {
            this.octopus.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        this.walking = false;
        this.octopus.clearOctopusPose(OctopusEntity.OctopusPose.RESTING);
    }

    private void rest() {
        this.octopus.getNavigation().stop();
        this.octopus.setOctopusPose(OctopusEntity.OctopusPose.RESTING);
    }

    @Nullable
    public static BlockPos findDen(OctopusEntity octopus) {
        RandomSource random = octopus.getRandom();
        BlockPos origin = octopus.blockPosition();
        BlockPos best = null;
        int bestScore = 2;
        for (int i = 0; i < SEARCH_TRIES; i++) {
            BlockPos candidate = origin.offset(random.nextInt(SEARCH_HORIZONTAL * 2 + 1) - SEARCH_HORIZONTAL,
                    random.nextInt(SEARCH_VERTICAL * 2 + 1) - SEARCH_VERTICAL, random.nextInt(SEARCH_HORIZONTAL * 2 + 1) - SEARCH_HORIZONTAL);
            int score = octopus.scoreDenSpot(candidate);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }
}
