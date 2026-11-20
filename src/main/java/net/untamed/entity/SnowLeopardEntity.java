package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
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
import net.untamed.entity.ai.snowleopard.SnowLeopardAmbushGoal;
import net.untamed.init.ConfigInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;

public class SnowLeopardEntity extends AbstractBigCatEntity {

    private static final double NOTICE_DISTANCE_SCALE = 1.4D;
    private static final double GREET_RANGE = 4.0D;
    private static final int GREET_CHECK_INTERVAL = 40;
    private static final int GREET_COOLDOWN = 1200;
    private static final int YOWL_CHANCE = 300;
    private static final int MAX_DEN_ELEVATION_BONUS = 4;

    private long lastGreetTime = -GREET_COOLDOWN;

    public SnowLeopardEntity(EntityType<? extends SnowLeopardEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(5, new SnowLeopardAmbushGoal(this));
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.SNOW_LEOPARD_FOOD);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 22.0D).add(Attributes.FOLLOW_RANGE, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D).add(Attributes.SAFE_FALL_DISTANCE, 8.0D).add(Attributes.STEP_HEIGHT, 1.0D).add(Attributes.JUMP_STRENGTH, 0.5D);
    }

    public static boolean checkSnowLeopardEntitySpawnRules(EntityType<SnowLeopardEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(TagInit.SNOW_LEOPARD_HABITAT)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.SNOW_LEOPARDS_SPAWNABLE_ON);
    }

    @Override
    public boolean isRestTime() {
        long time = this.level().getDayTime() % 24000L;
        return (time >= 3000L && time <= 10000L) || (time >= 16000L && time <= 21000L);
    }

    @Override
    public double scoreCoverSpot(BlockPos pos) {
        Level level = this.level();
        double score = level.canSeeSky(pos.above()) ? -10.0D : 10.0D;
        int rock = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState side = level.getBlockState(pos.relative(direction));
            if (side.is(BlockTags.BASE_STONE_OVERWORLD) || side.is(BlockTags.SNOW) || side.is(BlockTags.ICE)) {
                rock++;
            }
        }
        if (rock < 2) {
            return Math.min(score, -1.0D);
        }
        score += rock * 4.0D;
        score += Math.min(MAX_DEN_ELEVATION_BONUS, Math.max(0, (pos.getY() - level.getSeaLevel()) / 16));
        return score;
    }

    @Override
    public boolean isPreyType(LivingEntity entity) {
        return entity.getType().is(TagInit.SNOW_LEOPARD_PREY)
                || (ConfigInit.CONFIG.snowLeopardsHuntLivestock && entity.getType().is(TagInit.SNOW_LEOPARD_LIVESTOCK_PREY));
    }

    @Override
    public double getNoticeDistanceScale() {
        return NOTICE_DISTANCE_SCALE;
    }

    @Override
    public boolean snarlsAtCloseRange() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if ((this.tickCount + this.getId()) % GREET_CHECK_INTERVAL == 0) {
            this.tryGreet();
        }
        if (this.isInLove() && this.random.nextInt(YOWL_CHANCE) == 0) {
            this.playSound(SoundInit.SNOW_LEOPARD_YOWL_EVENT, 1.5F, this.getVoicePitch());
        }
    }

    private void tryGreet() {
        long time = this.level().getGameTime();
        if (time - this.lastGreetTime < GREET_COOLDOWN) {
            return;
        }
        for (SnowLeopardEntity other : this.level().getEntitiesOfClass(SnowLeopardEntity.class, this.getBoundingBox().inflate(GREET_RANGE),
                other -> other != this && other.isAlive())) {
            boolean family = other.getMother() == this || this.getMother() == other;
            boolean courting = this.isInLove() && other.isInLove();
            if ((family || courting) && time - other.lastGreetTime >= GREET_COOLDOWN) {
                this.playSound(SoundInit.SNOW_LEOPARD_PRUSTEN_EVENT, 0.8F, this.getVoicePitch());
                this.lastGreetTime = time;
                other.lastGreetTime = time;
                return;
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.SNOW_LEOPARD_IDLE_EVENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.SNOW_LEOPARD_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.SNOW_LEOPARD_DEATH_EVENT;
    }

    @Override
    protected SoundEvent getWarningSound() {
        return SoundInit.SNOW_LEOPARD_ATTACK_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.SNOW_LEOPARD_STEP_EVENT, 0.15F, 1.0F);
    }
}
