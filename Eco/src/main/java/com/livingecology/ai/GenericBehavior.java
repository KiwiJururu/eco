package com.livingecology.ai;

import com.livingecology.data.*;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.EnvironmentSnapshot;
import com.livingecology.territory.TerritoryContext;
import com.livingecology.territory.RelationService;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import com.livingecology.territory.TerritoryZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Conservative behavior layer shared by species that do not yet need a dedicated controller.
 * It deliberately complements vanilla goals/brains rather than replacing them: orders are issued
 * only for high-value ecological decisions (danger, cohesion, territorial return and idle patrol).
 */
public final class GenericBehavior {
    private GenericBehavior() {}

    public static void tick(Mob mob, ServerLevel level, SpeciesProfile profile) {
        if ((mob instanceof TamableAnimal tame && tame.isTame())
                || (mob instanceof AbstractHorse horse && horse.isTamed())) {
            // Player-owned animals retain vanilla owner-follow/command/ride semantics. The adaptive
            // layer still supplies memory/states/debug data, but never steals their navigation.
            MobMindData.setTerritoryId(mob, 0L);
            MobMindData.setResting(mob, false);
            return;
        }

        TerritoryRecord territory = TerritoryManager.ensureTerritory(mob, level);
        TerritoryContext context = TerritoryManager.contextForMob(mob, level);
        EnvironmentSnapshot environment = EnvironmentManager.snapshot(level, mob.blockPosition(), profile.species());

        if (profile.movementDomain() == MovementDomain.LAND && BehaviorUtil.guardAgainstObviousLandHazard(mob, level)) {
            MobMindData.addState(mob, StateType.STRESS, 1);
            MobMindData.setResting(mob, false);
            return;
        }

        Optional<BlockPos> fire = fireThreat(mob, level, profile);
        if (fire.isPresent()) {
            MobMindData.rememberThreatPosition(mob, fire.get(), 16, level);
            MobMindData.addState(mob, StateType.STRESS, 1);
            if (isFearFamily(profile.behaviorFamily())) MobMindData.addState(mob, StateType.FEAR, 2);
            else MobMindData.addState(mob, StateType.RAGE, 1);

            if (isFearFamily(profile.behaviorFamily())) {
                Vec3 socialBias = sameSpeciesCenter(mob, level, profile, territory);
                Vec3 target = BehaviorUtil.targetAwayForDomain(mob, level, profile,
                        Vec3.atCenterOf(fire.get()), 8.0D, socialBias);
                move(mob, target, 1.10D);
                MobMindData.setResting(mob, false);
                return;
            }
        }

        if (environment.habitability() < 25) MobMindData.addState(mob, StateType.STRESS, 1);
        prioritizeEcologicalEnemy(mob, level, profile);

        switch (profile.behaviorFamily()) {
            case PASSIVE_HERD -> passiveHerd(mob, level, profile, territory, context);
            case PASSIVE_WANDERER, SPECIAL_MOUNT -> passiveWanderer(mob, level, profile, territory, context);
            case AQUATIC -> aquatic(mob, level, profile, territory, context);
            case PREDATOR -> predator(mob, level, profile, territory, context);
            case FLYING_PASSIVE -> flyingPassive(mob, level, profile, territory, context);
            case COLONY -> colony(mob, level, profile, territory, context);
            case ARTHROPOD -> arthropod(mob, level, profile, territory, context);
            case UNDEAD_HORDE -> undeadHorde(mob, level, profile, territory, context);
            case UNDEAD_COMBAT, HOSTILE_MELEE, HOSTILE_RANGED, FLYING_HOSTILE, EXPLOSIVE -> hostile(mob, level, profile, territory, context);
            case SOCIETY_PEACEFUL, SOCIETY_HOSTILE, GUARDIAN -> societyLowTouch(mob, level, profile, territory, context);
            case BOSS -> bossLowTouch(mob, level, profile, territory, context);
        }
    }

