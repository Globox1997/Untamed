package net.untamed.entity.ai.raccoon;

import net.minecraft.world.entity.ai.goal.Goal;
import net.untamed.entity.RaccoonEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class KitFollowMotherGoal extends Goal {

    private static final double START_DISTANCE = 3.5D;
    private static final double STOP_DISTANCE = 1.8D;
    private static final double RUN_DISTANCE = 10.0D;

    private final RaccoonEntity kit;
    @Nullable
    private RaccoonEntity leader;
    private int repathTicks;

    public KitFollowMotherGoal(RaccoonEntity kit) {
        this.kit = kit;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.kit.isBaby() || this.kit.isResting()) {
            return false;
        }
        this.leader = this.findLeader();
        return this.leader != null && this.kit.distanceToSqr(this.leader) > START_DISTANCE * START_DISTANCE;
    }

    @Nullable
    private RaccoonEntity findLeader() {
        RaccoonEntity mother = this.kit.getMother();
        if (mother == null) {
            return null;
        }
        List<RaccoonEntity> siblings = mother.getKits();
        int index = siblings.indexOf(this.kit);
        if (index > 0) {
            RaccoonEntity ahead = siblings.get(index - 1);
            if (ahead.isAlive() && ahead.distanceToSqr(mother) < RUN_DISTANCE * RUN_DISTANCE) {
                return ahead;
            }
        }
        return mother;
    }

    @Override
    public boolean canContinueToUse() {
        return this.kit.isBaby() && this.leader != null && this.leader.isAlive() && this.kit.distanceToSqr(this.leader) > STOP_DISTANCE * STOP_DISTANCE;
    }

    @Override
    public void start() {
        this.repathTicks = 0;
    }

    @Override
    public void tick() {
        if (this.leader != null && --this.repathTicks <= 0) {
            this.repathTicks = this.adjustedTickDelay(10);
            this.kit.getNavigation().moveTo(this.leader, this.kit.distanceToSqr(this.leader) > RUN_DISTANCE * RUN_DISTANCE ? 1.5D : 1.15D);
        }
    }

    @Override
    public void stop() {
        this.leader = null;
        this.kit.getNavigation().stop();
    }
}
