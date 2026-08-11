package com.livingecology.ai;

import com.livingecology.data.AttributeType;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import com.livingecology.territory.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WolfBehavior {
    private static final int[][] LOCAL_PATROL_DIRECTIONS = {
            {1, 0}, {0, 1}, {-1, 0}, {0, -1},
            {1, 1}, {-1, 1}, {-1, -1}, {1, -1}
    };
    private static final int[] LOCAL_PATROL_DISTANCES = {4, 6, 8, 3};

    private WolfBehavior() {}

    public static void tick(Mob raw, ServerLevel level) {
        if (!(raw instanceof Wolf wolf)) return;
        if (wolf.isTame()) {
            tickTamed(wolf, level);
            return;
        }

        TerritoryRecord territory = TerritoryManager.ensureTerritory(wolf, level);
        TerritoryContext context = TerritoryManager.contextForMob(wolf, level);
        int adaptation = MobMindData.calculateAdaptation(wolf, level);
        int perception = MobMindData.getAttribute(wolf, AttributeType.PERCEPTION);
        double range = BehaviorUtil.perceptionRange(wolf, perception);

        if (BehaviorUtil.guardAgainstObviousLandHazard(wolf, level)) {
            MobMindData.addState(wolf, StateType.STRESS, 1);
            MobMindData.setResting(wolf, false);
            return;
        }

        Optional<BlockPos> fire = BehaviorUtil.nearbyFire(wolf, level, 2, 1);
        if (fire.isPresent()) {
            MobMindData.addState(wolf, StateType.FEAR, 2);
            Vec3 away = BehaviorUtil.safeTargetAwayFrom(wolf, level, Vec3.atCenterOf(fire.get()), 10.0D,
                    territory == null ? null : Vec3.atCenterOf(territory.core()));
            wolf.setTarget(null);
            wolf.getNavigation().moveTo(away.x, away.y, away.z, 1.25D);
            MobMindData.setResting(wolf, false);
            return;
        }

        List<Mob> pack = BehaviorUtil.nearbySameSpecies(wolf, level, 20.0D);
        if (wolf.getHealth() / wolf.getMaxHealth() < 0.34F || MobMindData.getState(wolf, StateType.PAIN) >= 4) {
            wolf.setTarget(null);
            Vec3 packCenter = BehaviorUtil.centroid(new ArrayList<>(pack), territory == null
                    ? wolf.position() : Vec3.atCenterOf(territory.core()));
            wolf.getNavigation().moveTo(packCenter.x, packCenter.y, packCenter.z, 1.25D);
            MobMindData.addState(wolf, StateType.FEAR, 1);
            MobMindData.setResting(wolf, false);
            return;
        }

        LivingEntity target = wolf.getTarget();
        if (!BehaviorUtil.isValidCombatTarget(target)) target = null;

        // High sociability: copy a meaningful target from a packmate, but never Creative/Spectator players.
        if (target == null && MobMindData.getAttribute(wolf, AttributeType.SOCIABILITY) >= 75) {
            for (Mob mate : pack) {
                LivingEntity shared = mate.getTarget();
                if (BehaviorUtil.isValidCombatTarget(shared) && wolf.distanceTo(shared) <= range * 1.3D) {
                    target = shared;
                    break;
                }
            }
        }

        // Natural aggressive war. A nearby Zombie can supersede vanilla wandering/player fixation when the
        // pack has a reason to engage it.
        Optional<Mob> zombie = BehaviorUtil.nearestSupported(wolf, level, range, WolfBehavior::isZombieFamily);
        if (zombie.isPresent()) {
            Mob z = zombie.get();
            TerritoryContext zContext = TerritoryManager.contextAt(level, z.blockPosition(), MobMindData.territoryId(wolf));
            boolean homeDefense = context.ownZone() == TerritoryZone.INNER || context.ownZone() == TerritoryZone.CORE;
            boolean packConfidence = pack.size() >= 1 || adaptation >= 70;
            boolean remembered = MobMindData.resolveThreat(wolf, level).filter(t -> t.getUUID().equals(z.getUUID())).isPresent();
            if (homeDefense || packConfidence || remembered || zContext.contested()) target = z;
        }

        // Wolf/Spider is a regulated border relation. Only deep intrusion or high tension escalates to combat.
        Optional<Mob> spider = BehaviorUtil.nearestSupported(wolf, level, range,
                type -> type == SpeciesType.SPIDER || type == SpeciesType.CAVE_SPIDER);
        if (spider.isPresent()) {
            Mob s = spider.get();
            TerritoryZone spiderInWolfHome = territory == null ? TerritoryZone.OUTSIDE : TerritoryManager.zoneFor(territory, s.blockPosition());
            if (spiderInWolfHome == TerritoryZone.CORE || spiderInWolfHome == TerritoryZone.INNER
                    || context.tension() >= 70) {
                target = s;
            }
        }

        if (context.noMansLand() && target == null && territory != null) {
            wolf.getNavigation().moveTo(territory.core().getX() + 0.5D, territory.core().getY(),
                    territory.core().getZ() + 0.5D, 1.0D);
            MobMindData.setResting(wolf, false);
            MobMindData.scheduleNextPatrol(wolf, level, 80, 150);
            return;
        }

        if (target != null) {
            wolf.setTarget(target);
            MobMindData.setResting(wolf, false);
            MobMindData.rememberThreat(wolf, target, 5, level);
            boolean stalled = MobMindData.movementStalled(wolf, level, 30, 0.65D);
            if (adaptation >= 70 && pack.size() >= 1 && wolf.distanceTo(target) > 4.0F) {
                double angle = (wolf.getId() * 1.61803398875D) % (Math.PI * 2.0D);
                double flank = 2.5D + Math.min(3.0D, pack.size() * 0.4D);
                Vec3 p = target.position().add(Math.cos(angle) * flank, 0.0D, Math.sin(angle) * flank);
                if (stalled) wolf.getNavigation().stop();
                if (stalled || wolf.getNavigation().isDone()) wolf.getNavigation().moveTo(p.x, p.y, p.z, 1.20D);
            } else if (stalled && wolf.distanceToSqr(target) > 6.25D) {
                wolf.getNavigation().stop();
                wolf.getNavigation().moveTo(target, 1.15D);
            }
            return;
        }

        if (territory != null && context.ownZone() == TerritoryZone.OUTSIDE) {
            wolf.getNavigation().moveTo(territory.core().getX() + 0.5D, territory.core().getY(),
                    territory.core().getZ() + 0.5D, 1.05D);
            MobMindData.setResting(wolf, false);
            MobMindData.scheduleNextPatrol(wolf, level, 80, 150);
            return;
        }

        if (pack.size() >= 2) {
            Vec3 packCenter = BehaviorUtil.centroid(new ArrayList<>(pack), wolf.position());
            if (wolf.position().distanceToSqr(packCenter) > 12.0D * 12.0D) {
                wolf.getNavigation().moveTo(packCenter.x, packCenter.y, packCenter.z, 1.0D);
                MobMindData.setResting(wolf, false);
                MobMindData.scheduleNextPatrol(wolf, level, 80, 150);
                return;
            }
        }

        // Explicit territorial patrol. Random territorial sampling gives natural routes, while the local
        // fallback prevents a due patrol from being lost just because the sampled point is unreachable.
        if (wolf.getNavigation().isDone() && MobMindData.patrolDue(wolf, level)) {
            if (orderPatrol(wolf, level, territory)) {
                MobMindData.setResting(wolf, false);
                MobMindData.scheduleNextPatrol(wolf, level, 100, 220);
                return;
            }
            MobMindData.scheduleNextPatrol(wolf, level, 40, 90);
        }

        // Fragmented rest happens only at night and only after active navigation has ended.
        long dayTime = Math.floorMod(level.getDayTime(), 24000L);
        boolean night = dayTime >= 13000L && dayTime <= 22500L;
        long restPhase = Math.floorMod(level.getGameTime() + (wolf.getUUID().getLeastSignificantBits() & Long.MAX_VALUE), 1200L);
        boolean canRest = night && restPhase < 120L && wolf.getNavigation().isDone()
                && MobMindData.getState(wolf, StateType.FEAR) == 0
                && MobMindData.getState(wolf, StateType.STRESS) <= 1
                && context.ownZone() != TerritoryZone.OUTSIDE;
        MobMindData.setResting(wolf, canRest);
        if (canRest) wolf.getNavigation().stop();
    }

    private static boolean orderPatrol(Wolf wolf, ServerLevel level, TerritoryRecord territory) {
        Vec3 center = territory == null ? wolf.position() : Vec3.atCenterOf(territory.core());
        double radius = territory == null ? 12.0D : Math.min(30.0D, territory.radiusChunks() * 16.0D * 0.55D);
        Optional<Vec3> patrol = BehaviorUtil.safePatrolPoint(wolf, level, center, radius);
        if (patrol.isPresent()) {
            Vec3 p = patrol.get();
            wolf.getNavigation().moveTo(p.x, p.y, p.z, 0.95D);
            if (!wolf.getNavigation().isDone()) return true;
        }

        // Compact arenas, caves and fragmented terrain can make a broad territorial sample unreachable.
        // Search locally without loading chunks and only accept orders vanilla pathfinding actually keeps.
        BlockPos origin = wolf.blockPosition();
        int directionStart = Math.floorMod(wolf.getId(), LOCAL_PATROL_DIRECTIONS.length);
        int[] yOffsets = {0, 1, -1, 2, -2};
        for (int distance : LOCAL_PATROL_DISTANCES) {
            for (int i = 0; i < LOCAL_PATROL_DIRECTIONS.length; i++) {
                int[] direction = LOCAL_PATROL_DIRECTIONS[(directionStart + i) % LOCAL_PATROL_DIRECTIONS.length];
                int x = origin.getX() + direction[0] * distance;
                int z = origin.getZ() + direction[1] * distance;
                for (int yOffset : yOffsets) {
                    BlockPos candidate = new BlockPos(x, origin.getY() + yOffset, z);
                    if (!level.hasChunkAt(candidate) || !BehaviorUtil.isSafeLand(level, candidate)) continue;
                    wolf.getNavigation().moveTo(candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D, 0.95D);
                    if (!wolf.getNavigation().isDone()) return true;
                }
            }
        }
        return false;
    }

    private static boolean isZombieFamily(SpeciesType type) {
        return type == SpeciesType.ZOMBIE || type == SpeciesType.ZOMBIE_VILLAGER || type == SpeciesType.HUSK
                || type == SpeciesType.DROWNED || type == SpeciesType.GIANT;
    }

    private static void tickTamed(Wolf wolf, ServerLevel level) {
        BehaviorUtil.sanitizeCombatTarget(wolf, level);
        if (BehaviorUtil.guardAgainstObviousLandHazard(wolf, level)) return;
        boolean hurt = wolf.getHealth() / wolf.getMaxHealth() < 0.30F || MobMindData.getState(wolf, StateType.PAIN) >= 4;
        if (hurt) {
            wolf.setTarget(null);
            LivingEntity owner = wolf.getOwner();
            if (owner != null && owner.isAlive() && wolf.distanceToSqr(owner) > 9.0D) {
                wolf.getNavigation().moveTo(owner, 1.20D);
            } else {
                wolf.getNavigation().stop();
            }
            MobMindData.setResting(wolf, false);
        }
    }
}