    private static void passiveHerd(Mob mob, ServerLevel level, SpeciesProfile profile,
                                    TerritoryRecord territory, TerritoryContext context) {
        LivingEntity threat = perceivedThreat(mob, level, profile);
        int fear = MobMindData.getState(mob, StateType.FEAR);
        List<Mob> herd = BehaviorUtil.nearbySameSpecies(mob, level, 8.0D + profile.midpoint(AttributeType.SOCIABILITY) * 0.09D);
        Vec3 herdCenter = sameSpeciesCenter(mob, level, profile, territory);

        if (threat != null || fear >= 2) {
            Vec3 source = threat != null ? threat.position()
                    : MobMindData.lastThreatPos(mob).map(Vec3::atCenterOf).orElse(mob.position().subtract(mob.getLookAngle()));
            Vec3 target = BehaviorUtil.targetAwayForDomain(mob, level, profile, source, 7.0D + fear, herdCenter);
            move(mob, target, 1.10D + fear * 0.04D);
            MobMindData.setResting(mob, false);
            return;
        }

        if (herd.size() >= 2 && mob.distanceToSqr(herdCenter) > 90.0D) {
            move(mob, herdCenter, 0.95D);
            return;
        }
        returnHomeOrPatrol(mob, level, profile, territory, context, 0.86D);
        updateRest(mob, level, profile, context, true);
    }

    private static void passiveWanderer(Mob mob, ServerLevel level, SpeciesProfile profile,
                                        TerritoryRecord territory, TerritoryContext context) {
        LivingEntity threat = perceivedThreat(mob, level, profile);
        if (threat != null && MobMindData.threatScore(mob) >= 12) {
            MobMindData.addState(mob, StateType.FEAR, 1);
            Vec3 target = BehaviorUtil.targetAwayForDomain(mob, level, profile, threat.position(), 7.0D,
                    territory == null ? null : Vec3.atCenterOf(territory.core()));
            move(mob, target, 1.05D);
            return;
        }
        returnHomeOrPatrol(mob, level, profile, territory, context, 0.82D);
        updateRest(mob, level, profile, context, true);
    }

    private static void aquatic(Mob mob, ServerLevel level, SpeciesProfile profile,
                                TerritoryRecord territory, TerritoryContext context) {
        LivingEntity threat = perceivedThreat(mob, level, profile);
        if (threat != null && MobMindData.getState(mob, StateType.FEAR) >= 1) {
            Vec3 target = BehaviorUtil.targetAwayForDomain(mob, level, profile, threat.position(), 7.0D,
                    sameSpeciesCenter(mob, level, profile, territory));
            move(mob, target, 1.08D);
            return;
        }

        int social = MobMindData.getAttribute(mob, AttributeType.SOCIABILITY);
        List<Mob> school = BehaviorUtil.nearbySameSpecies(mob, level, 7.0D + social * 0.08D);
        if (school.size() >= 2) {
            ArrayList<LivingEntity> group = new ArrayList<>();
            group.add(mob);
            group.addAll(school);
            Vec3 center = BehaviorUtil.centroid(group, mob.position());
            if (mob.distanceToSqr(center) > 100.0D) {
                move(mob, center, 0.90D);
                return;
            }
        }
        returnHomeOrPatrol(mob, level, profile, territory, context, 0.80D);
    }

    private static void predator(Mob mob, ServerLevel level, SpeciesProfile profile,
                                 TerritoryRecord territory, TerritoryContext context) {
        LivingEntity target = mob.getTarget();
        if (BehaviorUtil.isValidCombatTarget(target)) {
            MobMindData.rememberThreat(mob, target, 2, level);
            if (mob.getHealth() < mob.getMaxHealth() * 0.28F || MobMindData.getState(mob, StateType.PAIN) >= 4) {
                mob.setTarget(null);
                Vec3 retreat = BehaviorUtil.targetAwayForDomain(mob, level, profile, target.position(), 9.0D,
                        territory == null ? null : Vec3.atCenterOf(territory.core()));
                move(mob, retreat, 1.18D);
            }
            return;
        }
        returnHomeOrPatrol(mob, level, profile, territory, context, 0.88D);
        updateRest(mob, level, profile, context, false);
    }

