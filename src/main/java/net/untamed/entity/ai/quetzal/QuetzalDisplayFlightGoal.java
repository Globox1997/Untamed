package net.untamed.entity.ai.quetzal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractBirdEntity;
import net.untamed.entity.QuetzalEntity;
import net.untamed.entity.ai.LandUtils;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class QuetzalDisplayFlightGoal extends Goal {

    private static final int LOVE_CHANCE = 40;
    private static final int MORNING_CHANCE = 1200;
    private static final long MORNING_END = 3000L;
    private static final double CLIMB_HEIGHT = 10.0D;
    private static final double SPIRAL_RADIUS = 4.0D;
    private static final float SPIRAL_STEP = 0.12F;
    private static final double CLIMB_PER_TICK = 0.12D;
    private static final double CLIMB_SPEED = 1.2D;
    private static final double DIVE_SPEED = 1.6D;
    private static final int CALL_INTERVAL = 30;
    private static final int MAX_TICKS = 400;

    private final QuetzalEntity quetzal;
    private Vec3 center = Vec3.ZERO;
    private double startY;
    private float angle;
    private boolean diving;
    private int ticks;
    @Nullable
    private BlockPos landing;

    public QuetzalDisplayFlightGoal(QuetzalEntity quetzal) {
        this.quetzal = quetzal;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.quetzal.isBaby() || !this.quetzal.hasPlumes() || this.quetzal.isAlarmed() || this.quetzal.isBusy() || this.quetzal.isRoostTime()
                || !this.quetzal.level().canSeeSky(this.quetzal.blockPosition().above())) {
            return false;
        }
        boolean morning = this.quetzal.level().getDayTime() % 24000L < MORNING_END;
        int chance = this.quetzal.isInLove() ? LOVE_CHANCE : morning ? MORNING_CHANCE : 0;
        return chance > 0 && this.quetzal.getRandom().nextInt(reducedTickDelay(chance)) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.ticks < MAX_TICKS && !this.quetzal.isAlarmed() && !(this.diving && this.quetzal.getNavigation().isDone());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.center = this.quetzal.position();
        this.startY = this.quetzal.getY();
        this.angle = this.quetzal.getRandom().nextFloat() * Mth.TWO_PI;
        this.diving = false;
        this.ticks = 0;
        this.landing = null;
        this.quetzal.clearBirdPose(AbstractBirdEntity.BirdPose.PERCHED);
        this.quetzal.playCall(0.3F);
    }

    @Override
    public void tick() {
        this.ticks++;
        if (this.ticks % CALL_INTERVAL == 0) {
            this.quetzal.playCall(0.3F);
        }
        if (this.diving) {
            return;
        }
        this.angle += SPIRAL_STEP;
        double y = Math.min(this.startY + CLIMB_HEIGHT, this.startY + this.ticks * CLIMB_PER_TICK);
        this.quetzal.getMoveControl().setWantedPosition(this.center.x + Mth.cos(this.angle) * SPIRAL_RADIUS, y, this.center.z + Mth.sin(this.angle) * SPIRAL_RADIUS, CLIMB_SPEED);
        if (this.quetzal.getY() >= this.startY + CLIMB_HEIGHT - 1.0D) {
            this.startDive();
        }
    }

    private void startDive() {
        this.diving = true;
        BlockPos origin = BlockPos.containing(this.center);
        if (this.quetzal.level() instanceof ServerLevel level) {
            this.landing = LandUtils.findAerialPerch(level, this.quetzal.getRandom(), origin, 8, 16);
        }
        BlockPos target = this.landing != null ? this.landing : origin;
        this.quetzal.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, DIVE_SPEED);
    }

    @Override
    public void stop() {
        this.quetzal.getNavigation().stop();
    }
}
