package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.ai.hyena.*;
import net.untamed.init.ConfigInit;
import net.untamed.init.EntityInit;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HyenaEntity extends Animal implements NeutralMob {

    private static final EntityDataAccessor<Byte> DATA_HYENA_POSE = SynchedEntityData.defineId(HyenaEntity.class, EntityDataSerializers.BYTE);
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);

    public static final int MAX_CLAN_SIZE = 5;
    private static final int CLAN_REFRESH_INTERVAL = 40;
    private static final double CLAN_RANGE = 64.0D;
    private static final double JOIN_RANGE = 16.0D;
    private static final float CLAN_CUB_CHANCE = 0.25F;
    private static final int MAX_HUNGER = 48000;
    public static final int HUNGRY = 18000;
    private static final int MAX_STAMINA = 600;
    private static final int MAX_TOLERANCE_ENTRIES = 8;
    private static final double WHOOP_RANGE = 64.0D;
    private static final int WHOOP_POSE_TICKS = 40;
    private static final int RECRUIT_WHOOP_COOLDOWN = 100;
    private static final int RALLY_TICKS = 600;

    private int warningSoundTicks;
    private int remainingPersistentAngerTime;
    @Nullable
    private UUID persistentAngerTarget;

    @Nullable
    private UUID clanId;
    @Nullable
    private BlockPos denPos;
    private List<HyenaEntity> clan = List.of();
    private boolean clanInitialized;

    private int alarmTicks;
    private int hunger;
    private int stamina = MAX_STAMINA;
    private boolean drainedStamina;
    @Nullable
    private HyenaHunt hunt;
    private long huntCooldownUntil;
    private long whoopRequestTime;
    private boolean whoopRecruit;
    private long answerWhoopTime;
    private long lastRecruitWhoopTime = -24000L;
    private int whoopPoseTicks;
    private long lastHeardWhoopTime = -24000L;
    @Nullable
    private Vec3 rallyPos;
    @Nullable
    private HyenaEntity rallyCaller;
    private int rallyTicks;
    @Nullable
    private Vec3 forageTarget;
    private final Map<UUID, Integer> playerTolerance = new HashMap<>();

    private float restAmount, restAmountO;
    private float sniffAmount, sniffAmountO;
    private float alertAmount, alertAmountO;
    private float excitedAmount, excitedAmountO;
    private float whoopAmount, whoopAmountO;
    private float eatAmount, eatAmountO;

    public HyenaEntity(EntityType<? extends HyenaEntity> entityType, Level level) {
        super(entityType, level);
    }

    public enum HyenaPose {
        NONE, RESTING, SNIFFING, ALERT, EXCITED, WHOOPING, EATING;

        private static final HyenaPose[] VALUES = values();

        public static HyenaPose byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    public static class ClanGroupData extends AgeableMob.AgeableMobGroupData {
        private final UUID clanId;
        private final BlockPos den;
        private boolean membersAdded;

        public ClanGroupData(UUID clanId, BlockPos den) {
            super(CLAN_CUB_CHANCE);
            this.clanId = clanId;
            this.den = den;
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 2.0, pathfinderMob -> pathfinderMob.isBaby() ? DamageTypeTags.PANIC_CAUSES : DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new HyenaMeleeAttackGoal(this, 1.3));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new HyenaThreatResponseGoal(this));
        this.goalSelector.addGoal(5, new HyenaHuntGoal(this));
        this.goalSelector.addGoal(5, new ScavengeGoal(this));
        this.goalSelector.addGoal(6, new RallyGoal(this));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(7, new WhoopGoal(this));
        this.goalSelector.addGoal(7, new ReturnToDenGoal(this));
        this.goalSelector.addGoal(8, new HyenaRestGoal(this));
        this.goalSelector.addGoal(9, new ForageGoal(this));
        this.goalSelector.addGoal(10, new HyenaStrollGoal(this, 0.8));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new ClanHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MOVEMENT_SPEED, 0.28D).add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    public static boolean checkHyenaEntitySpawnRules(EntityType<HyenaEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        Holder<Biome> holder = levelAccessor.getBiome(blockPos);
        return !holder.is(BiomeTags.IS_SAVANNA)
                ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.HYENAS_SPAWNABLE_ON);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HYENA_POSE, (byte) HyenaPose.NONE.ordinal());
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagInit.HYENA_FOOD);
    }

    // Breeding: a full clan does not breed, cubs join their parent's clan

    @Override
    public boolean canFallInLove() {
        return super.canFallInLove() && this.clan.size() + 1 < MAX_CLAN_SIZE;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        HyenaEntity cub = EntityInit.HYENA.create(serverLevel);
        if (cub != null) {
            cub.joinClan(this.clanId, this.denPos);
        }
        return cub;
    }

    // Spawning: a natural spawn founds a clan and brings 2 to 4 more members

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            boolean natural = mobSpawnType == MobSpawnType.NATURAL || mobSpawnType == MobSpawnType.CHUNK_GENERATION;
            spawnGroupData = natural ? new ClanGroupData(UUID.randomUUID(), this.blockPosition()) : new AgeableMobGroupData(false);
        }
        if (spawnGroupData instanceof ClanGroupData clanData) {
            this.joinClan(clanData.clanId, clanData.den);
        }
        this.hunger = this.random.nextInt(HUNGRY);
        SpawnGroupData result = super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
        if (spawnGroupData instanceof ClanGroupData clanData && !clanData.membersAdded && !this.isBaby()) {
            clanData.membersAdded = true;
            this.spawnClanMembers(serverLevelAccessor, difficultyInstance, mobSpawnType, clanData);
        }
        return result;
    }

    private void spawnClanMembers(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, ClanGroupData clanData) {
        int members = 2 + this.random.nextInt(MAX_CLAN_SIZE - 2);
        for (int i = 0; i < members; i++) {
            HyenaEntity member = EntityInit.HYENA.create(level.getLevel());
            if (member != null) {
                member.moveTo(this.getX(), this.getY(), this.getZ(), this.random.nextFloat() * 360.0F, 0.0F);
                member.finalizeSpawn(level, difficulty, spawnType, clanData);
                level.addFreshEntityWithPassengers(member);
            }
        }
    }

    // Clan

    public void joinClan(@Nullable UUID clanId, @Nullable BlockPos den) {
        this.clanId = clanId;
        this.denPos = den;
        this.clanInitialized = false;
    }

    @Nullable
    public BlockPos getDenPos() {
        return this.denPos;
    }

    public boolean isInClanWith(HyenaEntity other) {
        return this.clanId != null && this.clanId.equals(other.clanId);
    }

    public List<HyenaEntity> getClan() {
        return this.clan;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.alarmTicks > 0) {
            this.alarmTicks--;
        }
        if (this.getTarget() != null) {
            this.alarm(20);
        }
        if (this.hunger < MAX_HUNGER) {
            this.hunger++;
        }
        if (!this.drainedStamina && this.stamina < MAX_STAMINA) {
            this.stamina++;
        }
        this.drainedStamina = false;
        long time = this.level().getGameTime();
        if (this.whoopRequestTime != 0L && time >= this.whoopRequestTime) {
            boolean recruit = this.whoopRecruit;
            this.whoopRequestTime = 0L;
            this.whoopRecruit = false;
            this.whoop(recruit, false);
        }
        if (this.answerWhoopTime != 0L && time >= this.answerWhoopTime) {
            this.answerWhoopTime = 0L;
            this.whoop(false, false);
        }
        if (this.whoopPoseTicks > 0 && --this.whoopPoseTicks == 0) {
            this.clearHyenaPose(HyenaPose.WHOOPING);
        }
        if (this.rallyTicks > 0 && --this.rallyTicks == 0) {
            this.clearRally();
        }
        if (this.hunt != null && this.hunt.isFinished()) {
            this.hunt = null;
        }
        if (!this.clanInitialized || (this.tickCount + this.getId()) % CLAN_REFRESH_INTERVAL == 0) {
            this.clanInitialized = true;
            this.refreshClan();
        }
    }

    private void refreshClan() {
        List<HyenaEntity> nearby = new ArrayList<>(this.level().getEntitiesOfClass(HyenaEntity.class, this.getBoundingBox().inflate(CLAN_RANGE, 16.0D, CLAN_RANGE),
                other -> other != this && other.isAlive()));
        nearby.sort(Comparator.comparingDouble(this::distanceToSqr));
        if (this.clanId == null) {
            this.tryJoinClan(nearby);
        }
        List<HyenaEntity> members = new ArrayList<>();
        for (HyenaEntity other : nearby) {
            if (this.isInClanWith(other) && members.size() < MAX_CLAN_SIZE - 1) {
                members.add(other);
            }
        }
        this.clan = List.copyOf(members);
    }

    // Hyenas without a clan (spawn eggs, old saves) join a clan nearby that has room, or found their own
    private void tryJoinClan(List<HyenaEntity> nearby) {
        for (HyenaEntity other : nearby) {
            if (other.clanId == null || this.distanceTo(other) > JOIN_RANGE) {
                continue;
            }
            int size = 1;
            for (HyenaEntity member : nearby) {
                if (other.clanId.equals(member.clanId)) {
                    size++;
                }
            }
            if (size < MAX_CLAN_SIZE) {
                this.joinClan(other.clanId, other.denPos != null ? other.denPos : other.blockPosition());
                this.clanInitialized = true;
                return;
            }
        }
        this.joinClan(UUID.randomUUID(), this.blockPosition());
        this.clanInitialized = true;
    }

    public int countAdultsNearby(double distance) {
        int count = 0;
        for (HyenaEntity member : this.clan) {
            if (member.isAlive() && !member.isBaby() && this.distanceToSqr(member) < distance * distance) {
                count++;
            }
        }
        return count;
    }

    public boolean hasCubNearby(double distance) {
        for (HyenaEntity member : this.clan) {
            if (member.isAlive() && member.isBaby() && this.distanceToSqr(member) < distance * distance) {
                return true;
            }
        }
        return false;
    }

    public int countRestingNearby(double distance) {
        int count = 0;
        for (HyenaEntity member : this.clan) {
            if (member.isAlive() && member.isResting() && this.distanceToSqr(member) < distance * distance) {
                count++;
            }
        }
        return count;
    }

    public boolean isNearDen(double distance) {
        return this.denPos != null && this.blockPosition().distSqr(this.denPos) < distance * distance;
    }

    // Daily rhythm: rest through the day, active at dusk, night and dawn

    public boolean isRestTime() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 1000L && time <= 11500L;
    }

    public boolean isDawn() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 22500L || time < 1000L;
    }

    // Alarm

    public boolean isAlarmed() {
        return this.alarmTicks > 0;
    }

    public void alarm(int ticks) {
        this.alarmTicks = Math.max(this.alarmTicks, ticks);
    }

    public void alarmClan(int ticks, double distance) {
        this.alarm(ticks);
        for (HyenaEntity member : this.clan) {
            if (member.isAlive() && this.distanceToSqr(member) < distance * distance) {
                member.alarm(ticks);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource damageSource, float f) {
        boolean result = super.hurt(damageSource, f);
        if (result && !this.level().isClientSide()) {
            this.alarmClan(200, 24.0D);
            this.clearHyenaPose(HyenaPose.RESTING);
            if (damageSource.getEntity() instanceof Player player) {
                this.playerTolerance.remove(player.getUUID());
            }
            if (!this.isBaby()) {
                this.requestWhoop(5, true);
            }
        }
        return result;
    }

    // Player tolerance

    public int getTolerance(Player player) {
        return this.playerTolerance.getOrDefault(player.getUUID(), 0);
    }

    public void addTolerance(Player player, int amount, int max) {
        if (!this.playerTolerance.containsKey(player.getUUID()) && this.playerTolerance.size() >= MAX_TOLERANCE_ENTRIES) {
            this.playerTolerance.clear();
        }
        this.playerTolerance.merge(player.getUUID(), amount, (a, b) -> Math.min(max, a + b));
    }

    // Provoked hyenas bring the clan along
    public void provokeBy(Player player, double rallyRange) {
        this.setTarget(player);
        this.setPersistentAngerTarget(player.getUUID());
        this.startPersistentAngerTimer();
        this.playerTolerance.remove(player.getUUID());
        this.requestWhoop(0, true);
        for (HyenaEntity member : this.clan) {
            if (member.isAlive() && !member.isBaby() && member.getTarget() == null && this.distanceToSqr(member) < rallyRange * rallyRange) {
                member.setTarget(player);
                member.setPersistentAngerTarget(player.getUUID());
                member.startPersistentAngerTimer();
            }
        }
    }

    // Hunger, stamina and hunting

    public boolean isHungry() {
        return this.hunger >= HUNGRY;
    }

    public boolean isPeckish() {
        return this.hunger >= HUNGRY / 2;
    }

    public void feed(int amount) {
        this.hunger = Math.max(0, this.hunger - amount);
    }

    public int getStamina() {
        return this.stamina;
    }

    public void drainStamina() {
        this.drainedStamina = true;
        if (this.stamina > 0) {
            this.stamina--;
        }
    }

    @Nullable
    public HyenaHunt getHunt() {
        return this.hunt;
    }

    public void setHunt(@Nullable HyenaHunt hunt) {
        this.hunt = hunt;
    }

    @Nullable
    public LivingEntity getHuntPrey() {
        return this.hunt != null && !this.hunt.isFinished() ? this.hunt.getPrey() : null;
    }

    public boolean canStartHunt() {
        return this.level().getGameTime() >= this.huntCooldownUntil;
    }

    public void setHuntCooldown(int ticks) {
        this.huntCooldownUntil = this.level().getGameTime() + ticks;
    }

    public boolean isValidPrey(LivingEntity entity, int hunters) {
        if (!(entity instanceof Animal animal) || !animal.isAlive() || !isPreyType(animal)) {
            return false;
        }
        if (animal.isLeashed() || animal.hasCustomName() || (animal instanceof TamableAnimal tamable && tamable.isTame()) || (animal instanceof AbstractHorse horse && horse.isTamed())) {
            return false;
        }
        return !animal.getType().is(TagInit.HYENA_LARGE_PREY) || hunters >= 4 || animal.isBaby();
    }

    public static boolean isPreyType(LivingEntity entity) {
        return entity.getType().is(TagInit.HYENA_PREY) || entity.getType().is(TagInit.HYENA_LARGE_PREY)
                || (ConfigInit.CONFIG.hyenasHuntLivestock && entity.getType().is(TagInit.HYENA_LIVESTOCK_PREY));
    }

    @Override
    public boolean killedEntity(ServerLevel serverLevel, LivingEntity livingEntity) {
        if (isPreyType(livingEntity)) {
            this.feed(HUNGRY);
            for (HyenaEntity member : this.clan) {
                if (member.isAlive() && this.distanceToSqr(member) < 32.0D * 32.0D) {
                    member.feed(HUNGRY);
                }
            }
        }
        return super.killedEntity(serverLevel, livingEntity);
    }

    // Whooping and rallying

    public void requestWhoop(int delay, boolean recruit) {
        long time = this.level().getGameTime() + delay;
        if (this.whoopRequestTime == 0L || time < this.whoopRequestTime) {
            this.whoopRequestTime = time;
        }
        this.whoopRecruit |= recruit;
    }

    // Contact whoops may be answered by clan members; recruiting whoops call clan members to this hyena
    public void whoop(boolean recruit, boolean invitesAnswer) {
        long time = this.level().getGameTime();
        if (recruit) {
            if (time - this.lastRecruitWhoopTime < RECRUIT_WHOOP_COOLDOWN) {
                return;
            }
            this.lastRecruitWhoopTime = time;
        }
        this.playWhoop();
        this.hearWhoop();
        this.whoopPoseTicks = WHOOP_POSE_TICKS;
        if (this.getHyenaPose() == HyenaPose.NONE) {
            this.setHyenaPose(HyenaPose.WHOOPING);
        }
        for (HyenaEntity other : this.level().getEntitiesOfClass(HyenaEntity.class, this.getBoundingBox().inflate(WHOOP_RANGE, 16.0D, WHOOP_RANGE),
                other -> other != this && other.isAlive())) {
            other.hearWhoop();
            if (!this.isInClanWith(other) || other.isBaby()) {
                continue;
            }
            if (recruit && other.getTarget() == null && other.getHunt() == null) {
                other.rallyTo(this, RALLY_TICKS);
            } else if (invitesAnswer && !other.isResting() && this.random.nextBoolean()) {
                other.answerWhoopTime = time + 20 + this.random.nextInt(40);
            }
        }
    }

    public long getLastHeardWhoopTime() {
        return this.lastHeardWhoopTime;
    }

    public void hearWhoop() {
        this.lastHeardWhoopTime = this.level().getGameTime();
    }

    public void rallyTo(HyenaEntity caller, int ticks) {
        this.rallyCaller = caller;
        this.rallyPos = caller.position();
        this.rallyTicks = ticks;
    }

    @Nullable
    public Vec3 getRallyPos() {
        return this.rallyPos;
    }

    @Nullable
    public HyenaEntity getRallyCaller() {
        return this.rallyCaller;
    }

    public void clearRally() {
        this.rallyPos = null;
        this.rallyCaller = null;
        this.rallyTicks = 0;
    }

    // Foraging

    @Nullable
    public Vec3 getForageTarget() {
        return this.forageTarget;
    }

    public void setForageTarget(@Nullable Vec3 forageTarget) {
        this.forageTarget = forageTarget;
    }

    // Pose

    public HyenaPose getHyenaPose() {
        return HyenaPose.byId(this.entityData.get(DATA_HYENA_POSE));
    }

    public void setHyenaPose(HyenaPose pose) {
        this.entityData.set(DATA_HYENA_POSE, (byte) pose.ordinal());
    }

    public void clearHyenaPose(HyenaPose pose) {
        if (this.getHyenaPose() == pose) {
            this.setHyenaPose(HyenaPose.NONE);
        }
    }

    public boolean isResting() {
        return this.getHyenaPose() == HyenaPose.RESTING;
    }

    public float getRestAmount(float partialTick) {
        return Mth.lerp(partialTick, this.restAmountO, this.restAmount);
    }

    public float getSniffAmount(float partialTick) {
        return Mth.lerp(partialTick, this.sniffAmountO, this.sniffAmount);
    }

    public float getAlertAmount(float partialTick) {
        return Mth.lerp(partialTick, this.alertAmountO, this.alertAmount);
    }

    public float getExcitedAmount(float partialTick) {
        return Mth.lerp(partialTick, this.excitedAmountO, this.excitedAmount);
    }

    public float getWhoopAmount(float partialTick) {
        return Mth.lerp(partialTick, this.whoopAmountO, this.whoopAmount);
    }

    public float getEatAmount(float partialTick) {
        return Mth.lerp(partialTick, this.eatAmountO, this.eatAmount);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.warningSoundTicks > 0) {
            this.warningSoundTicks--;
        }

        if (this.level().isClientSide()) {
            HyenaPose pose = this.getHyenaPose();
            this.restAmountO = this.restAmount;
            this.sniffAmountO = this.sniffAmount;
            this.alertAmountO = this.alertAmount;
            this.excitedAmountO = this.excitedAmount;
            this.whoopAmountO = this.whoopAmount;
            this.eatAmountO = this.eatAmount;
            this.restAmount = Mth.approach(this.restAmount, pose == HyenaPose.RESTING ? 1.0F : 0.0F, 0.1F);
            this.sniffAmount = Mth.approach(this.sniffAmount, pose == HyenaPose.SNIFFING ? 1.0F : 0.0F, 0.15F);
            this.alertAmount = Mth.approach(this.alertAmount, pose == HyenaPose.ALERT ? 1.0F : 0.0F, 0.2F);
            this.excitedAmount = Mth.approach(this.excitedAmount, pose == HyenaPose.EXCITED ? 1.0F : 0.0F, 0.2F);
            this.whoopAmount = Mth.approach(this.whoopAmount, pose == HyenaPose.WHOOPING ? 1.0F : 0.0F, 0.15F);
            this.eatAmount = Mth.approach(this.eatAmount, pose == HyenaPose.EATING ? 1.0F : 0.0F, 0.15F);
        } else {
            this.updatePersistentAnger((ServerLevel) this.level(), true);
        }
    }

    // Sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundInit.HYENA_IDLE_EVENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundInit.HYENA_HURT_EVENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundInit.HYENA_DEATH_EVENT;
    }

    @Override
    protected void playStepSound(BlockPos blockPos, BlockState blockState) {
        this.playSound(SoundInit.HYENA_STEP_EVENT, 0.15F, 1.0F);
    }

    @Override
    public void playAmbientSound() {
        if (!this.isResting()) {
            super.playAmbientSound();
        }
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.7F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    public void playWarningSound() {
        if (this.warningSoundTicks <= 0) {
            this.makeSound(SoundInit.HYENA_ATTACK_EVENT);
            this.warningSoundTicks = 40;
        }
    }

    public void playWhoop() {
        this.playSound(SoundInit.HYENA_WHOOP_EVENT, 4.0F, this.getVoicePitch());
    }

    // Anger

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public void setRemainingPersistentAngerTime(int i) {
        this.remainingPersistentAngerTime = i;
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID uUID) {
        this.persistentAngerTarget = uUID;
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    // Save data

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        this.addPersistentAngerSaveData(compoundTag);
        if (this.clanId != null) {
            compoundTag.putUUID("Clan", this.clanId);
        }
        if (this.denPos != null) {
            compoundTag.put("Den", NbtUtils.writeBlockPos(this.denPos));
        }
        compoundTag.putInt("Hunger", this.hunger);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.readPersistentAngerSaveData(this.level(), compoundTag);
        this.clanId = compoundTag.hasUUID("Clan") ? compoundTag.getUUID("Clan") : null;
        this.denPos = NbtUtils.readBlockPos(compoundTag, "Den").orElse(null);
        this.hunger = compoundTag.getInt("Hunger");
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }
}
