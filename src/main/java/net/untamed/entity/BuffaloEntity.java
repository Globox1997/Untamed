package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.ai.herd.HerdVoteGoal;
import net.untamed.entity.ai.herd.MobThreatGoal;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BuffaloEntity extends HerdBovineEntity {

    private static final ThreatProfile THREAT_PROFILE = new ThreatProfile(10.0D, 7.0D, 4.0D, 40);
    private static final int VOTE_VALID_TICKS = 400;
    private static final int TRAVEL_TICKS = 1200;
    private static final double TRAVEL_DISTANCE = 16.0D;
    private static final int SPONTANEOUS_VOTE_CHANCE = 2400;

    private float voteYaw;
    private int voteTicks;
    private boolean wantsToVote;
    @Nullable
    private Vec3 travelDirection;
    private int travelTicks;

    public BuffaloEntity(EntityType<? extends BuffaloEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.BUFFALO_FOOD);
    }

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(6, new HerdVoteGoal(this));
        this.targetSelector.addGoal(2, new MobThreatGoal(this, TagInit.BUFFALO_THREATS));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40.0D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MOVEMENT_SPEED, 0.21D).add(Attributes.ATTACK_DAMAGE, 5.0D).add(Attributes.ATTACK_KNOCKBACK, 0.3D);
    }

    public static boolean checkBuffaloEntitySpawnRules(EntityType<BuffaloEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.BUFFALOS_SPAWNABLE_ON);
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
    public boolean defendsAsGroup() {
        return true;
    }

    @Override
    public boolean prefersShade() {
        return true;
    }

    @Override
    public void onRestEnded() {
        if (!this.isBaby() && !this.isMale()) {
            this.wantsToVote = true;
        }
    }

    public boolean wantsToVote() {
        return this.wantsToVote;
    }

    public void castVote(float yaw) {
        this.voteYaw = yaw;
        this.voteTicks = VOTE_VALID_TICKS;
        this.wantsToVote = false;
    }

    public float getVoteYaw() {
        return this.voteYaw;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.voteTicks > 0) {
            this.voteTicks--;
        }
        if (this.travelTicks > 0 && --this.travelTicks == 0) {
            this.travelDirection = null;
        }
        if (this.travelDirection == null && !this.isBaby() && !this.isMale() && this.random.nextInt(SPONTANEOUS_VOTE_CHANCE) == 0) {
            this.wantsToVote = true;
        }
    }

    @Override
    protected void onHerdRefreshed() {
        if (this.travelDirection != null) {
            return;
        }
        int cows = 0;
        int votes = 0;
        double x = 0.0D;
        double z = 0.0D;
        for (HerdBovineEntity member : this.getHerdIncludingSelf()) {
            if (member instanceof BuffaloEntity buffalo && buffalo.isAlive() && !buffalo.isBaby() && !buffalo.isMale()) {
                cows++;
                if (buffalo.voteTicks > 0) {
                    votes++;
                    float yaw = buffalo.voteYaw * Mth.DEG_TO_RAD;
                    x -= Mth.sin(yaw);
                    z += Mth.cos(yaw);
                }
            }
        }
        if (votes == 0 || votes * 2 < cows) {
            return;
        }
        Vec3 sum = new Vec3(x, 0.0D, z);
        if (sum.length() < 0.3D * votes) {
            return;
        }
        this.travelDirection = sum.normalize();
        this.travelTicks = TRAVEL_TICKS;
    }

    private List<HerdBovineEntity> getHerdIncludingSelf() {
        List<HerdBovineEntity> all = new ArrayList<>(this.getHerd());
        all.add(this);
        return all;
    }

    @Nullable
    @Override
    public Vec3 getHerdTravelTarget() {
        if (this.travelDirection == null) {
            return null;
        }
        Vec3 center = this.getHerdCenter();
        return (center != null ? center : this.position()).add(this.travelDirection.scale(TRAVEL_DISTANCE));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.BUFFALO_IDLE_EVENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.BUFFALO_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.BUFFALO_DEATH_EVENT;
    }

    @Override
    protected SoundEvent getWarningSound() {
        return SoundInit.BUFFALO_WARNING_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.BUFFALO_STEP_EVENT, 0.15F, 1.0F);
    }
}
