package net.untamed.entity.ai;

import com.mojang.datafixers.util.Unit;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.untamed.init.BrainInit;

public final class SetPlayerLookTargetSometimes {

    private static final int LOOK_DURATION_TICKS = 40;

    private SetPlayerLookTargetSometimes() {
    }

    public static <E extends LivingEntity> OneShot<E> create(float maxDistance, UniformInt timeBetweenLooks) {
        final float maxDistanceSqr = maxDistance * maxDistance;
        return BehaviorBuilder.create(instance -> instance.group(
                instance.present(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES),
                instance.absent(MemoryModuleType.LOOK_TARGET),
                instance.absent(BrainInit.LOOK_AT_PLAYER_COOLDOWN)
        ).apply(instance, (visibleAccessor, lookAccessor, cooldownAccessor) -> (level, entity, time) -> {
            Player player = instance.get(visibleAccessor)
                    .findClosest(livingEntity -> livingEntity instanceof Player)
                    .map(Player.class::cast)
                    .orElse(null);
            if (player == null || entity.distanceToSqr(player) > maxDistanceSqr) return false;
            entity.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new EntityTracker(player, true), LOOK_DURATION_TICKS);
            entity.getBrain().setMemoryWithExpiry(BrainInit.LOOK_AT_PLAYER_COOLDOWN, Unit.INSTANCE, LOOK_DURATION_TICKS + timeBetweenLooks.sample(level.getRandom()));
            return true;
        }));
    }
}
