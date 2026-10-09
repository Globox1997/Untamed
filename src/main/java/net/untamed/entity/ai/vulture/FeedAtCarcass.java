package net.untamed.entity.ai.vulture;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.VultureEntity;
import net.untamed.entity.ai.CarcassTracker;
import net.untamed.init.SoundInit;
import net.untamed.init.TagInit;

import java.util.List;

public class FeedAtCarcass extends Behavior<VultureEntity> {

    private static final double EAT_RANGE = 2.0D;
    private static final double SQUABBLE_RANGE = 1.6D;
    private static final int MIN_BITE_INTERVAL = 40;
    private static final int BITE_INTERVAL_VARIATION = 20;
    private static final int SQUABBLE_CHANCE = 80;
    private static final int SQUABBLE_TICKS = 20;
    private static final int REPATH_TICKS = 20;
    private static final float WALK_SPEED = 1.0F;
    private static final double SQUABBLE_PUSH = 0.4D;
    private static final int MAX_FAILED_PATHS = 3;

    private long nextBiteTime;
    private long squabbleUntil;
    private long nextRepathTime;
    private int failedPaths;

    public FeedAtCarcass() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), 1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity vulture) {
        return this.canFeed(level, vulture);
    }

    private boolean canFeed(ServerLevel level, VultureEntity vulture) {
        CarcassTracker.Carcass carcass = vulture.getCarcass();
        return carcass != null && carcass.isValid(level) && vulture.getFlightState() == VultureEntity.FLIGHT_GROUND && !vulture.isInWater() && !vulture.isBaby();
    }

    @Override
    protected void start(ServerLevel level, VultureEntity vulture, long time) {
        this.nextBiteTime = time + MIN_BITE_INTERVAL;
        this.squabbleUntil = 0L;
        this.nextRepathTime = 0L;
        this.failedPaths = 0;
    }

    @Override
    protected boolean canStillUse(ServerLevel level, VultureEntity vulture, long time) {
        return this.canFeed(level, vulture);
    }

    @Override
    protected void tick(ServerLevel level, VultureEntity vulture, long time) {
        CarcassTracker.Carcass carcass = vulture.getCarcass();
        if (carcass == null) {
            return;
        }
        Vec3 center = Vec3.atBottomCenterOf(carcass.getPos());
        if (vulture.position().distanceToSqr(center) > EAT_RANGE * EAT_RANGE) {
            vulture.setAction(VultureEntity.ACTION_NONE);
            if (time >= this.nextRepathTime || vulture.getNavigation().isDone()) {
                this.nextRepathTime = time + REPATH_TICKS;
                if (!vulture.getNavigation().moveTo(center.x, center.y, center.z, WALK_SPEED) && ++this.failedPaths > MAX_FAILED_PATHS) {
                    vulture.abandonCarcass();
                }
            }
            return;
        }
        vulture.getNavigation().stop();
        vulture.getLookControl().setLookAt(center.x, center.y, center.z);
        if (time < this.squabbleUntil) {
            vulture.setAction(VultureEntity.ACTION_SQUABBLING);
            return;
        }
        vulture.setAction(VultureEntity.ACTION_FEEDING);
        if (time >= this.nextBiteTime) {
            this.nextBiteTime = time + MIN_BITE_INTERVAL + vulture.getRandom().nextInt(BITE_INTERVAL_VARIATION);
            this.bite(level, vulture, carcass, center);
        }
        if (vulture.getRandom().nextInt(SQUABBLE_CHANCE) == 0) {
            this.squabble(level, vulture, time);
        }
    }

    private void bite(ServerLevel level, VultureEntity vulture, CarcassTracker.Carcass carcass, Vec3 center) {
        List<ItemEntity> food = level.getEntitiesOfClass(ItemEntity.class, new AABB(center, center).inflate(EAT_RANGE),
                item -> item.isAlive() && item.getItem().is(TagInit.VULTURE_FOOD));
        if (!food.isEmpty()) {
            food.get(0).getItem().shrink(1);
            vulture.heal(2.0F);
        } else if (carcass.bite()) {
            vulture.heal(1.0F);
        } else {
            return;
        }
        vulture.playSound(SoundEvents.GENERIC_EAT, 0.6F, 0.8F + vulture.getRandom().nextFloat() * 0.3F);
    }

    private void squabble(ServerLevel level, VultureEntity vulture, long time) {
        List<VultureEntity> rivals = level.getEntitiesOfClass(VultureEntity.class, vulture.getBoundingBox().inflate(SQUABBLE_RANGE),
                other -> other != vulture && other.isAlive() && other.getAction() == VultureEntity.ACTION_FEEDING);
        if (rivals.isEmpty()) {
            return;
        }
        VultureEntity rival = rivals.get(0);
        this.squabbleUntil = time + SQUABBLE_TICKS;
        vulture.setAction(VultureEntity.ACTION_SQUABBLING);
        vulture.getLookControl().setLookAt(rival);
        vulture.playSound(SoundInit.VULTURE_IDLE_EVENT, 1.0F, 1.3F + vulture.getRandom().nextFloat() * 0.2F);
        rival.knockback(SQUABBLE_PUSH, vulture.getX() - rival.getX(), vulture.getZ() - rival.getZ());
    }

    @Override
    protected void stop(ServerLevel level, VultureEntity vulture, long time) {
        vulture.setAction(VultureEntity.ACTION_NONE);
        vulture.finishFeeding();
    }
}
