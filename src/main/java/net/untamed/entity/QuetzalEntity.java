package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.untamed.entity.ai.quetzal.HoverPluckGoal;
import net.untamed.entity.ai.quetzal.QuetzalDisplayFlightGoal;
import net.untamed.init.EntityInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

public class QuetzalEntity extends AbstractBirdEntity {

    private static final EntityDataAccessor<Boolean> DATA_PLUMES = SynchedEntityData.defineId(QuetzalEntity.class, EntityDataSerializers.BOOLEAN);
    private static final float PLUMES_CHANCE = 0.5F;
    private static final int MIN_SEED_TICKS = 600;
    private static final int SEED_TICKS_VARIATION = 600;
    private static final float SEED_DROP_CHANCE = 0.3F;
    private static final float SAPLING_CHANCE = 0.05F;

    private ItemStack carriedSeed = ItemStack.EMPTY;
    private int seedTicks;

    public QuetzalEntity(EntityType<? extends QuetzalEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6.0D).add(Attributes.FLYING_SPEED, 0.4D).add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    public static boolean checkQuetzalEntitySpawnRules(EntityType<QuetzalEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        return !levelAccessor.getBiome(blockPos).is(BiomeTags.IS_JUNGLE) ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.QUETZALS_SPAWNABLE_ON);
    }

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(5, new HoverPluckGoal(this));
        this.goalSelector.addGoal(5, new QuetzalDisplayFlightGoal(this));
    }

    @Override
    protected TagKey<Item> getFoodTag() {
        return TagInit.QUETZAL_FOOD;
    }

    @Override
    public int getMinPerchTicks() {
        return 200;
    }

    @Override
    public int getPerchTicksVariation() {
        return 300;
    }

    @Override
    public boolean freezesWhenThreatened() {
        return true;
    }

    @Override
    protected float getSpeciesPitch() {
        return 0.9F;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return EntityInit.QUETZAL.create(serverLevel);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLUMES, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData result = super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
        this.setPlumes(!this.isBaby() && this.random.nextFloat() < PLUMES_CHANCE);
        return result;
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.isBaby() && !this.level().isClientSide()) {
            this.setPlumes(this.random.nextFloat() < PLUMES_CHANCE);
        }
    }

    public boolean hasPlumes() {
        return this.entityData.get(DATA_PLUMES);
    }

    private void setPlumes(boolean plumes) {
        this.entityData.set(DATA_PLUMES, plumes);
    }

    public void carrySeed(ItemStack seed) {
        this.carriedSeed = seed;
        this.seedTicks = MIN_SEED_TICKS + this.random.nextInt(SEED_TICKS_VARIATION);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.seedTicks > 0 && --this.seedTicks == 0 && !this.carriedSeed.isEmpty()) {
            float roll = this.random.nextFloat();
            ItemStack drop = roll < SAPLING_CHANCE ? new ItemStack(Items.JUNGLE_SAPLING) : roll < SEED_DROP_CHANCE ? this.carriedSeed : ItemStack.EMPTY;
            if (!drop.isEmpty()) {
                ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), drop);
                item.setDefaultPickUpDelay();
                this.level().addFreshEntity(item);
            }
            this.carriedSeed = ItemStack.EMPTY;
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.setPlumes(compoundTag.getBoolean("Plumes"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putBoolean("Plumes", this.hasPlumes());
    }
}
