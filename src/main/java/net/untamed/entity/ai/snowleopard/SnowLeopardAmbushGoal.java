package net.untamed.entity.ai.snowleopard;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.AbstractBigCatEntity;
import net.untamed.entity.SnowLeopardEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class SnowLeopardAmbushGoal extends Goal {

    private static final double SEARCH_RANGE = 20.0D;
    private static final int START_CHANCE = 60;
    private static final int SPOT_TRIES = 16;
    private static final int SPOT_MIN_DISTANCE = 4;
    private static final int SPOT_MAX_DISTANCE = 10;
    private static final int MIN_DROP = 2;
    private static final int MAX_DROP = 8;
    private static final double APPROACH_SPEED = 0.6D;
    private static final double CHASE_SPEED = 1.8D;
    private static final double AT_SPOT = 1.5D;
    private static final double LEAP_MAX_DISTANCE = 12.0D;
    private static final double LEAP_SPEED_PER_BLOCK = 0.11D;
    private static final double LEAP_MIN_SPEED = 0.5D;
    private static final double LEAP_MAX_SPEED = 1.3D;
    private static final double LEAP_VERTICAL = 0.3D;
    private static final double GIVE_UP_DISTANCE = 12.0D;
    private static final int MAX_WAIT_TICKS = 200;
    private static final int MAX_CHASE_TICKS = 40;
    private static final int MAX_TICKS = 900;
    private static final int ATTACK_COOLDOWN = 15;
    private static final int FAILED_HUNT_COOLDOWN = 1200;

    private enum Stage {
        APPROACH, WAIT, CHASE
    }

    private final SnowLeopardEntity leopard;
    @Nullable
    private LivingEntity prey;
    @Nullable
    private BlockPos spot;
    private Stage stage = Stage.APPROACH;
    private boolean failed;
    private int ticks;
    private int waitTicks;
    private int chaseTicks;
    private int attackCooldown;
    private int repathTicks;

    public SnowLeopardAmbushGoal(SnowLeopardEntity leopard) {
        this.leopard = leopard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.leopard.isBaby() || !this.leopard.isHungry() || this.leopard.isRestTime() || this.leopard.isAlarmed() || this.leopard.getTarget() != null
                || !this.leopard.canStartHunt() || this.leopard.getRandom().nextInt(reducedTickDelay(START_CHANCE)) != 0) {
            return false;
        }
        this.prey = null;
        for (Animal candidate : this.leopard.level().getEntitiesOfClass(Animal.class, this.leopard.getBoundingBox().inflate(SEARCH_RANGE, 10.0D, SEARCH_RANGE),
                this.leopard::isValidPrey)) {
            if (this.prey == null || this.leopard.distanceToSqr(candidate) < this.leopard.distanceToSqr(this.prey)) {
                this.prey = candidate;
            }
        }
        if (this.prey == null) {
            return false;
        }
        this.spot = this.findAmbushSpot(this.prey);
        return this.spot != null;
    }

    @Nullable
    private BlockPos findAmbushSpot(LivingEntity target) {
        Level level = this.leopard.level();
        BlockPos best = null;
        int bestDrop = 0;
        for (int i = 0; i < SPOT_TRIES; i++) {
            float angle = this.leopard.getRandom().nextFloat() * Mth.TWO_PI;
            int distance = SPOT_MIN_DISTANCE + this.leopard.getRandom().nextInt(SPOT_MAX_DISTANCE - SPOT_MIN_DISTANCE + 1);
            int x = Mth.floor(target.getX() + Mth.cos(angle) * distance);
            int z = Mth.floor(target.getZ() + Mth.sin(angle) * distance);
            BlockPos stand = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            int drop = stand.getY() - target.getBlockY();
            if (drop < MIN_DROP || drop > MAX_DROP || drop <= bestDrop || !level.isEmptyBlock(stand) || !level.isEmptyBlock(stand.above())
                    || !level.getFluidState(stand.below()).isEmpty()) {
                continue;
            }
            best = stand;
            bestDrop = drop;
        }
        if (best == null) {
            return null;
        }
        Path path = this.leopard.getNavigation().createPath(best, 1);
        return path != null && path.canReach() ? best : null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.prey != null && this.prey.isAlive() && !this.failed && this.ticks < MAX_TICKS && this.leopard.getTarget() == null && !this.leopard.isAlarmed();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.stage = Stage.APPROACH;
        this.failed = false;
        this.ticks = 0;
        this.waitTicks = 0;
        this.chaseTicks = 0;
        this.attackCooldown = 0;
        this.repathTicks = 0;
        this.leopard.setHunting(true);
        this.leopard.setPantherPose(AbstractBigCatEntity.PantherPose.STALKING);
        if (this.spot != null) {
            this.leopard.getNavigation().moveTo(this.spot.getX() + 0.5D, this.spot.getY(), this.spot.getZ() + 0.5D, APPROACH_SPEED);
        }
    }

    @Override
    public void tick() {
        if (this.prey == null || this.spot == null) {
            return;
        }
        this.ticks++;
        this.leopard.getLookControl().setLookAt(this.prey, 30.0F, 30.0F);
        switch (this.stage) {
            case APPROACH -> {
                if (this.leopard.position().distanceToSqr(Vec3.atBottomCenterOf(this.spot)) < AT_SPOT * AT_SPOT || this.leopard.getNavigation().isDone()) {
                    this.stage = Stage.WAIT;
                    this.leopard.getNavigation().stop();
                }
            }
            case WAIT -> {
                this.leopard.getNavigation().stop();
                if (this.canLeap()) {
                    this.leap();
                } else if (++this.waitTicks > MAX_WAIT_TICKS) {
                    this.failed = true;
                }
            }
            case CHASE -> this.tickChase();
        }
    }

    private boolean canLeap() {
        if (!this.leopard.onGround() || this.prey == null) {
            return false;
        }
        double dx = this.prey.getX() - this.leopard.getX();
        double dz = this.prey.getZ() - this.leopard.getZ();
        return dx * dx + dz * dz < LEAP_MAX_DISTANCE * LEAP_MAX_DISTANCE && this.prey.getY() < this.leopard.getY() - 1.0D
                && this.leopard.getSensing().hasLineOfSight(this.prey);
    }

    private void leap() {
        double dx = this.prey.getX() - this.leopard.getX();
        double dz = this.prey.getZ() - this.leopard.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > 1.0E-4D) {
            double speed = Mth.clamp(distance * LEAP_SPEED_PER_BLOCK, LEAP_MIN_SPEED, LEAP_MAX_SPEED);
            this.leopard.setDeltaMovement(dx / distance * speed, LEAP_VERTICAL, dz / distance * speed);
            this.leopard.hasImpulse = true;
        }
        this.leopard.startPounce();
        this.stage = Stage.CHASE;
        this.repathTicks = 10;
    }

    private void tickChase() {
        this.chaseTicks++;
        if (--this.repathTicks <= 0 && this.leopard.onGround()) {
            this.repathTicks = 5;
            this.leopard.getNavigation().moveTo(this.prey, CHASE_SPEED);
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        } else if (this.leopard.isWithinMeleeAttackRange(this.prey)) {
            this.leopard.doHurtTarget(this.prey);
            this.attackCooldown = ATTACK_COOLDOWN;
        }
        double distance = this.leopard.distanceTo(this.prey);
        if (distance > GIVE_UP_DISTANCE || (this.chaseTicks > MAX_CHASE_TICKS && distance > 3.0D)) {
            this.failed = true;
        }
    }

    @Override
    public void stop() {
        this.leopard.clearPantherPose(AbstractBigCatEntity.PantherPose.STALKING);
        this.leopard.setHunting(false);
        this.leopard.getNavigation().stop();
        if (this.failed || this.ticks >= MAX_TICKS) {
            this.leopard.setHuntCooldown(FAILED_HUNT_COOLDOWN);
        }
        this.prey = null;
        this.spot = null;
    }
}
