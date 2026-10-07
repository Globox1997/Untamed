package net.untamed.entity;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.untamed.entity.ai.herd.BisonChargeGoal;
import net.untamed.entity.ai.herd.FollowLeaderGoal;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

public class BisonEntity extends HerdBovineEntity {

    private static final ThreatProfile THREAT_PROFILE = new ThreatProfile(12.0D, 8.0D, 5.0D, 60);

    @Nullable
    private HerdBovineEntity leader;

    public BisonEntity(EntityType<? extends BisonEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.BISON_FOOD);
    }

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(1, new BisonChargeGoal(this));
        this.goalSelector.addGoal(6, new FollowLeaderGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 36.0D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MOVEMENT_SPEED, 0.22D).add(Attributes.ATTACK_DAMAGE, 4.0D).add(Attributes.ATTACK_KNOCKBACK, 0.5D);
    }

    public static boolean checkBisonEntitySpawnRules(EntityType<BisonEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(ConventionalBiomeTags.IS_PLAINS)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.BISONS_SPAWNABLE_ON);
    }

    @Override
    public ThreatProfile getThreatProfile() {
        return THREAT_PROFILE;
    }

    @Override
    public boolean isRestTime() {
        return this.level().isNight();
    }

    @Override
    protected void onHerdRefreshed() {
        HerdBovineEntity best = this.isBaby() ? null : this;
        for (HerdBovineEntity member : this.getHerd()) {
            if (member.isAlive() && !member.isBaby() && (best == null || isBetterLeader(member, best))) {
                best = member;
            }
        }
        this.leader = best;
    }

    private static boolean isBetterLeader(HerdBovineEntity candidate, HerdBovineEntity current) {
        if (candidate.isRoamer() != current.isRoamer()) {
            return !candidate.isRoamer();
        }
        if (candidate.getSeniority() != current.getSeniority()) {
            return candidate.getSeniority() > current.getSeniority();
        }
        return candidate.getUUID().compareTo(current.getUUID()) > 0;
    }

    @Nullable
    @Override
    public HerdBovineEntity getHerdLeader() {
        return this.leader != null && this.leader.isAlive() ? this.leader : null;
    }

    @Override
    public void grazeAt(BlockPos pos) {
        super.grazeAt(pos);
        if (this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && this.level().getBlockState(pos).is(Blocks.SNOW)) {
            this.level().destroyBlock(pos, false);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.BISON_IDLE_EVENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.BISON_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.BISON_DEATH_EVENT;
    }

    @Override
    protected SoundEvent getWarningSound() {
        return SoundInit.BISON_WARNING_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.BISON_STEP_EVENT, 0.15F, 1.0F);
    }
}
