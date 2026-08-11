package com.livingecology.ai;

import com.livingecology.data.MobMindData;
import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.FluidTags;
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

    /**
     * Central combat-target gate for the prototype. Creative and spectator players may observe
     * ecology without becoming targets, even if a mob remembered them while they were in another mode.
     */
    public static boolean isValidCombatTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) return false;
        if (entity instanceof Player player) return !player.isCreative() && !player.isSpectator();
        return true;
    }

    /** Clears stale vanilla/custom targets that became invalid after a gamemode change. */
    public static void sanitizeCombatTarget(Mob mob, ServerLevel level) {
        LivingEntity current = mob.getTarget();
        if (current != null && !isValidCombatTarget(current)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            MobMindData.clearThreatIfMatches(mob, current.getUUID());
        }
        MobMindData.clearInvalidPlayerThreat(mob, level);
    }

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

    public static Optional<Mob> nearestSupported(Mob mob, ServerLevel level, double radius, Predicate<SpeciesType> speciesFilter) {
        return level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(radius), e -> {
                    if (e == mob || !e.isAlive()) return false;
                    SpeciesType type = SpeciesType.from(e).orElse(null);
                    return type != null && speciesFilter.test(type);
                }).stream().min(Comparator.comparingDouble(mob::distanceToSqr));
    }

    public static Optional<LivingEntity> nearestLiving(Mob mob, ServerLevel level, double radius,
                                                       Predicate<LivingEntity> predicate) {
        return level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(radius),
                        e -> e != mob && isValidCombatTarget(e) && predicate.test(e)).stream()
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

    /**
     * Picks a loaded, walkable point without forcing chunks. Used for explicit patrol/drift so mobs do not
     * depend entirely on vanilla RandomStrollGoal while the adaptive layer is active.
     */
    public static Optional<Vec3> safePatrolPoint(Mob mob, ServerLevel level, Vec3 center, double radius) {
        Vec3 current = mob.position();
        Vec3 biasedCenter = current.scale(0.70D).add(center.scale(0.30D));
        double safeRadius = Mth.clamp(radius, 5.0D, 32.0D);
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = 3.0D + mob.getRandom().nextDouble() * Math.max(2.0D, safeRadius - 3.0D);
            int x = Mth.floor(biasedCenter.x + Math.cos(angle) * distance);
            int z = Mth.floor(biasedCenter.z + Math.sin(angle) * distance);
            int baseY = mob.blockPosition().getY();
            for (int step = 0; step <= 4; step++) {
                int[] ys = step == 0 ? new int[]{baseY} : new int[]{baseY + step, baseY - step};
                for (int y : ys) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.hasChunkAt(pos)) continue;
                    if (isSafeLand(level, pos)) return Optional.of(new Vec3(x + 0.5D, y, z + 0.5D));
                }
            }
        }
        return Optional.empty();
    }


    /** Domain-aware patrol point. Never asks for or loads a missing chunk. */
    public static Optional<Vec3> safePatrolPoint(Mob mob, ServerLevel level, SpeciesProfile profile, Vec3 center, double radius) {
        MovementDomain domain = profile.movementDomain();
        if (domain == MovementDomain.LAND) return safePatrolPoint(mob, level, center, radius);
        if (domain == MovementDomain.STATIC) return Optional.empty();

        double safeRadius = Mth.clamp(radius, 4.0D, 28.0D);
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
            double distance = 3.0D + mob.getRandom().nextDouble() * Math.max(1.0D, safeRadius - 3.0D);
            int x = Mth.floor(center.x + Math.cos(angle) * distance);
            int z = Mth.floor(center.z + Math.sin(angle) * distance);
            int y = Mth.floor(mob.getY() + mob.getRandom().nextInt(9) - 4);
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.hasChunkAt(pos)) continue;

            if (domain == MovementDomain.WATER) {
                if (level.getFluidState(pos).is(FluidTags.WATER)) return Optional.of(Vec3.atCenterOf(pos));
            } else if (domain == MovementDomain.AMPHIBIOUS) {
                if (level.getFluidState(pos).is(FluidTags.WATER) || isSafeLand(level, pos))
                    return Optional.of(Vec3.atCenterOf(pos));
            } else if (domain == MovementDomain.LAVA) {
                if (level.getFluidState(pos).is(FluidTags.LAVA)) return Optional.of(Vec3.atCenterOf(pos));
            } else if (domain == MovementDomain.AIR) {
                BlockState here = level.getBlockState(pos);
                BlockState above = level.getBlockState(pos.above());
                if (here.getCollisionShape(level, pos).isEmpty() && above.getCollisionShape(level, pos.above()).isEmpty())
                    return Optional.of(Vec3.atCenterOf(pos));
            }
        }
        return Optional.empty();
    }

    public static Vec3 targetAwayForDomain(Mob mob, ServerLevel level, SpeciesProfile profile, Vec3 threat, double distance, Vec3 socialBias) {
        MovementDomain domain = profile.movementDomain();
        if (domain == MovementDomain.LAND) return safeTargetAwayFrom(mob, level, threat, distance, socialBias);
        if (domain == MovementDomain.STATIC) return mob.position();

        Vec3 away = mob.position().subtract(threat);
        if (away.lengthSqr() < 0.01D) away = new Vec3(1.0D, domain == MovementDomain.AIR ? 0.15D : 0.0D, 0.0D);
        if (domain != MovementDomain.AIR) away = new Vec3(away.x, 0.0D, away.z);
        if (away.lengthSqr() < 0.01D) away = new Vec3(1.0D, 0.0D, 0.0D);
        away = away.normalize();

        Vec3 social = socialBias == null ? Vec3.ZERO : socialBias.subtract(mob.position()).scale(0.12D);
        Vec3 desired = away.scale(distance).add(social);
        ArrayList<Vec3> candidates = new ArrayList<>();
        candidates.add(mob.position().add(desired));
        for (int i = 1; i <= 5; i++) {
            double angle = (Math.PI / 6.0D) * i;
            candidates.add(mob.position().add(rotateY(desired, angle)));
            candidates.add(mob.position().add(rotateY(desired, -angle)));
        }
        if (domain == MovementDomain.AIR) {
            candidates.add(mob.position().add(desired).add(0.0D, 3.0D, 0.0D));
            candidates.add(mob.position().add(desired).add(0.0D, -2.0D, 0.0D));
        } else if (domain == MovementDomain.WATER || domain == MovementDomain.LAVA || domain == MovementDomain.AMPHIBIOUS) {
            candidates.add(mob.position().add(desired).add(0.0D, 2.0D, 0.0D));
            candidates.add(mob.position().add(desired).add(0.0D, -2.0D, 0.0D));
        }

        Vec3 best = mob.position();
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Vec3 candidate : candidates) {
            BlockPos pos = BlockPos.containing(candidate);
            if (!level.hasChunkAt(pos) || !isValidForDomain(level, pos, domain)) continue;
            double score = candidate.distanceToSqr(threat);
            if (socialBias != null) score -= candidate.distanceToSqr(socialBias) * 0.08D;
            if (score > bestScore) {
                bestScore = score;
                best = Vec3.atCenterOf(pos);
            }
        }
        return best;
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
            if (!level.hasChunkAt(pos) || !isSafeLand(level, pos)) continue;
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
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        if (isHazard(feet) || isHazard(head) || isHazard(below)) return false;
        // LAND routing must never classify the surface of water/lava as walkable. Vanilla pathfinding
        // may choose to swim on its own, but ecological patrol/flee orders should not push land mobs in.
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()
                || !level.getFluidState(pos.below()).isEmpty()) return false;
        if (!feet.getCollisionShape(level, pos).isEmpty() && !feet.isAir()) return false;
        if (!head.getCollisionShape(level, pos.above()).isEmpty() && !head.isAir()) return false;
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

    /** Public domain gate for specialized controllers and objective movement tests. */
    public static boolean isValidForDomain(ServerLevel level, BlockPos pos, MovementDomain domain) {
        return switch (domain) {
            case LAND -> isSafeLand(level, pos);
            case WATER -> level.getFluidState(pos).is(FluidTags.WATER);
            case LAVA -> level.getFluidState(pos).is(FluidTags.LAVA);
            case AMPHIBIOUS -> level.getFluidState(pos).is(FluidTags.WATER) || isSafeLand(level, pos);
            case AIR -> level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                    && level.getFluidState(pos).isEmpty();
            case STATIC -> false;
        };
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
        if (!level.hasChunkAt(ahead) || isSafeLand(level, ahead)) return false;

        mob.getNavigation().stop();
        Vec3 reverse = mob.position().subtract(direction.scale(3.5D));
        BlockPos reversePos = BlockPos.containing(reverse);
        if (level.hasChunkAt(reversePos) && isSafeLand(level, reversePos)) {
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
                    if (!level.hasChunkAt(pos)) continue;
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
