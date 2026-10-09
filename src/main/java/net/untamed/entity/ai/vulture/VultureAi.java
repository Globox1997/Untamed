package net.untamed.entity.ai.vulture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AnimalMakeLove;
import net.minecraft.world.entity.ai.behavior.BabyFollowAdult;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.untamed.entity.VultureEntity;
import net.untamed.init.BrainInit;
import net.untamed.init.EntityInit;

public class VultureAi {

    private static final float SPEED_MULTIPLIER_STROLL = 0.8F;
    private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 1.1F;
    private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(4, 12);

    private static final ImmutableList<SensorType<? extends Sensor<? super VultureEntity>>> SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            SensorType.HURT_BY,
            SensorType.NEAREST_ADULT);

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
            BrainInit.IS_DAYTIME);

    public static Brain.Provider<VultureEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    public static Brain<?> makeBrain(Brain<VultureEntity> brain) {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink()));
        brain.addActivity(Activity.IDLE, ImmutableList.of(
                Pair.of(0, new VultureFlee()),
                Pair.of(1, new AnimalMakeLove(EntityInit.VULTURE, SPEED_MULTIPLIER_WHEN_MAKING_LOVE, 2)),
                Pair.of(2, new FeedAtCarcass()),
                Pair.of(3, BabyFollowAdult.create(ADULT_FOLLOW_RANGE, livingEntity -> SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT)),
                Pair.of(4, new RunOne<>(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), ImmutableList.of(
                        Pair.of(BehaviorBuilder.triggerIf(VultureEntity::canWander, RandomStroll.stroll(SPEED_MULTIPLIER_STROLL)), 2),
                        Pair.of(new DoNothing(60, 160), 3))))));
        brain.addActivityWithConditions(BrainInit.NIGHT, ImmutableList.of(
                        Pair.of(0, new VultureFlee()),
                        Pair.of(1, new RoostAtNight())),
                ImmutableSet.of(Pair.of(BrainInit.IS_DAYTIME, MemoryStatus.VALUE_ABSENT)));
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    public static void updateActivity(VultureEntity vulture) {
        vulture.getBrain().setActiveActivityToFirstValid(ImmutableList.of(BrainInit.NIGHT, Activity.IDLE));
    }
}
