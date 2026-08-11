package com.livingecology.ai;

import com.livingecology.data.SpeciesType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class BehaviorUtil {
    private BehaviorUtil() {}

    public static int countNearbyAllies(Mob mob, ServerLevel level, double radius) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return 0;
        return (int) level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(radius),
                other -> other != mob && other.isAlive() && species.matches(other)).size();
    }

    public static List<Mob> nearbySameSpecies(Mob mob, ServerLevel level, double radius) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return List.of();
        return level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(radius),
                other -> other != mob && other.isAlive() && species.matches(other));
    }

    public static int countSpecies(ServerLevel level, BlockPos center, SpeciesType species, double radius) {
        AABB box = new AABB(center).inflate(radius);
        return level.getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && species.matches(e)).size();
    }

    public static List<Mob> species(ServerLevel level, BlockPos center, SpeciesType species, double radius) {
        return level.getEntitiesOfClass(Mob.class, new AABB(center).inflate(radius),
                e -> e.isAlive() && species.matches(e));
    }

    public static Optional<Mob> nearestSpecies(Mob mob, ServerLevel level, SpeciesType species, double radius) {
        return level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(radius),
                        e -> e != mob && e.isAlive() && species.matches(e)).stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr));
    }

    public static Optional<LivingEntity> nearestLiving(Mob mob, ServerLevel level, double radius,
                                                       Predicate<LivingEntity> predicate) {
        return level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(radius),
                        e -> e != mob && e.isAlive() && predicate.test(e)).stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr));
    }

    public static Vec3 centroid(List<? extends LivingEntity> entities, Vec3 fallback) {
        if (entities.isEmpty()) return fallback;
        double x = 0, y = 0, z = 0;
        for (LivingEntity entity : entities) {
            x += entity.getX();
            y += entity.getY();
            z += entity.getZ();
        }
        return new Vec3(x / entities.size(), y / entities.size(), z / entities.size());
    }

    public static Vec3 safeTargetAwayFrom(Mob mob, ServerLevel level, Vec3 threat, double distance, Vec3 socialBias) {
        Vec3 away = mob.position().subtract(threat);
        if (away.lengthSqr() < 0.01D) away = new Vec3(1, 0, 0);
        away = new Vec3(away.x, 0, away.z).normalize();
        Vec3 bias = socialBias == null ? Vec3.ZERO : socialBias.subtract(mob.position()).scale(0.20D);
        Vec3 desired = away.scale(distance).add(bias);

        ArrayList<Vec3> candidates = new ArrayList<>();
        candidates.add(mob.position().add(desired));
        for (int i = 1; i <= 6; i++) {
            double angle = (Math.PI / 7.0D) * i;
            candidates.add(mob.position().add(rotateY(desired, angle)));
            candidates.add(mob.position().add(rotateY(desired, -angle)));
        }

        Vec3 best = mob.position();
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Vec3 candidate : candidates) {
            BlockPos pos = BlockPos.containing(candidate.x, mob.getY(), candidate.z);
            if (!isSafeLand(level, pos)) continue;
            double score = candidate.distanceToSqr(threat);
            if (socialBias != null) score -= candidate.distanceToSqr(socialBias) * 0.12D;
            if (score > bestScore) {
                bestScore = score;
                best = new Vec3(candidate.x, pos.getY(), candidate.z);
            }
        }
        return best;
    }

    private static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    public static boolean isSafeLand(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        if (isHazard(feet) || isHazard(below)) return false;
        if (!feet.getCollisionShape(level, pos).isEmpty() && !feet.isAir()) return false;
        if (below.isAir()) {
            int air = 0;
            for (int i = 1; i <= 4; i++) {
                if (level.getBlockState(pos.below(i)).isAir()) air++;
                else break;
            }
            if (air >= 3) return false;
        }
        return true;
    }

    public static boolean isHazard(BlockState state) {
        return state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS);
    }

    /** Prevents our own navigation orders from sending land animals into obvious lethal terrain. */
    public static boolean guardAgainstObviousLandHazard(Mob mob, ServerLevel level) {
        Vec3 direction = mob.getDeltaMovement();
        if (direction.horizontalDistanceSqr() < 0.0025D) direction = mob.getLookAngle();
        direction = new Vec3(direction.x, 0, direction.z);
        if (direction.lengthSqr() < 0.001D) return false;
        direction = direction.normalize();
        BlockPos ahead = BlockPos.containing(mob.position().add(direction.scale(1.7D)));
        if (isSafeLand(level, ahead)) return false;

        mob.getNavigation().stop();
        Vec3 reverse = mob.position().subtract(direction.scale(3.5D));
        BlockPos reversePos = BlockPos.containing(reverse);
        if (isSafeLand(level, reversePos)) {
            mob.getNavigation().moveTo(reverse.x, reversePos.getY(), reverse.z, 1.05D);
        }
        return true;
    }

    public static Optional<BlockPos> nearbyFire(Mob mob, ServerLevel level, int horizontalRadius, int verticalRadius) {
        BlockPos origin = mob.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(Blocks.FIRE) && !state.is(Blocks.SOUL_FIRE) && !state.is(Blocks.LAVA)) continue;
                    double distance = pos.distSqr(origin);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.immutable();
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    public static double perceptionRange(Mob mob, int perception) {
        return Mth.clamp(6.0D + perception * 0.18D, 6.0D, 26.0D);
    }
}
