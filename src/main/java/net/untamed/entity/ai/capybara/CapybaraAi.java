package net.untamed.entity.ai.capybara;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.GateBehavior.OrderPolicy;
import net.minecraft.world.entity.ai.behavior.GateBehavior.RunningPolicy;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.untamed.entity.CapybaraEntity;
import net.untamed.entity.ai.RestIdle;
import net.untamed.entity.ai.SetPlayerLookTargetSometimes;
import net.untamed.init.BrainInit;
import net.untamed.init.EntityInit;
import net.untamed.init.TagInit;

import java.util.Optional;
import java.util.function.Predicate;

public class CapybaraAi {

    private static final int REST_MIN_DURATION = 150;
    private static final int REST_MAX_DURATION = 400;
    private static final float SPEED_MULTIPLIER_PANIC = 1.6F;
    private static final float SPEED_MULTIPLIER_STROLL = 1.0F;
    private static final float SPEED_MULTIPLIER_SWIM = 0.9F;
    private static final float SPEED_MULTIPLIER_TEMPTED = 1.25F;
    private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 1.1F;
    private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);

    public static Predicate<ItemStack> getTemptations() {
        return itemStack -> itemStack.is(TagInit.CAPYBARA_FOOD);
    }

    private static final ImmutableList<SensorType<? extends Sensor<? super CapybaraEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            SensorType.NEAREST_ADULT,
            BrainInit.CAPYBARA_PREDATORS,
            BrainInit.CAPYBARA_HERD,
            BrainInit.CAPYBARA_TEMPTATIONS);

    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.NEAREST_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_VISIBLE_ADULT,
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
            MemoryModuleType.PATH,
            MemoryModuleType.BREED_TARGET,
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.TEMPTING_PLAYER,
            MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
            MemoryModuleType.IS_TEMPTED,
            MemoryModuleType.IS_PANICKING,
            MemoryModuleType.IS_IN_WATER,
            BrainInit.NEAREST_VISIBLE_PREDATOR,
            BrainInit.NEAREST_HERD_MEMBERS,
            BrainInit.IDLE_REST,
            BrainInit.LOOK_AT_PLAYER_COOLDOWN);

    public static Brain.Provider<CapybaraEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<CapybaraEntity> brain) {
        initCoreActivity(brain);
        initPanicActivity(brain);
        initSwimActivity(brain);
        initIdleActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(Brain<CapybaraEntity> brain) {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink(),
                new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)));
    }

    private static void initPanicActivity(Brain<CapybaraEntity> brain) {
        brain.addActivityAndRemoveMemoriesWhenStopped(Activity.PANIC,
                ImmutableList.of(Pair.of(0, new FleeToWater(SPEED_MULTIPLIER_PANIC))),
                ImmutableSet.of(Pair.of(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_PRESENT)),
                ImmutableSet.of(MemoryModuleType.IS_PANICKING));
    }

    private static void initSwimActivity(Brain<CapybaraEntity> brain) {
        brain.addActivityWithConditions(Activity.SWIM, ImmutableList.of(
                        Pair.of(0, BehaviorBuilder.triggerIf(e -> !e.isDiving(),
                                SetPlayerLookTargetSometimes.create(6.0F, UniformInt.of(30, 60)))),
                        Pair.of(1, new FollowTemptation(livingEntity -> SPEED_MULTIPLIER_TEMPTED)),
                        Pair.of(2, new AnimalMakeLove(EntityInit.CAPYBARA, SPEED_MULTIPLIER_WHEN_MAKING_LOVE, 2)),
                        Pair.of(3, BehaviorBuilder.triggerIf(e -> !e.isDiving(), BabyFollowAdult.create(
                                ADULT_FOLLOW_RANGE, livingEntity -> SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT))),
                        Pair.of(4, new OccasionalDive(SPEED_MULTIPLIER_SWIM)),
                        Pair.of(5, new SeekLand(SPEED_MULTIPLIER_STROLL)),
                        Pair.of(6, new RestIdle<>(REST_MIN_DURATION, REST_MAX_DURATION,
                                c -> !c.isDiving() && (!c.isInWater() || c.hasSurfaceAccess()),
                                CapybaraAi::herdLookTarget)),
                        Pair.of(7, new SoakInWater()),
                        Pair.of(8, new GateBehavior<>(
                                ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                                ImmutableSet.of(),
                                OrderPolicy.ORDERED,
                                RunningPolicy.TRY_ALL,
                                ImmutableList.of(
                                        Pair.of(BehaviorBuilder.triggerIf(
                                                e -> !e.isDiving()
                                                        && !e.getBrain().hasMemoryValue(BrainInit.IDLE_REST)
                                                        && e.getRandom().nextInt(80) == 0,
                                                (OneShot<? super CapybaraEntity>) RandomStroll.swim(SPEED_MULTIPLIER_SWIM)), 1),
                                        Pair.of(RandomStroll.stroll(SPEED_MULTIPLIER_STROLL, false), 1))))),
                ImmutableSet.of(Pair.of(MemoryModuleType.IS_IN_WATER, MemoryStatus.VALUE_PRESENT)));
    }


    private static void initIdleActivity(Brain<CapybaraEntity> brain) {
        brain.addActivity(Activity.IDLE, ImmutableList.of(
                Pair.of(0, SetPlayerLookTargetSometimes.create(6.0F, UniformInt.of(30, 60))),
                Pair.of(1, new AnimalMakeLove(EntityInit.CAPYBARA, SPEED_MULTIPLIER_WHEN_MAKING_LOVE, 2)),
                Pair.of(2, new RunOne<>(ImmutableList.of(
                        Pair.of(new FollowTemptation(
                                livingEntity -> SPEED_MULTIPLIER_TEMPTED), 1),
                        Pair.of(BabyFollowAdult.create(ADULT_FOLLOW_RANGE,
                                livingEntity -> SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT), 1)))),
                Pair.of(3, new HerdTogether(SPEED_MULTIPLIER_STROLL)),
                Pair.of(4, new SeekWater(SPEED_MULTIPLIER_STROLL)),
                Pair.of(5, new RestIdle<>(REST_MIN_DURATION, REST_MAX_DURATION,
                        c -> !c.isInWater() || c.hasSurfaceAccess(),
                        CapybaraAi::herdLookTarget)),
                Pair.of(6, new GateBehavior<>(ImmutableMap.of(MemoryModuleType.WALK_TARGET,
                        MemoryStatus.VALUE_ABSENT),
                        ImmutableSet.of(),
                        OrderPolicy.ORDERED,
                        RunningPolicy.TRY_ALL,
                        ImmutableList.of(Pair.of(BehaviorBuilder.triggerIf(
                                e -> !e.isFloating()
                                        && (e.getWaterAnchor() == null || e.blockPosition().distSqr(e.getWaterAnchor()) < 576)
                                        && !e.getBrain().hasMemoryValue(BrainInit.IDLE_REST)
                                        && e.getRandom().nextInt(40) == 0,
                                RandomStroll.stroll(SPEED_MULTIPLIER_STROLL)), 1))))));
    }

    private static Optional<PositionTracker> herdLookTarget(CapybaraEntity capybara) {
        return capybara.getBrain().getMemory(BrainInit.NEAREST_HERD_MEMBERS)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(capybara.getRandom().nextInt(list.size())))
                .filter(LivingEntity::isAlive)
                .map(member -> new EntityTracker(member, true));
    }

    public static void updateActivity(CapybaraEntity capybara) {
        capybara.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.PANIC, Activity.SWIM, Activity.IDLE));
    }
}
