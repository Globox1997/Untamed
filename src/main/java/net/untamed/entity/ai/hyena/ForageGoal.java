package net.untamed.entity.ai.hyena;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.HyenaEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

// At night clan members split up and roam far from the den alone or in twos and threes, stopping to sniff around
public class ForageGoal extends Goal {

    private static final double MIN_RADIUS = 16.0D;
    private static final double RADIUS_VARIATION = 32.0D;
    private static final double JOIN_RANGE = 12.0D;
    private static final double ARRIVE_DISTANCE = 4.0D;
    private static final int MAX_FORAGE_TICKS = 1200;
    private static final int SNIFF_TICKS = 30;

    private final HyenaEntity hyena;
    @Nullable
    private Vec3 target;
    private int ticks;
    private int nextSniff;
    private int sniffTicks;
    private int repathTicks;

    public ForageGoal(HyenaEntity hyena) {
        this.hyena = hyena;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.hyena.isBaby() || this.hyena.isRestTime() || this.hyena.isDawn() || !this.isFree()
                || this.hyena.getRandom().nextInt(reducedTickDelay(100)) != 0) {
            return false;
        }
        this.target = this.pickTarget();
        return this.target != null;
    }

    @Nullable
    private Vec3 pickTarget() {
        for (HyenaEntity member : this.hyena.getClan()) {
            if (member.isAlive() && member.getForageTarget() != null && this.hyena.distanceToSqr(member) < JOIN_RANGE * JOIN_RANGE && this.hyena.getRandom().nextBoolean()) {
                return member.getForageTarget();
            }
        }
        BlockPos den = this.hyena.getDenPos();
        Vec3 center = den != null ? Vec3.atBottomCenterOf(den) : this.hyena.position();
        float angle = this.hyena.getRandom().nextFloat() * Mth.TWO_PI;
        double radius = MIN_RADIUS + this.hyena.getRandom().nextDouble() * RADIUS_VARIATION;
        Vec3 point = center.add(Mth.cos(angle) * radius, 0.0D, Mth.sin(angle) * radius);
        return LandRandomPos.getPosTowards(this.hyena, 16, 7, point);
    }

    private boolean isFree() {
        return !this.hyena.isAlarmed() && this.hyena.getTarget() == null && this.hyena.getHunt() == null && this.hyena.getRallyPos() == null && !this.hyena.isResting();
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null && this.ticks > 0 && this.isFree() && !this.hyena.isRestTime()
                && this.hyena.position().distanceToSqr(this.target) > ARRIVE_DISTANCE * ARRIVE_DISTANCE;
    }

    @Override
    public void start() {
        this.ticks = this.adjustedTickDelay(MAX_FORAGE_TICKS);
        this.nextSniff = this.adjustedTickDelay(60 + this.hyena.getRandom().nextInt(60));
        this.sniffTicks = 0;
        this.repathTicks = 0;
        this.hyena.setForageTarget(this.target);
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.ticks--;
        if (this.sniffTicks > 0) {
            if (--this.sniffTicks == 0) {
                this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.SNIFFING);
                this.repathTicks = 0;
            }
            return;
        }
        if (--this.nextSniff <= 0) {
            this.nextSniff = this.adjustedTickDelay(60 + this.hyena.getRandom().nextInt(60));
            this.sniffTicks = this.adjustedTickDelay(SNIFF_TICKS);
            this.hyena.getNavigation().stop();
            this.hyena.setHyenaPose(HyenaEntity.HyenaPose.SNIFFING);
            return;
        }
        if (--this.repathTicks <= 0 || this.hyena.getNavigation().isDone()) {
            this.repathTicks = this.adjustedTickDelay(40);
            this.hyena.getNavigation().moveTo(this.target.x, this.target.y, this.target.z, 0.9D);
        }
    }

    @Override
    public void stop() {
        this.hyena.clearHyenaPose(HyenaEntity.HyenaPose.SNIFFING);
        this.hyena.setForageTarget(null);
        this.hyena.getNavigation().stop();
        this.target = null;
    }
}
