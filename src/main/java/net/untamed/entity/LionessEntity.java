package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.untamed.entity.ai.lion.LionHuntGoal;
import net.untamed.init.TagInit;

public class LionessEntity extends AbstractLionEntity {

    private static final ThreatProfile THREAT_PROFILE = new ThreatProfile(14.0D, 8.0D, 5.0D, 40, 1);
    private static final int MAX_STAMINA = 200;

    public LionessEntity(EntityType<? extends LionessEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isMale() {
        return false;
    }

    @Override
    public ThreatProfile getThreatProfile() {
        return THREAT_PROFILE;
    }

    @Override
    public int getMaxStamina() {
        return MAX_STAMINA;
    }

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(5, new LionHuntGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 26.0D).add(Attributes.FOLLOW_RANGE, 28.0D).add(Attributes.MOVEMENT_SPEED, 0.29D).add(Attributes.ATTACK_DAMAGE, 6.0D);
    }

    public static boolean checkLionessEntitySpawnRules(EntityType<LionessEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.LIONS_SPAWNABLE_ON);
    }
}
