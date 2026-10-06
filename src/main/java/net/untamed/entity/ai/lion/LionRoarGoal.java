package net.untamed.entity.ai.lion;

import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.AbstractLionEntity;

import java.util.EnumSet;

public class LionRoarGoal extends Goal {

    private static final int ROAR_TICKS = 50;
    private static final int SOUND_TICK = 10;
    private static final int ROAR_CHANCE = 600;
    private static final int MIN_TICKS_BETWEEN_ROARS = 2400;
    private static final double CHORUS_RANGE = 32.0D;
    private static final double HEARING_RANGE = 64.0D;

    private final AbstractLionEntity lion;
    private boolean chorus;
    private int ticks;

    public LionRoarGoal(AbstractLionEntity lion) {
        this.lion = lion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.lion.isBaby() || this.lion.getTarget() != null || this.lion.getHunt() != null || this.lion.isSleeping()) {
            return false;
        }
        if (this.lion.hasRoarRequest()) {
            return true;
        }
        if (this.lion.isRestTime() || this.lion.isNomad() || this.lion.level().getGameTime() - this.lion.getLastHeardRoarTime() < MIN_TICKS_BETWEEN_ROARS) {
            return false;
        }
        return this.lion.getRandom().nextInt(reducedTickDelay(ROAR_CHANCE)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.ticks < ROAR_TICKS && this.lion.getTarget() == null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.chorus = this.lion.hasRoarRequest() && this.lion.level().getGameTime() - this.lion.getLastHeardRoarTime() < 100;
        this.lion.clearRoarRequest();
        this.ticks = 0;
        this.lion.getNavigation().stop();
        this.lion.setLionPose(AbstractLionEntity.LionPose.ROARING);
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.ticks == SOUND_TICK) {
            this.lion.playTerritoryRoar();
            if (!this.chorus) {
                this.broadcast();
            }
        }
    }

    @Override
    public void stop() {
        this.lion.clearLionPose(AbstractLionEntity.LionPose.ROARING);
    }

    private void broadcast() {
        this.lion.hearRoar(this.lion);
        for (AbstractLionEntity other : this.lion.level().getEntitiesOfClass(AbstractLionEntity.class, this.lion.getBoundingBox().inflate(HEARING_RANGE, 16.0D, HEARING_RANGE),
                other -> other != this.lion && other.isAlive())) {
            other.hearRoar(this.lion);
            if (other.isInPrideWith(this.lion) && !other.isBaby() && !other.isSleeping() && other.getHunt() == null && this.lion.distanceToSqr(other) < CHORUS_RANGE * CHORUS_RANGE) {
                other.requestRoar(10 + this.lion.getRandom().nextInt(30));
            }
        }
    }
}
