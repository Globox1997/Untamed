package net.untamed.entity.ai.bear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.BlackBearEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class RaidBeehiveGoal extends Goal {

    private static final int SEARCH_RANGE = 16;
    private static final int MAX_HIVE_HEIGHT_ABOVE_FEET = 2;
    private static final double REACH_DISTANCE = 1.5D;
    private static final int RAID_TICKS = 60;
    private static final int EAT_TICKS = 40;
    private static final int MAX_TICKS = 600;
    private static final int HONEY_FOOD = 10000;
    private static final double BEE_ANGER_RANGE = 12.0D;

    private final BlackBearEntity bear;
    @Nullable
    private BlockPos hive;
    @Nullable
    private BlockPos standPos;
    private boolean raiding;
    private boolean eating;
    private int raidTicks;
    private int ticks;
    private int repathTicks;

    public RaidBeehiveGoal(BlackBearEntity bear) {
        this.bear = bear;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.bear.isBaby() || !this.bear.isPeckish() || this.bear.isAlarmed() || this.bear.getTarget() != null || this.bear.isResting()
                || !(this.bear.level() instanceof ServerLevel serverLevel) || !serverLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || this.bear.getRandom().nextInt(reducedTickDelay(100)) != 0) {
            return false;
        }
        List<BlockPos> hives = serverLevel.getPoiManager().findAll(holder -> holder.is(PoiTypeTags.BEE_HOME), this::isFullHive, this.bear.blockPosition(), SEARCH_RANGE, PoiManager.Occupancy.ANY)
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(this.bear.blockPosition())))
                .toList();
        for (BlockPos candidate : hives) {
            BlockPos stand = this.findStandPos(candidate);
            if (stand == null) {
                continue;
            }
            Path path = this.bear.getNavigation().createPath(stand, 0);
            if (path != null && path.canReach()) {
                this.hive = candidate;
                this.standPos = stand;
                return true;
            }
        }
        return false;
    }

    private boolean isFullHive(BlockPos pos) {
        BlockState state = this.bear.level().getBlockState(pos);
        return state.getBlock() instanceof BeehiveBlock && state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS;
    }

    @Nullable
    private BlockPos findStandPos(BlockPos hivePos) {
        Level level = this.bear.level();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos column = hivePos.relative(direction);
            for (int dy = 0; dy <= MAX_HIVE_HEIGHT_ABOVE_FEET; dy++) {
                BlockPos feet = column.below(dy);
                if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                        && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
                    return feet;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.hive != null && this.ticks < MAX_TICKS && this.bear.getTarget() == null && !this.bear.isAlarmed()
                && (this.eating || this.isFullHive(this.hive));
    }

    @Override
    public void start() {
        this.raiding = false;
        this.eating = false;
        this.raidTicks = 0;
        this.ticks = 0;
        this.repathTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.hive == null || this.standPos == null) {
            return;
        }
        this.ticks++;
        Vec3 hiveCenter = Vec3.atCenterOf(this.hive);
        if (this.eating) {
            this.bear.getNavigation().stop();
            if (++this.raidTicks >= EAT_TICKS) {
                this.hive = null;
            }
            return;
        }
        if (!this.raiding) {
            Vec3 stand = Vec3.atBottomCenterOf(this.standPos);
            if (this.bear.position().distanceToSqr(stand) < REACH_DISTANCE * REACH_DISTANCE) {
                this.raiding = true;
                this.raidTicks = 0;
                this.bear.getNavigation().stop();
            } else if (--this.repathTicks <= 0 || this.bear.getNavigation().isDone()) {
                this.repathTicks = 20;
                this.bear.getNavigation().moveTo(stand.x, stand.y, stand.z, 1.0D);
            }
            return;
        }
        this.bear.getNavigation().stop();
        this.bear.getLookControl().setLookAt(hiveCenter.x, hiveCenter.y, hiveCenter.z);
        this.bear.setBearPose(BlackBearEntity.BearPose.STANDING);
        if (this.raidTicks % 15 == 0) {
            this.bear.playSound(SoundEvents.BEEHIVE_SHEAR, 0.8F, 0.8F);
        }
        if (++this.raidTicks >= RAID_TICKS) {
            this.takeHoney();
        }
    }

    private void takeHoney() {
        Level level = this.bear.level();
        BlockState state = level.getBlockState(this.hive);
        if (!(state.getBlock() instanceof BeehiveBlock beehive) || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            this.hive = null;
            return;
        }
        beehive.resetHoneyLevel(level, state, this.hive);
        if (level.getBlockEntity(this.hive) instanceof BeehiveBlockEntity hiveEntity) {
            hiveEntity.emptyAllLivingFromHive(null, level.getBlockState(this.hive), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
        }
        for (Bee bee : level.getEntitiesOfClass(Bee.class, this.bear.getBoundingBox().inflate(BEE_ANGER_RANGE))) {
            bee.setTarget(this.bear);
            bee.setPersistentAngerTarget(this.bear.getUUID());
            bee.startPersistentAngerTimer();
        }
        this.bear.feed(HONEY_FOOD);
        this.bear.playSound(SoundEvents.HONEY_DRINK, 1.0F, 0.8F);
        this.bear.setBearPose(BlackBearEntity.BearPose.EATING);
        this.eating = true;
        this.raidTicks = 0;
    }

    @Override
    public void stop() {
        this.bear.clearBearPose(BlackBearEntity.BearPose.STANDING);
        this.bear.clearBearPose(BlackBearEntity.BearPose.EATING);
        this.bear.getNavigation().stop();
        this.hive = null;
        this.standPos = null;
    }
}