    private static void flyingPassive(Mob mob, ServerLevel level, SpeciesProfile profile,
                                      TerritoryRecord territory, TerritoryContext context) {
        LivingEntity threat = perceivedThreat(mob, level, profile);
        if (threat != null) {
            MobMindData.addState(mob, StateType.FEAR, 1);
            move(mob, BehaviorUtil.targetAwayForDomain(mob, level, profile, threat.position(), 8.0D,
                    sameSpeciesCenter(mob, level, profile, territory)), 1.02D);
            return;
        }
        returnHomeOrPatrol(mob, level, profile, territory, context, 0.80D);
        updateRest(mob, level, profile, context, true);
    }

    private static void colony(Mob mob, ServerLevel level, SpeciesProfile profile,
                               TerritoryRecord territory, TerritoryContext context) {
        LivingEntity target = mob.getTarget();
        if (BehaviorUtil.isValidCombatTarget(target)) {
            for (Mob ally : BehaviorUtil.nearbySameSpecies(mob, level, 12.0D)) {
                if (ally.getTarget() == null && ally.hasLineOfSight(mob)) {
                    ally.setTarget(target);
                    MobMindData.rememberThreat(ally, target, 6, level);
                }
            }
            return;
        }
        returnHomeOrPatrol(mob, level, profile, territory, context, 0.88D);
    }

    private static void arthropod(Mob mob, ServerLevel level, SpeciesProfile profile,
                                  TerritoryRecord territory, TerritoryContext context) {
        LivingEntity target = mob.getTarget();
        if (BehaviorUtil.isValidCombatTarget(target)) {
            if (mob.getHealth() < mob.getMaxHealth() * 0.25F && context.ownZone() != TerritoryZone.CORE) {
                mob.setTarget(null);
                move(mob, BehaviorUtil.targetAwayForDomain(mob, level, profile, target.position(), 6.0D,
                        territory == null ? null : Vec3.atCenterOf(territory.core())), 1.05D);
            }
            return;
        }
        returnHomeOrPatrol(mob, level, profile, territory, context, 0.82D);
    }

    private static void undeadHorde(Mob mob, ServerLevel level, SpeciesProfile profile,
                                    TerritoryRecord territory, TerritoryContext context) {
        List<Mob> horde = BehaviorUtil.nearbySameSpecies(mob, level, 12.0D + profile.midpoint(AttributeType.SOCIABILITY) * 0.08D);
        LivingEntity target = mob.getTarget();
        if (!BehaviorUtil.isValidCombatTarget(target)) target = null;
        if (target == null) {
            for (Mob ally : horde) {
                if (BehaviorUtil.isValidCombatTarget(ally.getTarget())) {
                    target = ally.getTarget();
                    break;
                }
            }
        }
        if (target != null) {
            mob.setTarget(target);
            MobMindData.rememberThreat(mob, target, 3, level);
            for (int i = 0; i < Math.min(horde.size(), 5); i++) {
                Mob ally = horde.get(i);
                if (ally.getTarget() == null && ally.hasLineOfSight(mob)) ally.setTarget(target);
            }
            recoverNavigation(mob, level, profile, target.position(), 1.0D);
            return;
        }
        returnHomeOrPatrol(mob, level, profile, territory, context, 0.82D);
    }

    private static void hostile(Mob mob, ServerLevel level, SpeciesProfile profile,
                                TerritoryRecord territory, TerritoryContext context) {
        LivingEntity target = mob.getTarget();
        if (BehaviorUtil.isValidCombatTarget(target)) {
            MobMindData.rememberThreat(mob, target, 2, level);
            // Ranged and special vanilla mobs own their combat positioning. Only recover truly stalled
            // melee-like navigation; never continuously overwrite a working vanilla path.
            if (profile.behaviorFamily() == BehaviorFamily.HOSTILE_MELEE
                    || profile.behaviorFamily() == BehaviorFamily.UNDEAD_COMBAT) {
                recoverNavigation(mob, level, profile, target.position(), 1.0D);
            }
            return;
        }
        returnHomeOrPatrol(mob, level, profile, territory, context, 0.80D);
    }

