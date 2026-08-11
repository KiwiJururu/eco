package com.livingecology.ai;

import com.livingecology.data.ActivityPattern;
import com.livingecology.data.AttributeType;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.EnvironmentSnapshot;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import com.livingecology.territory.TerritoryZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Conservative overlay that never invents targets for terrestrial predators/independent animals. */
public final class IndependentLandBehavior {
    private IndependentLandBehavior() {}

    public static void tick(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return;
        IndependentLandSpeciesPolicy policy = IndependentLandSpeciesPolicy.of(species).orElse(null);
        if (policy == null) return;

        SpeciesProfile profile = SpeciesProfile.of(species);
        TerritoryRecord territory = TerritoryManager.ensureTerritory(mob, level);
        EnvironmentSnapshot environment = EnvironmentManager.snapshot(level, mob.blockPosition(), species);
        if (environment.habitability() < 25) MobMindData.addState(mob, StateType.STRESS, 1);
        MobMindData.setResting(mob, false);

        switch (policy.overlayMode()) {
            case FOX_STATE_SAFE_HOME_RANGE -> fox((Fox) mob, level, profile, territory);
            case OCELOT_TRUST_SAFE_HOME_RANGE -> ocelot((Ocelot) mob, level, profile, territory);
            case WILD_CAT_HOME_RANGE -> cat((Cat) mob, level, profile, territory);
            case POLAR_BEAR_NEUTRAL_OBSERVATION -> polarBear((PolarBear) mob, level, profile, territory);
            case PANDA_GENE_STATE_OBSERVATION -> panda((Panda) mob, level, profile, territory);
        }
    }

    private static void fox(Fox fox, ServerLevel level, SpeciesProfile profile,
                            TerritoryRecord territory) {
        if (fox.isSleeping()) MobMindData.setResting(fox, true);
        if (fox.isSleeping() || fox.isSitting() || fox.isPouncing() || fox.isCrouching()
                || fox.isFaceplanted() || fox.isJumping() || fox.isPassenger() || fox.isLeashed()
                || fox.isInLove() || BehaviorUtil.isValidCombatTarget(fox.getTarget())
                || !fox.getNavigation().isDone()) return;

        if (retreatFromRememberedThreat(fox, level, profile, territory, 0.42F, 1, 1.08D)) return;
        if (allowsIdleOverlay(profile.activityPattern(), level.getDayTime())) {
            returnHomeIfFarOutside(fox, level, profile, territory, 0.88D);
        }
    }

    private static void ocelot(Ocelot ocelot, ServerLevel level, SpeciesProfile profile,
                               TerritoryRecord territory) {
        if (ocelot.isPassenger() || ocelot.isLeashed() || ocelot.isInLove()
                || BehaviorUtil.isValidCombatTarget(ocelot.getTarget())
                || !ocelot.getNavigation().isDone()) return;

        if (retreatFromRememberedThreat(ocelot, level, profile, territory, 0.45F, 1, 1.10D)) return;
        if (allowsIdleOverlay(profile.activityPattern(), level.getDayTime())) {
            returnHomeIfFarOutside(ocelot, level, profile, territory, 0.90D);
        }
    }

    private static void cat(Cat cat, ServerLevel level, SpeciesProfile profile,
                            TerritoryRecord territory) {
        if (cat.isTame()) {
            MobMindData.setTerritoryId(cat, 0L);
            return;
        }
        if (cat.isOrderedToSit() || cat.isLying() || cat.isRelaxStateOne()
                || cat.isPassenger() || cat.isLeashed() || cat.isInLove()
                || BehaviorUtil.isValidCombatTarget(cat.getTarget())
                || !cat.getNavigation().isDone()) return;

        if (retreatFromRememberedThreat(cat, level, profile, territory, 0.45F, 1, 1.08D)) return;
        returnHomeIfFarOutside(cat, level, profile, territory, 0.88D);
    }

    private static void polarBear(PolarBear bear, ServerLevel level, SpeciesProfile profile,
                                  TerritoryRecord territory) {
        if (bear.getRemainingPersistentAngerTime() > 0) MobMindData.addState(bear, StateType.RAGE, 1);
        // Neutral anger, standing attacks and the adult-with-cub target goal are exclusively vanilla.
        if (bear.isStanding() || bear.getRemainingPersistentAngerTime() > 0
                || bear.getPersistentAngerTarget() != null
                || BehaviorUtil.isValidCombatTarget(bear.getTarget())
                || hasNearbyCub(bear, level) || bear.isPassenger() || bear.isLeashed()
                || !bear.getNavigation().isDone()) return;

        if (retreatFromRememberedThreat(bear, level, profile, territory, 0.25F, 2, 1.05D)) return;
        returnHomeIfFarOutside(bear, level, profile, territory, 0.86D);
    }

