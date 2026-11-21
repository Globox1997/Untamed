package net.untamed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.untamed.entity.ai.bluewing.PounceHuntGoal;
import net.untamed.init.EntityInit;
import net.untamed.init.TagInit;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluewingEntity extends AbstractBirdEntity {

    public static class BluewingGroupData extends AgeableMob.AgeableMobGroupData {
        private final UUID familyId = UUID.randomUUID();

        public BluewingGroupData() {
            super(0.25F);
        }
    }

    private static final int FAMILY_REFRESH_INTERVAL = 40;
    private static final double FAMILY_RANGE = 24.0D;
    private static final double PERCH_NEAR_FAMILY_RANGE = 16.0D;
    private static final double ALARM_RANGE = 16.0D;
    private static final int CHORUS_CALLS = 3;
    private static final int CHORUS_START_VARIATION = 30;
    private static final int CHORUS_MEMBER_DELAY = 10;
    private static final int MIN_CALL_GAP = 8;
    private static final int CALL_GAP_VARIATION = 8;

    @Nullable
    private UUID familyId;
    private List<BluewingEntity> family = List.of();
    private long lastChorusKey = -1L;
    private int chorusDelay;
    private int chorusCallsLeft;

    public BluewingEntity(EntityType<? extends BluewingEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.FLYING_SPEED, 0.78D).add(Attributes.MOVEMENT_SPEED, 0.2D).add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    public static boolean checkBluewingEntitySpawnRules(EntityType<BluewingEntity> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource) {
        return !levelAccessor.getBiome(blockPos).is(BiomeTags.IS_SAVANNA) ? checkAnimalSpawnRules(entityType, levelAccessor, mobSpawnType, blockPos, randomSource)
                : isBrightEnoughToSpawn(levelAccessor, blockPos) && levelAccessor.getBlockState(blockPos.below()).is(TagInit.BLUEWINGS_SPAWNABLE_ON);
    }

    @Override
    protected void registerSpeciesGoals() {
        this.goalSelector.addGoal(5, new PounceHuntGoal(this));
    }

    @Override
    protected TagKey<Item> getFoodTag() {
        return TagInit.BLUEWING_FOOD;
    }

    @Override
    public int getFlyRange() {
        return 20;
    }

    @Override
    public double getFlySpeedModifier() {
        return 1.4D;
    }

    @Override
    public int getMinPerchTicks() {
        return 100;
    }

    @Override
    public int getPerchTicksVariation() {
        return 200;
    }

    @Override
    public boolean freezesWhenThreatened() {
        return false;
    }

    @Override
    protected float getSpeciesPitch() {
        return 1.15F;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        BluewingEntity chick = EntityInit.BLUEWING.create(serverLevel);
        if (chick != null) {
            chick.familyId = this.familyId;
        }
        return chick;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, DifficultyInstance difficultyInstance, MobSpawnType mobSpawnType, @Nullable SpawnGroupData spawnGroupData) {
        if (spawnGroupData == null) {
            spawnGroupData = new BluewingGroupData();
        }
        if (spawnGroupData instanceof BluewingGroupData groupData) {
            this.familyId = groupData.familyId;
        }
        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.familyId == null) {
            this.familyId = this.getUUID();
        }
        if ((this.tickCount + this.getId()) % FAMILY_REFRESH_INTERVAL == 0) {
            this.refreshFamily();
            this.tryStartChorus();
        }
        this.tickChorus();
    }

    private void refreshFamily() {
        List<BluewingEntity> members = new ArrayList<>(this.level().getEntitiesOfClass(BluewingEntity.class, this.getBoundingBox().inflate(FAMILY_RANGE),
                other -> other != this && other.isAlive() && this.familyId != null && this.familyId.equals(other.familyId)));
        this.family = List.copyOf(members);
    }

    public List<BluewingEntity> getFamily() {
        return this.family;
    }

    private boolean isFamilyLeader() {
        if (this.isBaby()) {
            return false;
        }
        for (BluewingEntity member : this.family) {
            if (!member.isBaby() && member.getUUID().compareTo(this.getUUID()) < 0) {
                return false;
            }
        }
        return true;
    }

    private long chorusWindowKey() {
        long dayTime = this.level().getDayTime();
        long time = dayTime % 24000L;
        long day = dayTime / 24000L;
        if (time >= 23000L) {
            return (day + 1) * 2L;
        }
        if (time < 1000L) {
            return day * 2L;
        }
        if (time >= 12000L && time < 13000L) {
            return day * 2L + 1L;
        }
        return -1L;
    }

    private void tryStartChorus() {
        long key = this.chorusWindowKey();
        if (key < 0L || key == this.lastChorusKey || !this.isFamilyLeader() || this.isAlarmed()) {
            return;
        }
        this.lastChorusKey = key;
        this.scheduleChorus(this.random.nextInt(CHORUS_START_VARIATION));
        int index = 1;
        for (BluewingEntity member : this.family) {
            member.lastChorusKey = key;
            member.scheduleChorus(index * CHORUS_MEMBER_DELAY + this.random.nextInt(CHORUS_START_VARIATION));
            index++;
        }
    }

    private void scheduleChorus(int delay) {
        this.chorusDelay = Math.max(1, delay);
        this.chorusCallsLeft = CHORUS_CALLS;
    }

    private void tickChorus() {
        if (this.chorusCallsLeft <= 0 || --this.chorusDelay > 0) {
            return;
        }
        this.playCall((this.random.nextFloat() - 0.5F) * 0.4F);
        this.chorusCallsLeft--;
        this.chorusDelay = MIN_CALL_GAP + this.random.nextInt(CALL_GAP_VARIATION);
    }

    @Nullable
    @Override
    public BlockPos getPreferredPerchCenter() {
        for (BluewingEntity member : this.family) {
            if (member.isPerched() && this.distanceToSqr(member) < PERCH_NEAR_FAMILY_RANGE * PERCH_NEAR_FAMILY_RANGE) {
                return member.blockPosition();
            }
        }
        return null;
    }

    @Override
    public void onThreatAlarm(LivingEntity threat) {
        this.playCall(0.5F);
        for (BluewingEntity member : this.family) {
            if (member.distanceToSqr(this) < ALARM_RANGE * ALARM_RANGE) {
                member.warnOf(threat);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        this.familyId = compoundTag.hasUUID("Family") ? compoundTag.getUUID("Family") : null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        if (this.familyId != null) {
            compoundTag.putUUID("Family", this.familyId);
        }
    }
}