    private static void societyLowTouch(Mob mob, ServerLevel level, SpeciesProfile profile,
                                        TerritoryRecord territory, TerritoryContext context) {
        // Villager/Piglin/Illager brains are complex. Do not steal their navigation unless they have
        // demonstrably left their recognized territory and are currently idle.
        if (BehaviorUtil.isValidCombatTarget(mob.getTarget())) return;
        if (territory != null && context.ownZone() == TerritoryZone.OUTSIDE && mob.getNavigation().isDone()) {
            double d2 = mob.distanceToSqr(Vec3.atCenterOf(territory.core()));
            if (d2 > 32.0D * 32.0D) move(mob, Vec3.atCenterOf(territory.core()), 0.82D);
        }
    }

    private static void bossLowTouch(Mob mob, ServerLevel level, SpeciesProfile profile,
                                     TerritoryRecord territory, TerritoryContext context) {
        if (territory != null && MobMindData.isBoss(mob)) TerritoryManager.boostBossTerritory(mob, level);
        // Existing bosses have bespoke vanilla state machines; ecological code records context only.
    }

    private static void prioritizeEcologicalEnemy(Mob mob, ServerLevel level, SpeciesProfile profile) {
        boolean combatFamily = switch (profile.behaviorFamily()) {
            case PREDATOR, COLONY, ARTHROPOD, UNDEAD_HORDE, UNDEAD_COMBAT, HOSTILE_MELEE,
                    HOSTILE_RANGED, FLYING_HOSTILE, EXPLOSIVE, SOCIETY_HOSTILE, GUARDIAN -> true;
            default -> false;
        };
        if (!combatFamily) return;
        double range = BehaviorUtil.perceptionRange(mob, MobMindData.getAttribute(mob, AttributeType.PERCEPTION));
        Optional<LivingEntity> candidate = BehaviorUtil.nearestLiving(mob, level, range, living -> {
            if (!(living instanceof Mob other) || !MobMindData.supports(other)) return false;
            SpeciesType otherSpecies = SpeciesType.from(other).orElse(null);
            if (otherSpecies == null || otherSpecies == profile.species()) return false;
            var relation = RelationService.natural(profile.species(), otherSpecies);
            return relation.rivalry() >= 85 && relation.affinity() < 30;
        });
        if (candidate.isEmpty()) return;
        LivingEntity enemy = candidate.get();
        LivingEntity current = mob.getTarget();
        if (!BehaviorUtil.isValidCombatTarget(current)
                || current instanceof net.minecraft.world.entity.player.Player
                || mob.distanceToSqr(enemy) < mob.distanceToSqr(current) * 0.80D) {
            mob.setTarget(enemy);
            MobMindData.rememberThreat(mob, enemy, 5, level);
        }
    }

    private static LivingEntity perceivedThreat(Mob mob, ServerLevel level, SpeciesProfile profile) {
        Optional<LivingEntity> remembered = MobMindData.resolveThreat(mob, level);
        if (remembered.isEmpty()) return null;
        LivingEntity threat = remembered.get();
        double range = BehaviorUtil.perceptionRange(mob, MobMindData.getAttribute(mob, AttributeType.PERCEPTION));
        if (mob.distanceTo(threat) > range * 1.5D) return null;
        return mob.hasLineOfSight(threat) || MobMindData.threatScore(mob) >= 45 ? threat : null;
    }

    private static Optional<BlockPos> fireThreat(Mob mob, ServerLevel level, SpeciesProfile profile) {
        if (profile.habitatClass() == HabitatClass.LAVA || profile.habitatClass() == HabitatClass.NETHER) return Optional.empty();
        if (profile.species() == SpeciesType.BLAZE || profile.species() == SpeciesType.MAGMA_CUBE) return Optional.empty();
        return BehaviorUtil.nearbyFire(mob, level, 2, 1);
    }

