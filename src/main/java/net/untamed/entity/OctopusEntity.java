package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.ai.octopus.*;
import net.untamed.init.EntityInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class OctopusEntity extends Animal {

    private static final int MAX_TIME_OUT_OF_WATER = 300;
    private static final int MAX_HUNGER = 48000;
    public static final int HUNGRY = 12000;
    private static final int INK_COOLDOWN = 600;
    private static final int INK_CLOUD_TICKS = 30;
    private static final double INK_BLIND_RANGE = 4.0D;
    private static final double JET_STRENGTH = 0.9D;
    private static final double JETTING_SPEED = 0.12D;
    private static final int THREAT_TICKS = 40;

    private static final EntityDataAccessor<Boolean> DATA_CRAWLING =
            SynchedEntityData.defineId(OctopusEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_OCTOPUS_POSE =
            SynchedEntityData.defineId(OctopusEntity.class, EntityDataSerializers.BYTE);

    private int hunger;
    private int inkCooldown;
    @Nullable
    private Vec3 inkCloudPos;
    private int inkCloudTicks;
    private int timedPoseTicks;
    @Nullable
    private BlockPos denPos;

    private float camoR = 1.0F, camoG = 1.0F, camoB = 1.0F;
    private float camoRO = 1.0F, camoGO = 1.0F, camoBO = 1.0F;
    private float camoAmount, camoAmountO;
    private float restAmount, restAmountO;
    private float jetAmount, jetAmountO;
    private float threatAmount, threatAmountO;
    private float pounceAmount, pounceAmountO;
    private float reachAmount, reachAmountO;
    private float stalkAmount, stalkAmountO;

    public OctopusEntity(EntityType<? extends OctopusEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.5F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public enum OctopusPose {
        NONE, RESTING, JETTING, THREAT, POUNCE, REACH, STALKING;

        private static final OctopusPose[] VALUES = values();

        public static OctopusPose byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        OctopusEntity baby = EntityInit.OCTOPUS.create(serverLevel);
        if (baby != null) {
            baby.setPersistenceRequired();
        }
        return baby;
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
        this.goalSelector.addGoal(4, new OctopusPounceGoal(this));
        this.goalSelector.addGoal(4, new CollectItemGoal(this));
        this.goalSelector.addGoal(5, new OctopusDenGoal(this));
        this.goalSelector.addGoal(5, new InvestigatePlayerGoal(this));
        this.goalSelector.addGoal(6, new CrawlOnFloorGoal(this, 0.8, 8, 3));
        this.goalSelector.addGoal(7, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(8, new OctopusRandomSwimmingGoal(this, 1.0, 10));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MOVEMENT_SPEED, 0.22D).add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    public static boolean checkOctopusEntitySpawnRules(EntityType<OctopusEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return holder.is(BiomeTags.IS_OCEAN) && levelAccessor.getFluidState(blockPos).is(FluidTags.WATER)
                && levelAccessor.getBlockState(blockPos.below()).is(TagInit.OCTOPUSES_SPAWNABLE_ON);
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

    public boolean isRestTime() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 1000L && time <= 11500L;
    }

    public boolean isHungry() {
        return this.hunger >= HUNGRY;
    }

    public void feed(int amount) {
        this.hunger = Math.max(0, this.hunger - amount);
    }

    @Override
    public boolean killedEntity(ServerLevel serverLevel, LivingEntity livingEntity) {
        if (livingEntity.getType().is(TagInit.OCTOPUS_PREY)) {
            this.feed(HUNGRY);
        }
        return super.killedEntity(serverLevel, livingEntity);
    }

    @Nullable
    public BlockPos getDenPos() {
        return this.denPos;
    }

    public void setDenPos(@Nullable BlockPos denPos) {
        this.denPos = denPos;
    }

    public int scoreDenSpot(BlockPos pos) {
        Level level = this.level();
        if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            return 0;
        }
        int score = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (!level.getBlockState(side).getCollisionShape(level, side).isEmpty()) {
                score++;
            }
        }
        if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            score += 2;
        }
        return score;
    }

    public boolean isDenSpot(BlockPos pos) {
        return this.scoreDenSpot(pos) >= 3;
    }

    public OctopusPose getOctopusPose() {
        return OctopusPose.byId(this.entityData.get(DATA_OCTOPUS_POSE));
    }

    public void setOctopusPose(OctopusPose pose) {
        this.entityData.set(DATA_OCTOPUS_POSE, (byte) pose.ordinal());
    }

    public void clearOctopusPose(OctopusPose pose) {
        if (this.getOctopusPose() == pose) {
            this.setOctopusPose(OctopusPose.NONE);
        }
    }

    private void setTimedPose(OctopusPose pose, int ticks) {
        this.setOctopusPose(pose);
        this.timedPoseTicks = ticks;
    }

    public boolean isResting() {
        return this.getOctopusPose() == OctopusPose.RESTING;
    }

    public boolean isThreatened() {
        return this.getLastHurtByMob() != null && this.tickCount - this.getLastHurtByMobTimestamp() < 200;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        boolean wasHurt = super.hurt(damageSource, amount);
        if (wasHurt && this.isAlive() && !this.level().isClientSide()) {
            this.clearOctopusPose(OctopusPose.RESTING);
            if (this.isInWater() && this.inkCooldown <= 0) {
                this.releaseInk(damageSource);
            } else {
                this.setTimedPose(OctopusPose.THREAT, THREAT_TICKS);
            }
        }
        return wasHurt;
    }

    private void releaseInk(DamageSource damageSource) {
        this.inkCooldown = INK_COOLDOWN;
        this.inkCloudPos = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        this.inkCloudTicks = INK_CLOUD_TICKS;
        this.playSound(SoundEvents.SQUID_SQUIRT, 1.0F, 0.9F);
        this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));

        Entity attacker = damageSource.getEntity();
        Vec3 away = attacker != null ? this.position().subtract(attacker.position()) : this.getLookAngle().reverse();
        if (away.lengthSqr() > 1.0E-4D) {
            away = away.normalize();
            this.setDeltaMovement(away.x * JET_STRENGTH, Math.max(0.1D, away.y * JET_STRENGTH), away.z * JET_STRENGTH);
            this.hasImpulse = true;
        }
        this.setTimedPose(OctopusPose.JETTING, 20);
        this.inkBlindNearby();
    }

    private void inkBlindNearby() {
        if (this.inkCloudPos == null) {
            return;
        }
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, new AABB(this.inkCloudPos, this.inkCloudPos).inflate(INK_BLIND_RANGE),
                entity -> entity != this && !(entity instanceof OctopusEntity))) {
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.hunger < MAX_HUNGER) {
            this.hunger++;
        }
        if (this.inkCooldown > 0) {
            this.inkCooldown--;
        }
        if (this.inkCloudTicks > 0 && this.inkCloudPos != null && this.level() instanceof ServerLevel serverLevel) {
            if (--this.inkCloudTicks % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.SQUID_INK, this.inkCloudPos.x, this.inkCloudPos.y, this.inkCloudPos.z, 10, 0.6D, 0.5D, 0.6D, 0.02D);
            }
            if (this.inkCloudTicks % 10 == 0) {
                this.inkBlindNearby();
            }
            if (this.inkCloudTicks == 0) {
                this.inkCloudPos = null;
            }
        }
        if (this.timedPoseTicks > 0 && --this.timedPoseTicks == 0) {
            this.clearOctopusPose(OctopusPose.THREAT);
            this.clearOctopusPose(OctopusPose.JETTING);
            this.clearOctopusPose(OctopusPose.POUNCE);
        }
        OctopusPose pose = this.getOctopusPose();
        boolean fastInWater = this.isInWater() && !this.onGround() && this.getDeltaMovement().horizontalDistance() > JETTING_SPEED;
        if (fastInWater && pose == OctopusPose.NONE) {
            this.setOctopusPose(OctopusPose.JETTING);
        } else if (!fastInWater && pose == OctopusPose.JETTING && this.timedPoseTicks <= 0) {
            this.setOctopusPose(OctopusPose.NONE);
        }
    }

    public void startPounce() {
        this.setTimedPose(OctopusPose.POUNCE, 15);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            return;
        }
        OctopusPose pose = this.getOctopusPose();
        this.restAmountO = this.restAmount;
        this.jetAmountO = this.jetAmount;
        this.threatAmountO = this.threatAmount;
        this.pounceAmountO = this.pounceAmount;
        this.reachAmountO = this.reachAmount;
        this.stalkAmountO = this.stalkAmount;
        this.restAmount = Mth.approach(this.restAmount, pose == OctopusPose.RESTING ? 1.0F : 0.0F, 0.08F);
        this.jetAmount = Mth.approach(this.jetAmount, pose == OctopusPose.JETTING ? 1.0F : 0.0F, 0.2F);
        this.threatAmount = Mth.approach(this.threatAmount, pose == OctopusPose.THREAT ? 1.0F : 0.0F, 0.3F);
        this.pounceAmount = Mth.approach(this.pounceAmount, pose == OctopusPose.POUNCE ? 1.0F : 0.0F, 0.35F);
        this.reachAmount = Mth.approach(this.reachAmount, pose == OctopusPose.REACH ? 1.0F : 0.0F, 0.15F);
        this.stalkAmount = Mth.approach(this.stalkAmount, pose == OctopusPose.STALKING ? 1.0F : 0.0F, 0.1F);
        this.updateCamouflage(pose);
    }

    private void updateCamouflage(OctopusPose pose) {
        this.camoRO = this.camoR;
        this.camoGO = this.camoG;
        this.camoBO = this.camoB;
        this.camoAmountO = this.camoAmount;
        float targetAmount = 0.0F;
        if (pose != OctopusPose.THREAT && (this.onGround() || this.isCrawling() || pose == OctopusPose.RESTING || pose == OctopusPose.STALKING)) {
            BlockPos below = this.blockPosition().below();
            MapColor mapColor = this.level().getBlockState(below).getMapColor(this.level(), below);
            if (mapColor != MapColor.NONE) {
                targetAmount = pose == OctopusPose.RESTING || pose == OctopusPose.STALKING ? 0.95F : 0.7F;
                this.camoR = Mth.approach(this.camoR, ((mapColor.col >> 16) & 0xFF) / 255.0F, 0.1F);
                this.camoG = Mth.approach(this.camoG, ((mapColor.col >> 8) & 0xFF) / 255.0F, 0.1F);
                this.camoB = Mth.approach(this.camoB, (mapColor.col & 0xFF) / 255.0F, 0.1F);
            }
        }
        this.camoAmount = Mth.approach(this.camoAmount, targetAmount, pose == OctopusPose.THREAT ? 0.3F : 0.04F);
    }

    public float getCamoAmount(float partialTick) {
        return Mth.lerp(partialTick, this.camoAmountO, this.camoAmount);
    }

    public float getCamoRed(float partialTick) {
        return Mth.lerp(partialTick, this.camoRO, this.camoR);
    }

    public float getCamoGreen(float partialTick) {
        return Mth.lerp(partialTick, this.camoGO, this.camoG);
    }

    public float getCamoBlue(float partialTick) {
        return Mth.lerp(partialTick, this.camoBO, this.camoB);
    }

    public float getRestAmount(float partialTick) {
        return Mth.lerp(partialTick, this.restAmountO, this.restAmount);
    }

    public float getJetAmount(float partialTick) {
        return Mth.lerp(partialTick, this.jetAmountO, this.jetAmount);
    }

    public float getThreatAmount(float partialTick) {
        return Mth.lerp(partialTick, this.threatAmountO, this.threatAmount);
    }

    public float getPounceAmount(float partialTick) {
        return Mth.lerp(partialTick, this.pounceAmountO, this.pounceAmount);
    }

    public float getReachAmount(float partialTick) {
        return Mth.lerp(partialTick, this.reachAmountO, this.reachAmount);
    }

    public float getStalkAmount(float partialTick) {
        return Mth.lerp(partialTick, this.stalkAmountO, this.stalkAmount);
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
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putInt("Hunger", this.hunger);
        if (this.denPos != null) {
            compoundTag.put("Den", NbtUtils.writeBlockPos(this.denPos));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.hunger = compoundTag.getInt("Hunger");
        this.denPos = NbtUtils.readBlockPos(compoundTag, "Den").orElse(null);
    }

    public static class GoToWaterGoal extends MoveToBlockGoal {
        private static final double HOP_DISTANCE = 3.0D;
        private static final int HOP_COOLDOWN = 20;

        private final OctopusEntity octopus;
        private int hopCooldown;

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
        protected int nextStartTick(PathfinderMob pathfinderMob) {
            return reducedTickDelay(10);
        }

        @Override
        public void start() {
            super.start();
            this.hopCooldown = 0;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.hopCooldown > 0) {
                this.hopCooldown--;
                return;
            }
            if (!this.octopus.onGround() || this.octopus.isInWater()) {
                return;
            }
            Vec3 target = Vec3.atCenterOf(this.blockPos);
            Vec3 horizontal = new Vec3(target.x - this.octopus.getX(), 0.0D, target.z - this.octopus.getZ());
            if (horizontal.lengthSqr() < HOP_DISTANCE * HOP_DISTANCE && horizontal.lengthSqr() > 1.0E-4D) {
                Vec3 direction = horizontal.normalize();
                this.octopus.setDeltaMovement(direction.x * 0.35D, 0.42D, direction.z * 0.35D);
                this.octopus.hasImpulse = true;
                this.hopCooldown = HOP_COOLDOWN;
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
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
            if (!this.octopus.isInWater() || this.octopus.isResting()) {
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
        }

        @Override
        public void tick() {
            this.octopus.setCrawling(this.octopus.onGround());
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
            return !this.octopus.isResting() && this.octopus.getRandom().nextInt(4) == 0 && super.canUse();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.OCTOPUS_IDLE_EVENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.OCTOPUS_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.OCTOPUS_DEATH_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.OCTOPUS_STEP_EVENT, 0.15F, 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CRAWLING, false);
        builder.define(DATA_OCTOPUS_POSE, (byte) OctopusPose.NONE.ordinal());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            spawnGroupData = new AgeableMobGroupData(false);
        }
        if (mobSpawnType == MobSpawnType.SPAWN_EGG || mobSpawnType == MobSpawnType.BUCKET) {
            this.setPersistenceRequired();
        }
        this.hunger = this.random.nextInt(HUNGRY);
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
    }
}
