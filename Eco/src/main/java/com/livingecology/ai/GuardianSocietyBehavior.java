package com.livingecology.ai;

import com.livingecology.data.AttributeType;
import com.livingecology.data.MobMindData;
import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.EnvironmentSnapshot;
import com.livingecology.territory.TerritoryContext;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import com.livingecology.territory.TerritoryZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Low-touch Guardian/Elder Guardian monument overlay.
 * Beam targeting, thorns, mining fatigue and swim/move-control remain vanilla-owned.
 */
public final class GuardianSocietyBehavior {
    private GuardianSocietyBehavior() {}

    public static void tick(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return;
        GuardianSocietySpeciesPolicy policy = GuardianSocietySpeciesPolicy.of(species).orElse(null);
        if (policy == null) return;

        BehaviorUtil.sanitizeCombatTarget(mob, level);
        TerritoryRecord territory = TerritoryManager.ensureTerritory(mob, level);
        TerritoryContext context = TerritoryManager.contextForMob(mob, level);
        EnvironmentSnapshot environment = EnvironmentManager.snapshot(level, mob.blockPosition(), species);
        if (environment.habitability() < 25) MobMindData.addState(mob, StateType.STRESS, 1);
        if (context.contested()) MobMindData.addState(mob, StateType.RAGE, 1);
        MobMindData.setResting(mob, false);

        LivingEntity target = mob.getTarget();
        LivingEntity report = BehaviorUtil.isValidCombatTarget(target) ? target : recentDirectAttacker(mob);
        if (report != null) {
            MobMindData.rememberThreat(mob, report, 3, level);
            shareBoundedMonumentMemory(mob, level, policy, report);
            return; // combat/movement remains entirely vanilla-owned while a threat is active
        }

        returnToMonumentOnlyWhenWaterIdle(mob, level, territory, context);
    }

    private static LivingEntity recentDirectAttacker(Mob mob) {
        LivingEntity attacker = mob.getLastHurtByMob();
        int elapsed = mob.tickCount - mob.getLastHurtByMobTimestamp();
        return elapsed >= 0 && elapsed <= 200 && BehaviorUtil.isValidCombatTarget(attacker) ? attacker : null;
    }

    private static void shareBoundedMonumentMemory(Mob source, ServerLevel level,
                                                    GuardianSocietySpeciesPolicy sourcePolicy,
                                                    LivingEntity threat) {
        int informed = 0;
        for (Mob ally : level.getEntitiesOfClass(Mob.class,
                new AABB(source.blockPosition()).inflate(sourcePolicy.memoryRange()), other -> {
                    if (other == source || !other.isAlive()) return false;
                    SpeciesType otherSpecies = SpeciesType.from(other).orElse(null);
                    return otherSpecies != null && GuardianSocietySpeciesPolicy.of(otherSpecies).isPresent();
                })) {
            if (informed >= sourcePolicy.maxMemoryReceivers()) break;
            if (!ally.hasLineOfSight(source) && ally.distanceToSqr(source) > 36.0D) continue;
            MobMindData.initialize(ally, level);
            double perception = BehaviorUtil.perceptionRange(ally,
                    MobMindData.getAttribute(ally, AttributeType.PERCEPTION));
            if (ally.distanceTo(threat) > perception * 1.70D) continue;
            MobMindData.rememberThreat(ally, threat, 5, level);
            MobMindData.addState(ally, StateType.RAGE,
                    sourcePolicy.monumentRole() == GuardianSocietySpeciesPolicy.MonumentRole.ELDER ? 2 : 1);
            informed++;
        }
    }

    private static void returnToMonumentOnlyWhenWaterIdle(Mob mob, ServerLevel level,
                                                           TerritoryRecord territory,
                                                           TerritoryContext context) {
        if (territory == null || context.ownZone() != TerritoryZone.OUTSIDE || !mob.getNavigation().isDone()) return;
        if (!mob.isInWaterOrBubble()) return;
        BlockPos core = territory.core();
        if (!level.hasChunkAt(core) || !BehaviorUtil.isValidForDomain(level, core, MovementDomain.WATER)) return;
        Vec3 target = Vec3.atCenterOf(core);
        if (mob.distanceToSqr(target) <= 16.0D * 16.0D) return;
        mob.getNavigation().moveTo(target.x, target.y, target.z, 0.78D);
    }
}
