package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.untamed.entity.ai.panther.PantherFishGoal;
import net.untamed.init.ConfigInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;

public class BlackPantherEntity extends AbstractBigCatEntity {

    public BlackPantherEntity(EntityType<? extends BlackPantherEntity> entityType, Level level) {
        super(entityType, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
    }

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(5, new PantherFishGoal(this));
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.BLACK_PANTHER_FOOD);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 28.0D).add(Attributes.FOLLOW_RANGE, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.28D).add(Attributes.ATTACK_DAMAGE, 6.0D);
    }

    public static boolean checkBlackPantherEntitySpawnRules(EntityType<BlackPantherEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_JUNGLE)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.BLACK_PANTHERS_SPAWNABLE_ON);
    }

    @Override
    public boolean isRestTime() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 1000L && time <= 11500L;
    }

    @Override
    public double scoreCoverSpot(BlockPos pos) {
        Level level = this.level();
        double score = level.canSeeSky(pos.above()) ? -10.0D : 10.0D;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState side = level.getBlockState(pos.relative(direction));
            if (side.is(BlockTags.LOGS) || side.is(BlockTags.LEAVES)) {
                score += 3.0D;
            }
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                score += 3.0D;
            }
        }
        return score;
    }

    @Override
    public boolean isPreyType(LivingEntity entity) {
        return entity.getType().is(TagInit.BLACK_PANTHER_PREY) || entity.getType().is(TagInit.BLACK_PANTHER_FISH)
                || (ConfigInit.CONFIG.panthersHuntLivestock && entity.getType().is(TagInit.BLACK_PANTHER_LIVESTOCK_PREY));
    }

    @Override
    protected int getPreyFeedValue(LivingEntity prey) {
        return prey.getType().is(TagInit.BLACK_PANTHER_FISH) ? HUNGRY / 3 : HUNGRY;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.BLACK_PANTHER_IDLE_EVENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.BLACK_PANTHER_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.BLACK_PANTHER_DEATH_EVENT;
    }

    @Override
    protected SoundEvent getWarningSound() {
        return SoundInit.BLACK_PANTHER_ATTACK_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.BLACK_PANTHER_STEP_EVENT, 0.15F, 1.0F);
    }
}
