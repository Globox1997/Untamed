package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.untamed.init.EntityInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class OctopusEntity extends Animal {

    private static final int MAX_TIME_OUT_OF_WATER = 300;

    private static final EntityDataAccessor<Boolean> DATA_CRAWLING =
            SynchedEntityData.defineId(OctopusEntity.class, EntityDataSerializers.BOOLEAN);

    public OctopusEntity(EntityType<? extends OctopusEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return EntityInit.OCTOPUS.create(serverLevel);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.OCTOPUS_FOOD);
    }

    @Override
    public boolean canMate(Animal animal) {
        if (!(animal instanceof OctopusEntity octopusEntity)) {
            return false;
        }
        return this.isInLove() && octopusEntity.isInLove();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.5));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, Ingredient.of(TagInit.OCTOPUS_FOOD), false));
        this.goalSelector.addGoal(3, new GoToWaterGoal(this, 1.2, 24));
        this.goalSelector.addGoal(4, new CrawlOnFloorGoal(this, 0.8, 8, 3));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(6, new OctopusRandomSwimmingGoal(this, 1.0, 10));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 0.0);
    }

    public static boolean checkOctopusEntitySpawnRules(EntityType<OctopusEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_OCEAN) ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.OCTOPUSES_SPAWNABLE_ON);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }

    public boolean isCrawling() {
        return this.entityData.get(DATA_CRAWLING);
    }

    public void setCrawling(boolean crawling) {
        this.entityData.set(DATA_CRAWLING, crawling);
    }

    @Override
    public void baseTick() {
        int i = this.getAirSupply();
        super.baseTick();
        this.handleOutOfWaterSurvivalTime(i);
    }

    protected void handleOutOfWaterSurvivalTime(int previousValue) {
        if (this.isAlive() && !this.isInWaterOrBubble()) {
            this.setAirSupply(previousValue - 1);
            if (this.getAirSupply() == -20) {
                this.setAirSupply(0);
                this.hurt(this.damageSources().dryOut(), 2.0F);
            }
        } else {
            this.setAirSupply(this.getMaxAirSupply());
        }
    }

    @Override
    public int getMaxAirSupply() {
        return MAX_TIME_OUT_OF_WATER;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        boolean wasHurt = super.hurt(damageSource, amount);
        if (wasHurt && this.isAlive() && this.isInWater() && this.random.nextFloat() < 0.6F) {
            this.releaseInkCloud(damageSource);
        }
        return wasHurt;
    }

    private void releaseInkCloud(DamageSource damageSource) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                    this.getX(), this.getY(0.5), this.getZ(),
                    14, 0.3, 0.3, 0.3, 0.05);
        }
        this.playSound(SoundEvents.AXOLOTL_SPLASH, 1.0F, 1.0F);
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));

        Entity attacker = damageSource.getEntity();
        if (attacker instanceof LivingEntity livingEntity) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        }
    }

    public static class GoToWaterGoal extends MoveToBlockGoal {
        private final OctopusEntity octopus;

        public GoToWaterGoal(OctopusEntity octopus, double speedModifier, int searchRange) {
            super(octopus, speedModifier, searchRange, 4);
            this.octopus = octopus;
        }

        @Override
        public boolean canUse() {
            return !this.octopus.isInWater() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !this.octopus.isInWater() && super.canContinueToUse();
        }

        @Override
        protected boolean isValidTarget(LevelReader levelReader, BlockPos blockPos) {
            return levelReader.getBlockState(blockPos).is(Blocks.WATER) && levelReader.getBlockState(blockPos.above()).isAir();
        }

        @Override
        public double acceptedDistance() {
            return 1.5;
        }
    }

    public static class CrawlOnFloorGoal extends Goal {
        private final OctopusEntity octopus;
        private final double speedModifier;
        private final int searchRadiusHorizontal;
        private final int searchRadiusVertical;
        private double wantedX;
        private double wantedY;
        private double wantedZ;

        public CrawlOnFloorGoal(OctopusEntity octopus, double speedModifier, int searchRadiusHorizontal, int searchRadiusVertical) {
            this.octopus = octopus;
            this.speedModifier = speedModifier;
            this.searchRadiusHorizontal = searchRadiusHorizontal;
            this.searchRadiusVertical = searchRadiusVertical;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.octopus.isInWater()) {
                return false;
            }
            if (this.octopus.getRandom().nextInt(30) != 0) {
                return false;
            }
            return this.findFloorTarget();
        }

        @Override
        public boolean canContinueToUse() {
            return this.octopus.isInWater() && !this.octopus.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.octopus.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.speedModifier);
            this.octopus.setCrawling(true);
        }

        @Override
        public void stop() {
            this.octopus.getNavigation().stop();
            this.octopus.setCrawling(false);
        }

        private boolean findFloorTarget() {
            Level level = this.octopus.level();
            RandomSource random = this.octopus.getRandom();
            BlockPos origin = this.octopus.blockPosition();

            for (int attempt = 0; attempt < 10; attempt++) {
                int dx = random.nextInt(this.searchRadiusHorizontal * 2 + 1) - this.searchRadiusHorizontal;
                int dz = random.nextInt(this.searchRadiusHorizontal * 2 + 1) - this.searchRadiusHorizontal;
                BlockPos column = origin.offset(dx, 0, dz);

                for (int dy = -this.searchRadiusVertical; dy <= this.searchRadiusVertical; dy++) {
                    BlockPos candidate = column.offset(0, dy, 0);
                    boolean candidateIsWater = level.getFluidState(candidate).is(FluidTags.WATER);
                    if (!candidateIsWater) {
                        continue;
                    }
                    BlockPos below = candidate.below();
//                    boolean groundIsSuitable = !level.getFluidState(below).is(FluidTags.WATER) && level.getBlockState(below).is(TagInit.OCTOPUSES_SPAWNABLE_ON);
                    boolean groundIsSuitable = !level.getFluidState(below).is(FluidTags.WATER) && level.getBlockState(below).isCollisionShapeFullBlock(level, below);
                    if (groundIsSuitable) {
                        this.wantedX = candidate.getX() + 0.5;
                        this.wantedY = candidate.getY();
                        this.wantedZ = candidate.getZ() + 0.5;
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static class OctopusRandomSwimmingGoal extends RandomSwimmingGoal {
        private final OctopusEntity octopus;

        public OctopusRandomSwimmingGoal(OctopusEntity octopus, double speedModifier, int interval) {
            super(octopus, speedModifier, interval);
            this.octopus = octopus;
        }

        @Override
        public boolean canUse() {
            return this.octopus.getRandom().nextInt(4) == 0 && super.canUse();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AXOLOTL_IDLE_WATER;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CRAWLING, false);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            spawnGroupData = new AgeableMobGroupData(1.0F);
        }
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
    }
}