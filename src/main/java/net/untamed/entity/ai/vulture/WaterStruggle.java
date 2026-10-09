package net.untamed.entity.ai.vulture;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.VultureEntity;
import net.untamed.entity.ai.LandUtils;

// It was primarily created to test a way of navigating difficult terrain while swimming.
// It doesn't have to be just for the vulture; it could also work for the capybara or another aquatic animal (like an octopus).
// But obviously, the code would need to be made more generic and avoid calls to entities, in addition to moving it to a upper package.
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

    private final VultureEntity vulture;

    private int diveTicks;
    private float escapeYaw;
    private int escapeRescanTimer;
    private double lastX, lastZ;
    private int progressCheckTimer;

    public WaterStruggle(VultureEntity vulture) { this.vulture = vulture; }

    public void tick() {
        Level level = this.vulture.level();
        Vec3 m = this.vulture.getDeltaMovement();

        if (this.diveTicks > 0) {
            this.diveTicks--;
            float diveDelta = Mth.wrapDegrees(this.escapeYaw - this.vulture.getYRot());
            this.vulture.setYRot(this.vulture.getYRot() + Mth.clamp(diveDelta, -15.0F, 15.0F));
            float yRotRad = this.vulture.getYRot() * ((float) Math.PI / 180F);
            this.vulture.setDeltaMovement(-Mth.sin(yRotRad) * WATER_DIVE_SPEED, -WATER_DIVE_DESCENT, Mth.cos(yRotRad) * WATER_DIVE_SPEED);
            return;
        }

        BlockPos headBlock = BlockPos.containing(this.vulture.getX(), this.vulture.getBoundingBox().maxY + 0.1, this.vulture.getZ());
        boolean ceilingNear = this.isSolid(level, headBlock);
        boolean coveredAbove = ceilingNear || this.isSolid(level, headBlock.above()) || this.isSolid(level, headBlock.above(2));

        if (--this.progressCheckTimer <= 0) {
            this.progressCheckTimer = WATER_PROGRESS_CHECK_TICKS;
            double dx = this.vulture.getX() - this.lastX;
            double dz = this.vulture.getZ() - this.lastZ;
            this.lastX = this.vulture.getX();
            this.lastZ = this.vulture.getZ();
            if (dx * dx + dz * dz < WATER_MIN_PROGRESS_SQR) {
                if (!this.isEyesUnderwater(level) && coveredAbove) {
                    this.escapeYaw = this.vulture.getYRot() + 180.0F;
                    this.diveTicks = WATER_DIVE_TICKS;
                    this.escapeRescanTimer = WATER_POST_DIVE_HOLD_TICKS;
                } else {
                    this.escapeYaw = this.vulture.getYRot() + (this.vulture.getRandom().nextBoolean() ? 90.0F : -90.0F);
                    this.escapeRescanTimer = WATER_ESCAPE_HOLD_TICKS;
                }
            }
        }

        if (this.vulture.horizontalCollision && !ceilingNear && !this.isEyesUnderwater(level) && this.canLandAhead(level)) {
            float yRotRad = this.vulture.getYRot() * ((float) Math.PI / 180F);
            Vec3 kick = m.add(-Mth.sin(yRotRad) * 0.2, 0.1, Mth.cos(yRotRad) * 0.2);
            double speed = Math.sqrt(kick.x * kick.x + kick.y * kick.y + kick.z * kick.z);
            if (speed > WATER_KICK_MAX_SPEED) kick = kick.scale(WATER_KICK_MAX_SPEED / speed);
            this.vulture.setDeltaMovement(kick);
            return;
        }

        if (this.escapeRescanTimer > 0) this.escapeRescanTimer--;
        else {
            float scan = LandUtils.nearestDryYaw(level, this.vulture, WATER_ESCAPE_SCAN_RANGE);
            this.escapeYaw = Float.isNaN(scan) ? this.vulture.getYRot() : scan;
            this.escapeRescanTimer = WATER_ESCAPE_HOLD_TICKS;
        }
        float delta = Mth.wrapDegrees(this.escapeYaw - this.vulture.getYRot());
        float turnRate = Math.abs(delta) > 30.0F ? 15.0F : 5.0F;
        this.vulture.setYRot(this.vulture.getYRot() + Mth.clamp(delta, -turnRate, turnRate));
        this.vulture.setYRot(this.vulture.getYRot() + (this.vulture.getRandom().nextFloat() - 0.5F) * 2.0F);

        float yRotRad = this.vulture.getYRot() * ((float) Math.PI / 180F);
        double sx = -Mth.sin(yRotRad) * WATER_SWIM_SPEED;
        double sz = Mth.cos(yRotRad) * WATER_SWIM_SPEED;
        if (ceilingNear) this.vulture.setDeltaMovement(sx, m.y * 0.5, sz);
        else if (this.isEyesUnderwater(level)) this.vulture.setDeltaMovement(sx, Math.min(m.y + 0.06, 0.05), sz);
        else this.vulture.setDeltaMovement(sx, m.y - WATER_GRAVITY, sz);

    }

    private boolean isSolid(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private boolean canLandAhead(Level level) {
        float yRotRad = this.vulture.getYRot() * ((float) Math.PI / 180F);
        BlockPos ahead = BlockPos.containing(
                this.vulture.getX() - Mth.sin(yRotRad) * 0.9,
                this.vulture.getBoundingBox().minY + 0.1,
                this.vulture.getZ() + Mth.cos(yRotRad) * 0.9);
        return this.isSolid(level, ahead) && !this.isSolid(level, ahead.above()) && !this.isSolid(level, ahead.above(2));
    }

    private boolean isEyesUnderwater(Level level) {
        return level.getFluidState(BlockPos.containing(this.vulture.getX(), this.vulture.getEyeY(), this.vulture.getZ())).is(FluidTags.WATER);
    }
}