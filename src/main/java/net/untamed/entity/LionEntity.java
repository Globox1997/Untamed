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
import net.untamed.entity.ai.lion.DriveOffIntruderGoal;
import net.untamed.entity.ai.lion.PatrolTerritoryGoal;
import net.untamed.init.TagInit;

public class LionEntity extends AbstractLionEntity {

    private static final ThreatProfile THREAT_PROFILE = new ThreatProfile(16.0D, 10.0D, 6.0D, 40, 2);
    private static final int MAX_STAMINA = 60;

    public LionEntity(EntityType<? extends LionEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isMale() {
        return true;
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
        this.goalSelector.addGoal(5, new DriveOffIntruderGoal(this));
        this.goalSelector.addGoal(8, new PatrolTerritoryGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 34.0D).add(Attributes.FOLLOW_RANGE, 24.0D).add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.ATTACK_DAMAGE, 9.0D);
    }

    public static boolean checkLionEntitySpawnRules(EntityType<LionEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.LIONS_SPAWNABLE_ON);
    }
}