    private static void panda(Panda panda, ServerLevel level, SpeciesProfile profile,
                              TerritoryRecord territory) {
        // Panda genes drive goal selection. Sitting/eating/sneezing/rolling/unhappy transitions
        // are atomic vanilla sequences and must never receive a competing path order.
        if (!panda.canPerformAction() || panda.isSitting() || panda.isOnBack() || panda.isEating()
                || panda.isSneezing() || panda.isRolling() || panda.getUnhappyCounter() > 0
                || panda.isPassenger() || panda.isLeashed() || panda.isInLove()
                || BehaviorUtil.isValidCombatTarget(panda.getTarget())
                || !panda.getNavigation().isDone()) return;

        int fearThreshold = panda.isWorried() ? 1 : 2;
        if (retreatFromRememberedThreat(panda, level, profile, territory,
                panda.isWeak() ? 0.55F : 0.35F, fearThreshold, 1.02D)) return;
        returnHomeIfFarOutside(panda, level, profile, territory, 0.78D);
    }

    private static boolean retreatFromRememberedThreat(Mob mob, ServerLevel level,
                                                        SpeciesProfile profile,
                                                        TerritoryRecord territory,
                                                        float healthFraction, int fearThreshold,
                                                        double speed) {
        if (mob.getHealth() > mob.getMaxHealth() * healthFraction
                && MobMindData.getState(mob, StateType.FEAR) < fearThreshold) return false;
        LivingEntity threat = perceivedThreat(mob, level);
        if (threat == null) return false;

        Vec3 homeBias = territory == null ? null : Vec3.atCenterOf(territory.core());
        Vec3 target = BehaviorUtil.targetAwayForDomain(mob, level, profile,
                threat.position(), 8.0D, homeBias);
        if (target.distanceToSqr(mob.position()) < 1.0D) return false;
        move(mob, target, speed);
        return true;
    }

    private static LivingEntity perceivedThreat(Mob mob, ServerLevel level) {
        Optional<LivingEntity> remembered = MobMindData.resolveThreat(mob, level);
        if (remembered.isEmpty()) return null;
        LivingEntity threat = remembered.get();
        double range = BehaviorUtil.perceptionRange(mob,
                MobMindData.getAttribute(mob, AttributeType.PERCEPTION));
        if (mob.distanceTo(threat) > range * 1.5D) return null;
        return mob.hasLineOfSight(threat) || MobMindData.threatScore(mob) >= 45 ? threat : null;
    }

    private static boolean hasNearbyCub(PolarBear bear, ServerLevel level) {
        int inspected = 0;
        for (Mob other : BehaviorUtil.nearbySameSpecies(bear, level, 12.0D)) {
            if (inspected++ >= 8) break;
            if (other.isBaby()) return true;
        }
        return false;
    }

    private static void returnHomeIfFarOutside(Mob mob, ServerLevel level, SpeciesProfile profile,
                                               TerritoryRecord territory, double speed) {
        if (territory == null || TerritoryManager.zoneFor(territory, mob.blockPosition())
                != TerritoryZone.OUTSIDE) return;
        Vec3 core = Vec3.atCenterOf(territory.core());
        if (mob.distanceToSqr(core) <= 32.0D * 32.0D) return;
        BlockPos corePos = BlockPos.containing(core);
        if (level.hasChunkAt(corePos) && BehaviorUtil.isSafeLand(level, corePos)) {
            move(mob, core, speed);
            return;
        }
        BehaviorUtil.safePatrolPoint(mob, level, profile, core, 6.0D)
                .ifPresent(target -> move(mob, target, speed));
    }

    private static void move(Mob mob, Vec3 target, double speed) {
        mob.getNavigation().moveTo(target.x, target.y, target.z, speed);
        MobMindData.setResting(mob, false);
    }

    public static boolean allowsIdleOverlay(ActivityPattern pattern, long rawDayTime) {
        long day = Math.floorMod(rawDayTime, 24000L);
        return switch (pattern) {
            case DIURNAL -> day < 13000L || day > 22500L;
            case NOCTURNAL -> day > 10500L;
            case VARIABLE -> true;
        };
    }
}
