package net.untamed.entity.ai.vulture;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.ai.LandUtils;

public class WaterStruggle {

    private static final int WATER_ESCAPE_SCAN_RANGE = 16;
    private static final double WATER_SWIM_SPEED = 0.08;
    private static final int WATER_POST_DIVE_HOLD_TICKS = 80;
    private static final double WATER_GRAVITY = 0.005;

    private static final int WATER_PROGRESS_CHECK_TICKS = 40;
    private static final double WATER_MIN_PROGRESS_SQR = 0.25;
    private static final double WATER_KICK_MAX_SPEED = 0.3;
    private static final int WATER_ESCAPE_HOLD_TICKS = 60;
    private static final int WATER_DIVE_TICKS = 40;
    private static final double WATER_DIVE_SPEED = 0.1;
    private static final double WATER_DIVE_DESCENT = 0.05;

    private final Mob mob;

    private int diveTicks;
    private float escapeYaw;
    private int escapeRescanTimer;
    private double lastX, lastZ;
    private int progressCheckTimer;

    public WaterStruggle(Mob mob) { this.mob = mob; }

    public void tick() {
        Level level = this.mob.level();
        Vec3 m = this.mob.getDeltaMovement();

        if (this.diveTicks > 0) {
            this.diveTicks--;
            float diveDelta = Mth.wrapDegrees(this.escapeYaw - this.mob.getYRot());
            this.mob.setYRot(this.mob.getYRot() + Mth.clamp(diveDelta, -15.0F, 15.0F));
            float yRotRad = this.mob.getYRot() * ((float) Math.PI / 180F);
            this.mob.setDeltaMovement(-Mth.sin(yRotRad) * WATER_DIVE_SPEED, -WATER_DIVE_DESCENT, Mth.cos(yRotRad) * WATER_DIVE_SPEED);
            return;
        }

        BlockPos headBlock = BlockPos.containing(this.mob.getX(), this.mob.getBoundingBox().maxY + 0.1, this.mob.getZ());
        boolean ceilingNear = this.isSolid(level, headBlock);
        boolean coveredAbove = ceilingNear || this.isSolid(level, headBlock.above()) || this.isSolid(level, headBlock.above(2));

        if (--this.progressCheckTimer <= 0) {
            this.progressCheckTimer = WATER_PROGRESS_CHECK_TICKS;
            double dx = this.mob.getX() - this.lastX;
            double dz = this.mob.getZ() - this.lastZ;
            this.lastX = this.mob.getX();
            this.lastZ = this.mob.getZ();
            if (dx * dx + dz * dz < WATER_MIN_PROGRESS_SQR) {
                if (!this.isEyesUnderwater(level) && coveredAbove) {
                    this.escapeYaw = this.mob.getYRot() + 180.0F;
                    this.diveTicks = WATER_DIVE_TICKS;
                    this.escapeRescanTimer = WATER_POST_DIVE_HOLD_TICKS;
                } else {
                    this.escapeYaw = this.mob.getYRot() + (this.mob.getRandom().nextBoolean() ? 90.0F : -90.0F);
                    this.escapeRescanTimer = WATER_ESCAPE_HOLD_TICKS;
                }
            }
        }

        if (this.mob.horizontalCollision && !ceilingNear && !this.isEyesUnderwater(level) && this.canLandAhead(level)) {
            float yRotRad = this.mob.getYRot() * ((float) Math.PI / 180F);
            Vec3 kick = m.add(-Mth.sin(yRotRad) * 0.2, 0.1, Mth.cos(yRotRad) * 0.2);
            double speed = Math.sqrt(kick.x * kick.x + kick.y * kick.y + kick.z * kick.z);
            if (speed > WATER_KICK_MAX_SPEED) kick = kick.scale(WATER_KICK_MAX_SPEED / speed);
            this.mob.setDeltaMovement(kick);
            return;
        }

        if (this.escapeRescanTimer > 0) this.escapeRescanTimer--;
        else {
            float scan = LandUtils.nearestDryYaw(level, this.mob, WATER_ESCAPE_SCAN_RANGE);
            this.escapeYaw = Float.isNaN(scan) ? this.mob.getYRot() : scan;
            this.escapeRescanTimer = WATER_ESCAPE_HOLD_TICKS;
        }
        float delta = Mth.wrapDegrees(this.escapeYaw - this.mob.getYRot());
        float turnRate = Math.abs(delta) > 30.0F ? 15.0F : 5.0F;
        this.mob.setYRot(this.mob.getYRot() + Mth.clamp(delta, -turnRate, turnRate));
        this.mob.setYRot(this.mob.getYRot() + (this.mob.getRandom().nextFloat() - 0.5F) * 2.0F);

        float yRotRad = this.mob.getYRot() * ((float) Math.PI / 180F);
        double sx = -Mth.sin(yRotRad) * WATER_SWIM_SPEED;
        double sz = Mth.cos(yRotRad) * WATER_SWIM_SPEED;
        if (ceilingNear) this.mob.setDeltaMovement(sx, m.y * 0.5, sz);
        else if (this.isEyesUnderwater(level)) this.mob.setDeltaMovement(sx, Math.min(m.y + 0.06, 0.05), sz);
        else this.mob.setDeltaMovement(sx, m.y - WATER_GRAVITY, sz);

    }

    private boolean isSolid(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private boolean canLandAhead(Level level) {
        float yRotRad = this.mob.getYRot() * ((float) Math.PI / 180F);
        BlockPos ahead = BlockPos.containing(
                this.mob.getX() - Mth.sin(yRotRad) * 0.9,
                this.mob.getBoundingBox().minY + 0.1,
                this.mob.getZ() + Mth.cos(yRotRad) * 0.9);
        return this.isSolid(level, ahead) && !this.isSolid(level, ahead.above()) && !this.isSolid(level, ahead.above(2));
    }

    private boolean isEyesUnderwater(Level level) {
        return level.getFluidState(BlockPos.containing(this.mob.getX(), this.mob.getEyeY(), this.mob.getZ())).is(FluidTags.WATER);
    }
}