package com.livingecology.ai;

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
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.AABB;

/**
 * Low-touch ecological overlay for Enderman, Endermite, Silverfish and Shulker.
 *
 * <p>This controller never issues navigation, chooses a valid target, teleports an entity or
 * changes carried blocks, attachment, peek, anger, lifetime or vanilla goal state. Silverfish may
 * share bounded threat memory inside a loaded nest, but receivers retain vanilla target choice.</p>
 */
public final class EndCaveSpecialBehavior {
    private static final int MAX_NEST_MEMORY_RECEIVERS = 8;
    private static final double NEST_MEMORY_RANGE = 12.0D;

    private EndCaveSpecialBehavior() {}

    public static void tick(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return;
        EndCaveSpecialSpeciesPolicy policy = EndCaveSpecialSpeciesPolicy.of(species).orElse(null);
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
        if (BehaviorUtil.isValidCombatTarget(target)) {
            MobMindData.rememberThreat(mob, target, 3, level);
            if (policy.sharesNestMemory()) shareSilverfishNestMemory(mob, level, target);
        }

        if (mob instanceof EnderMan enderman && enderman.isInWaterRainOrBubble()) {
            MobMindData.addState(enderman, StateType.STRESS, 2);
        }
    }

    private static void shareSilverfishNestMemory(Mob source, ServerLevel level,
                                                   LivingEntity target) {
        int informed = 0;
        for (Mob ally : level.getEntitiesOfClass(Mob.class,
                new AABB(source.blockPosition()).inflate(NEST_MEMORY_RANGE), other ->
                        other != source && other.isAlive()
                                && SpeciesType.from(other).orElse(null) == SpeciesType.SILVERFISH)) {
            if (informed >= MAX_NEST_MEMORY_RECEIVERS) break;
            if (!ally.hasLineOfSight(source) && ally.distanceToSqr(source) > 36.0D) continue;
            MobMindData.initialize(ally, level);
            MobMindData.rememberThreat(ally, target, 4, level);
            MobMindData.addState(ally, StateType.RAGE, 1);
            informed++;
        }
    }
}
