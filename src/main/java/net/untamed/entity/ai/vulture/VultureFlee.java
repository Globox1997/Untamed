package net.untamed.entity.ai.vulture;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.VultureEntity;
import org.jetbrains.annotations.Nullable;

public class VultureFlee extends Behavior<VultureEntity> {

    private static final float WALK_SPEED = 1.4F;
    private static final int FLEE_DISTANCE = 10;

    @Nullable
    private LivingEntity threat;

    public VultureFlee() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED), 40, 80);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, VultureEntity vulture) {
        if (vulture.getFlightState() != VultureEntity.FLIGHT_GROUND || vulture.isInWater()) {
            return false;
        }
        this.threat = vulture.findThreat();
        return this.threat != null;
    }

    @Override
    protected void start(ServerLevel level, VultureEntity vulture, long time) {
        if (this.threat == null) {
            return;
        }
        vulture.releasePerch();
        vulture.setAction(VultureEntity.ACTION_NONE);
        if (vulture.takeOffAwayFrom(this.threat.position())) {
            return;
        }
        Vec3 away = LandRandomPos.getPosAway(vulture, FLEE_DISTANCE, 7, this.threat.position());
        if (away != null) {
            vulture.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(away, WALK_SPEED, 0));
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, VultureEntity vulture, long time) {
        return vulture.getFlightState() == VultureEntity.FLIGHT_GROUND && vulture.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void stop(ServerLevel level, VultureEntity vulture, long time) {
        this.threat = null;
    }
}
