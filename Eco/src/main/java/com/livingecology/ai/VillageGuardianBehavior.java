package com.livingecology.ai;

import com.livingecology.data.AttributeType;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.EnvironmentSnapshot;
import com.livingecology.territory.TerritoryContext;
import com.livingecology.territory.TerritoryManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Low-touch village and trader overlay.
 *
 * <p>Brain schedules, trades, despawn clocks, navigation and guardian combat remain vanilla-owned.
 * A current vanilla target or directly perceived attacker may be reported as bounded local memory;
 * receivers never receive a forced combat target.</p>
 */
public final class VillageGuardianBehavior {
    private static final int MAX_DEFENSE_MEMORY_RECEIVERS = 8;
    private static final double DEFENSE_MEMORY_RANGE = 16.0D;

    private VillageGuardianBehavior() {}

    public static void tick(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return;
        VillageGuardianSpeciesPolicy policy =
                VillageGuardianSpeciesPolicy.of(species).orElse(null);
        if (policy == null) return;

        BehaviorUtil.sanitizeCombatTarget(mob, level);
        TerritoryManager.ensureTerritory(mob, level);
        TerritoryContext context = TerritoryManager.contextForMob(mob, level);
        EnvironmentSnapshot environment = EnvironmentManager.snapshot(
                level, mob.blockPosition(), species);
        if (environment.habitability() < 25) MobMindData.addState(mob, StateType.STRESS, 1);
        if (context.contested()) MobMindData.addState(mob, StateType.RAGE, 1);
        MobMindData.setResting(mob, false);

        LivingEntity target = mob.getTarget();
        LivingEntity report = BehaviorUtil.isValidCombatTarget(target)
                ? target : recentDirectAttacker(mob);
        if (report == null) return;

        MobMindData.rememberThreat(mob, report, 3, level);
        shareBoundedDefenseMemory(mob, level, policy, report);
    }

    private static LivingEntity recentDirectAttacker(Mob mob) {
        LivingEntity attacker = mob.getLastHurtByMob();
        int elapsed = mob.tickCount - mob.getLastHurtByMobTimestamp();
        return elapsed >= 0 && elapsed <= 200 && BehaviorUtil.isValidCombatTarget(attacker)
                ? attacker : null;
    }

    private static void shareBoundedDefenseMemory(Mob source, ServerLevel level,
                                                  VillageGuardianSpeciesPolicy sourcePolicy,
                                                  LivingEntity threat) {
        int informed = 0;
        for (Mob ally : level.getEntitiesOfClass(Mob.class,
                new AABB(source.blockPosition()).inflate(DEFENSE_MEMORY_RANGE), other -> {
                    if (other == source || !other.isAlive()) return false;
                    SpeciesType otherSpecies = SpeciesType.from(other).orElse(null);
                    return otherSpecies != null && VillageGuardianSpeciesPolicy.of(otherSpecies)
                            .map(policy -> policy.knowledgeGroup() == sourcePolicy.knowledgeGroup())
                            .orElse(false);
                })) {
            if (informed >= MAX_DEFENSE_MEMORY_RECEIVERS) break;
            if (!ally.hasLineOfSight(source) && ally.distanceToSqr(source) > 36.0D) continue;
            MobMindData.initialize(ally, level);
            double perception = BehaviorUtil.perceptionRange(ally,
                    MobMindData.getAttribute(ally, AttributeType.PERCEPTION));
            if (ally.distanceTo(threat) > perception * 1.70D) continue;

            MobMindData.rememberThreat(ally, threat, 5, level);
            VillageGuardianSpeciesPolicy.CommunityRole role =
                    VillageGuardianSpeciesPolicy.require(
                            SpeciesType.from(ally).orElseThrow()).communityRole();
            if (role == VillageGuardianSpeciesPolicy.CommunityRole.RESIDENT
                    || role == VillageGuardianSpeciesPolicy.CommunityRole.TRANSIENT_TRADER) {
                MobMindData.addState(ally, StateType.FEAR, 1);
            } else {
                MobMindData.addState(ally, StateType.RAGE, 1);
            }
            informed++;
        }
    }
}
