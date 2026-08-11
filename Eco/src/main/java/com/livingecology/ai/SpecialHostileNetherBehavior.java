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
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.hoglin.Hoglin;

/**
 * Observation-only ecological overlay for special hostile and Nether movers.
 *
 * <p>No method in this controller issues navigation, selects a valid target or mutates defining
 * vanilla state. Invalid Creative/Spectator targets are still sanitized. Fuse, jump control,
 * flight, Brain activities, riding and breeding remain vanilla-owned.</p>
 */
public final class SpecialHostileNetherBehavior {
    private SpecialHostileNetherBehavior() {}

    public static void tick(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return;
        SpecialHostileNetherSpeciesPolicy policy =
                SpecialHostileNetherSpeciesPolicy.of(species).orElse(null);
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
        }

        switch (policy.observationMode()) {
            case CREEPER_FUSE_OBSERVATION -> observeCreeper((Creeper) mob);
            case BLAZE_RANGED_FLIGHT_OBSERVATION -> observeBlaze((Blaze) mob);
            case GHAST_CHARGE_FLIGHT_OBSERVATION -> observeGhast((Ghast) mob);
            case VEX_OWNER_CHARGE_BOUND_OBSERVATION -> observeVex((Vex) mob);
            case HOGLIN_BRAIN_CONVERSION_OBSERVATION -> observeHoglin((Hoglin) mob);
            case STRIDER_RIDE_TEMPERATURE_OBSERVATION -> observeStrider((Strider) mob);
            case SLIME_JUMP_OBSERVATION, MAGMA_JUMP_OBSERVATION,
                    PHANTOM_SWEEP_ANCHOR_OBSERVATION, ZOGLIN_BRAIN_OBSERVATION -> {
                // These vanilla state machines expose no additional ecological action here.
            }
        }
    }

    private static void observeCreeper(Creeper creeper) {
        if (creeper.isIgnited() || creeper.getSwellDir() > 0) {
            MobMindData.addState(creeper, StateType.RAGE, 1);
        }
    }

    private static void observeBlaze(Blaze blaze) {
        if (blaze.isInWaterRainOrBubble()) MobMindData.addState(blaze, StateType.STRESS, 2);
    }

    private static void observeGhast(Ghast ghast) {
        if (ghast.isCharging()) MobMindData.addState(ghast, StateType.RAGE, 1);
    }

    private static void observeVex(Vex vex) {
        if (vex.isCharging()) MobMindData.addState(vex, StateType.RAGE, 1);
    }

    private static void observeHoglin(Hoglin hoglin) {
        if (hoglin.isConverting()) MobMindData.addState(hoglin, StateType.STRESS, 2);
    }

    private static void observeStrider(Strider strider) {
        if (strider.isSuffocating() || strider.isInWaterRainOrBubble()) {
            MobMindData.addState(strider, StateType.STRESS, 2);
        }
    }
}
