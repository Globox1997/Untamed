package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.ai.herd.BisonChargeGoal;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class RhinoEntity extends HerdBovineEntity {

    private static final ThreatProfile THREAT_PROFILE = new ThreatProfile(20.0D, 10.0D, 5.0D, 40);
    private static final double SIGHT_RANGE = 6.0D;
    private static final double SNEAK_SIGHT_RANGE = 3.0D;
    private static final double HEAR_SPRINT_RANGE = 16.0D;
    private static final double HEAR_WALK_RANGE = 8.0D;
    private static final double SMELL_RANGE = 20.0D;
    private static final double UPWIND_DOT = 0.7D;
    private static final double SHARE_NOTICE_RANGE = 16.0D;
    private static final int NOTICE_TICKS = 200;
    private static final int FRESH_NOTICE_TICKS = 10;
    private static final double STARTLE_DISTANCE = 5.0D;
    private static final double MOTHER_STARTLE_DISTANCE = 8.0D;
    private static final double CALF_RANGE = 8.0D;

    @Nullable
    private UUID noticedPlayer;
    private long noticedUntil;
    private long freshNoticeUntil;

    public RhinoEntity(EntityType<? extends RhinoEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.RHINO_FOOD);
    }

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(1, new BisonChargeGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 60.0D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D).add(Attributes.ATTACK_KNOCKBACK, 1.0D).add(Attributes.ARMOR, 4.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.75D);
    }

    public static boolean checkRhinoEntitySpawnRules(EntityType<RhinoEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.RHINOS_SPAWNABLE_ON);
    }

    @Override
    public ThreatProfile getThreatProfile() {
        return THREAT_PROFILE;
    }

    @Override
    public boolean isRestTime() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 4000L && time <= 9000L;
    }

    @Override
    public boolean prefersShade() {
        return true;
    }

    @Override
    public boolean defendsAsGroup() {
        return true;
    }

    @Override
    public boolean followsHerd() {
        return !this.isRoamer() || this.isBaby();
    }

    @Override
    public boolean calfLeadsMother() {
        return true;
    }

    @Override
    public double scoreRestSpot(BlockPos pos) {
        Level level = this.level();
        if (level.getBlockState(pos.below()).is(TagInit.RHINO_WALLOW_BLOCKS)) {
            return 20.0D;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER) || level.getFluidState(pos.below().relative(direction)).is(FluidTags.WATER)) {
                return 20.0D;
            }
        }
        return super.scoreRestSpot(pos);
    }

    @Override
    public void grazeAt(BlockPos pos) {
        Level level = this.level();
        BlockState state = level.getBlockState(pos);
        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && state.is(Blocks.TALL_GRASS) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 3);
            return;
        }
        super.grazeAt(pos);
    }

    @Override
    public boolean canDetect(Player player) {
        long time = this.level().getGameTime();
        if (player.getUUID().equals(this.noticedPlayer) && time < this.noticedUntil) {
            return true;
        }
        double distance = this.distanceTo(player);
        boolean detected = distance < (player.isDiscrete() ? SNEAK_SIGHT_RANGE : SIGHT_RANGE);
        if (!detected && !player.isDiscrete()) {
            double moved = Mth.square(player.getX() - player.xo) + Mth.square(player.getZ() - player.zo);
            detected = (player.isSprinting() && distance < HEAR_SPRINT_RANGE) || (moved > 0.0025D && distance < HEAR_WALK_RANGE);
        }
        if (!detected && distance < SMELL_RANGE) {
            Vec3 toRhino = new Vec3(this.getX() - player.getX(), 0.0D, this.getZ() - player.getZ());
            detected = toRhino.lengthSqr() > 1.0E-4D && toRhino.normalize().dot(this.getWindDirection()) > UPWIND_DOT;
        }
        if (detected) {
            this.notice(player);
            for (HerdBovineEntity member : this.getHerd()) {
                if (member instanceof RhinoEntity rhino && rhino.isAlive() && this.distanceToSqr(rhino) < SHARE_NOTICE_RANGE * SHARE_NOTICE_RANGE) {
                    rhino.notice(player);
                }
            }
        }
        return detected;
    }

    private void notice(Player player) {
        long time = this.level().getGameTime();
        if (!player.getUUID().equals(this.noticedPlayer) || time >= this.noticedUntil) {
            this.freshNoticeUntil = time + FRESH_NOTICE_TICKS;
        }
        this.noticedPlayer = player.getUUID();
        this.noticedUntil = time + NOTICE_TICKS;
    }

    private Vec3 getWindDirection() {
        long day = this.level().getDayTime() / 24000L;
        float yaw = (Mth.murmurHash3Mixer((int) day) & 0xFFFF) / 65536.0F * Mth.TWO_PI;
        return new Vec3(Mth.cos(yaw), 0.0D, Mth.sin(yaw));
    }

    @Override
    public void onThreatNoticed(Player player) {
        if (this.isBaby() || this.level().getGameTime() > this.freshNoticeUntil) {
            return;
        }
        double startle = this.hasCalfNearby(CALF_RANGE) ? MOTHER_STARTLE_DISTANCE : STARTLE_DISTANCE;
        if (this.distanceTo(player) < startle) {
            this.playWarningSound();
            this.provokeBy(player);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.RHINO_IDLE_EVENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.RHINO_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.RHINO_DEATH_EVENT;
    }

    @Override
    protected SoundEvent getWarningSound() {
        return SoundInit.RHINO_ATTACK_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.RHINO_STEP_EVENT, 0.15F, 1.0F);
    }
}