    private static Vec3 sameSpeciesCenter(Mob mob, ServerLevel level, SpeciesProfile profile, TerritoryRecord territory) {
        List<Mob> same = BehaviorUtil.nearbySameSpecies(mob, level,
                7.0D + profile.midpoint(AttributeType.SOCIABILITY) * 0.08D);
        if (!same.isEmpty()) {
            ArrayList<LivingEntity> group = new ArrayList<>();
            group.add(mob);
            group.addAll(same);
            return BehaviorUtil.centroid(group, mob.position());
        }
        return territory == null ? mob.position() : Vec3.atCenterOf(territory.core());
    }

    private static void returnHomeOrPatrol(Mob mob, ServerLevel level, SpeciesProfile profile,
                                           TerritoryRecord territory, TerritoryContext context, double speed) {
        if (territory != null && context.ownZone() == TerritoryZone.OUTSIDE
                && mob.distanceToSqr(Vec3.atCenterOf(territory.core())) > 28.0D * 28.0D
                && mob.getNavigation().isDone()) {
            move(mob, Vec3.atCenterOf(territory.core()), speed);
            return;
        }
        if (mob.getNavigation().isDone() && MobMindData.patrolDue(mob, level)) {
            Vec3 center = territory == null ? mob.position() : Vec3.atCenterOf(territory.core());
            double radius = territory == null ? 9.0D : Math.min(24.0D, territory.radiusChunks() * 8.0D);
            Optional<Vec3> point = BehaviorUtil.safePatrolPoint(mob, level, profile, center, radius);
            if (point.isPresent()) {
                move(mob, point.get(), speed);
                MobMindData.scheduleNextPatrol(mob, level, 90, 240);
            } else {
                MobMindData.scheduleNextPatrol(mob, level, 50, 110);
            }
        }
    }

    private static void recoverNavigation(Mob mob, ServerLevel level, SpeciesProfile profile, Vec3 destination, double speed) {
        boolean stalled = MobMindData.movementStalled(mob, level, 35, 0.45D);
        if (mob.getNavigation().isDone() || stalled) {
            if (stalled) mob.getNavigation().stop();
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
        }
    }

    private static void updateRest(Mob mob, ServerLevel level, SpeciesProfile profile, TerritoryContext context, boolean passive) {
        if (!passive || BehaviorUtil.isValidCombatTarget(mob.getTarget())) {
            MobMindData.setResting(mob, false);
            return;
        }
        long day = Math.floorMod(level.getDayTime(), 24000L);
        boolean quiet = MobMindData.getState(mob, StateType.FEAR) == 0
                && MobMindData.getState(mob, StateType.STRESS) <= 1
                && context.ownZone() != TerritoryZone.OUTSIDE;
        boolean inactive = switch (profile.activityPattern()) {
            case DIURNAL -> day >= 13500L && day <= 22500L;
            case NOCTURNAL -> day >= 1000L && day <= 10500L;
            case VARIABLE -> false;
        };
        if (inactive && quiet && mob.getRandom().nextInt(4) == 0) {
            mob.getNavigation().stop();
            MobMindData.setResting(mob, true);
        } else if (!inactive) {
            MobMindData.setResting(mob, false);
        }
    }

    private static boolean isFearFamily(BehaviorFamily family) {
        return switch (family) {
            case PASSIVE_HERD, PASSIVE_WANDERER, AQUATIC, FLYING_PASSIVE, SPECIAL_MOUNT, SOCIETY_PEACEFUL -> true;
            default -> false;
        };
    }

    private static void move(Mob mob, Vec3 point, double speed) {
        if (point.distanceToSqr(mob.position()) < 1.0D) return;
        mob.getNavigation().moveTo(point.x, point.y, point.z, speed);
        MobMindData.setResting(mob, false);
    }
}
