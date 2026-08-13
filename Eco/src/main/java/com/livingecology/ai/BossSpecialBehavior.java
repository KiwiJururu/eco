package com.livingecology.ai;

import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.territory.TerritoryManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Observation-only ecology for phase-driven bosses and special/internal mobs. */
public final class BossSpecialBehavior {
    private BossSpecialBehavior() {}

    public static void tick(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null || BossSpecialSpeciesPolicy.of(species).isEmpty()) return;
        BehaviorUtil.sanitizeCombatTarget(mob, level);
        TerritoryManager.ensureTerritory(mob, level);
        var context = TerritoryManager.contextForMob(mob, level);
        var environment = EnvironmentManager.snapshot(level, mob.blockPosition(), species);
        if (environment.habitability() < 25) MobMindData.addState(mob, StateType.STRESS, 1);
        if (context.contested()) MobMindData.addState(mob, StateType.RAGE, 1);
        MobMindData.setResting(mob, false);

        LivingEntity target = mob.getTarget();
        LivingEntity attacker = mob.getLastHurtByMob();
        int elapsed = mob.tickCount - mob.getLastHurtByMobTimestamp();
        LivingEntity report = BehaviorUtil.isValidCombatTarget(target) ? target
                : elapsed >= 0 && elapsed <= 200 && BehaviorUtil.isValidCombatTarget(attacker) ? attacker : null;
        if (report != null) MobMindData.rememberThreat(mob, report, 3, level);
        // Deliberately no target assignment, path order, move-control call or raw attribute mutation.
    }
}
