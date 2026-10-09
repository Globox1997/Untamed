package net.untamed.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import net.untamed.entity.VultureEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class CarcassTracker {

    private static final long CARCASS_LIFETIME = 3600L;
    private static final int MIN_BITES = 2;
    private static final int MAX_BITES = 12;
    private static final int MAX_CARCASSES_PER_LEVEL = 64;

    private static final Map<ServerLevel, List<Carcass>> CARCASSES = new WeakHashMap<>();

    private CarcassTracker() {}

    public static final class Carcass {
        private final BlockPos pos;
        private final long expireTime;
        private int bites;

        private Carcass(BlockPos pos, long expireTime, int bites) {
            this.pos = pos;
            this.expireTime = expireTime;
            this.bites = bites;
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public boolean isValid(ServerLevel level) {
            return this.bites > 0 && level.getGameTime() < this.expireTime;
        }

        public boolean bite() {
            if (this.bites <= 0) {
                return false;
            }
            this.bites--;
            return true;
        }
    }

    public static void onDeath(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !(entity instanceof Animal) || entity instanceof VultureEntity) {
            return;
        }
        int bites = Mth.clamp(Mth.ceil(entity.getBbWidth() * entity.getBbHeight() * 4.0F), MIN_BITES, MAX_BITES);
        if (entity.isBaby()) {
            bites = Math.max(MIN_BITES, bites / 2);
        }
        List<Carcass> list = CARCASSES.computeIfAbsent(level, key -> new ArrayList<>());
        prune(level, list);
        if (list.size() >= MAX_CARCASSES_PER_LEVEL) {
            list.remove(0);
        }
        list.add(new Carcass(entity.blockPosition(), level.getGameTime() + CARCASS_LIFETIME, bites));
    }

    @Nullable
    public static Carcass findNearest(ServerLevel level, Vec3 origin, double horizontalRange) {
        List<Carcass> list = CARCASSES.get(level);
        if (list == null) {
            return null;
        }
        prune(level, list);
        Carcass best = null;
        double bestDistSqr = horizontalRange * horizontalRange;
        for (Carcass carcass : list) {
            double dx = carcass.pos.getX() + 0.5D - origin.x;
            double dz = carcass.pos.getZ() + 0.5D - origin.z;
            double distSqr = dx * dx + dz * dz;
            if (distSqr < bestDistSqr) {
                bestDistSqr = distSqr;
                best = carcass;
            }
        }
        return best;
    }

    private static void prune(ServerLevel level, List<Carcass> list) {
        list.removeIf(carcass -> !carcass.isValid(level));
    }
}
