package net.untamed.entity.ai.capybara;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.CapybaraEntity;
import net.untamed.entity.ai.WaterUtils;

public class OccasionalDive extends Behavior<CapybaraEntity> {

    private static final int DIVE_START_CHANCE = 400;
    private static final int MIN_TARGET_DIST_SQR = 4;
    private static final int RETARGET_COOLDOWN_TICKS = 30;
    private static final int DIVE_MIN_RADIUS = 2;
    private static final int DIVE_MAX_RADIUS = 8;
    private static final double MIN_MOVE_SQR = 1.0E-4;

    private final float speedModifier;
    private long nextRetargetTime;
    private boolean reroutedOnRiverbed;

    public OccasionalDive(float speedModifier) {
        super(ImmutableMap.of(
                MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.TEMPTING_PLAYER, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT), 200, 400);
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CapybaraEntity capybara) {
        Vec3 delta = capybara.getDeltaMovement();
        boolean movingHorizontally = delta.x * delta.x + delta.z * delta.z > MIN_MOVE_SQR;
        return capybara.isFloating()
                && !capybara.isDiving()
                && movingHorizontally
                && capybara.getRandom().nextInt(DIVE_START_CHANCE) == 0
                && WaterUtils.getWaterDepth(level, capybara) >= WaterUtils.MIN_OPERABLE_WATER_DEPTH;
    }

    @Override
    protected void start(ServerLevel level, CapybaraEntity capybara, long time) {
        capybara.setDiving(true);
        capybara.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.reroutedOnRiverbed = false;
        this.moveAlongRiverbed(level, capybara);
    }

    @Override
    protected void tick(ServerLevel level, CapybaraEntity capybara, long time) {
        if (capybara.isRiverbed() && !this.reroutedOnRiverbed) {
            this.reroutedOnRiverbed = true;
            capybara.getNavigation().stop();
            this.moveAlongRiverbed(level, capybara);
        }
        if (time >= this.nextRetargetTime && capybara.getNavigation().isDone()) {
            this.nextRetargetTime = time + RETARGET_COOLDOWN_TICKS;
            this.moveAlongRiverbed(level, capybara);
        }
    }

    private void moveAlongRiverbed(ServerLevel level, CapybaraEntity capybara) {
        float angle = capybara.getRandom().nextFloat() * Mth.TWO_PI;
        int radius = DIVE_MIN_RADIUS + capybara.getRandom().nextInt(DIVE_MAX_RADIUS - DIVE_MIN_RADIUS);
        int x = capybara.blockPosition().getX() + Mth.floor(Mth.cos(angle) * radius);
        int z = capybara.blockPosition().getZ() + Mth.floor(Mth.sin(angle) * radius);
        BlockPos target = WaterUtils.riverbedAt(level, x, z);
        if (target != null && capybara.distanceToSqr(Vec3.atBottomCenterOf(target)) > MIN_TARGET_DIST_SQR)
            capybara.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, this.speedModifier);
    }

    // Sin uso. Still commented if another mobs requires. Maybe Octopus?
    //    private void retargetBottom(ServerLevel level, CapybaraEntity capybara) {
    //        float yRotRad = capybara.getYRot() * ((float) Math.PI / 180F);
    //        Vec3 ahead = capybara.position().add(-Mth.sin(yRotRad) * 6.0, 0.0, Mth.cos(yRotRad) * 6.0);
    //        for (int attempt = 0; attempt < 4; attempt++) {
    //            Vec3 candidate = attempt == 0
    //                    ? LandRandomPos.getPosTowards(capybara, 8, 4, ahead)
    //                    : LandRandomPos.getPos(capybara, 8, 4);
    //            if (candidate == null) return;
    //            BlockPos bottom = WaterUtils.findBottomAt(level,
    //                    BlockPos.containing(candidate.x, capybara.blockPosition().getY(), candidate.z));
    //            if (bottom != null && capybara.distanceToSqr(Vec3.atBottomCenterOf(bottom)) > 2.0) {
    //                capybara.getNavigation().moveTo(bottom.getX() + 0.5, bottom.getY(), bottom.getZ() + 0.5, this.speedModifier);
    //                return;
    //            }
    //        }
    //    }

    @Override
    protected boolean canStillUse(ServerLevel level, CapybaraEntity capybara, long time) {
        return capybara.isDiving() && !capybara.isPanicking();
    }

    @Override
    protected void stop(ServerLevel level, CapybaraEntity capybara, long time) {
        capybara.setDiving(false);
        capybara.getNavigation().stop();
        this.reroutedOnRiverbed = false;
        capybara.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        capybara.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }
}