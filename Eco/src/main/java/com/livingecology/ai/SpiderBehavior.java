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
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class SpiderBehavior {
    private SpiderBehavior() {}

    public static void tick(Mob raw, ServerLevel level) {
        if (!(raw instanceof Spider spider)) return;
        TerritoryRecord territory = TerritoryManager.ensureTerritory(spider, level);
        TerritoryContext context = TerritoryManager.contextForMob(spider, level);
        int perception = MobMindData.getAttribute(spider, AttributeType.PERCEPTION);
        double range = BehaviorUtil.perceptionRange(spider, perception);
        int adaptation = MobMindData.calculateAdaptation(spider, level);

        Optional<BlockPos> fire = BehaviorUtil.nearbyFire(spider, level, 2, 1);
        if (fire.isPresent()) {
            MobMindData.addState(spider, StateType.FEAR, 2);
            spider.setTarget(null);
            if (territory != null) {
                spider.getNavigation().moveTo(territory.core().getX() + 0.5D, territory.core().getY(),
                        territory.core().getZ() + 0.5D, 1.15D);
            }
            MobMindData.setResting(spider, false);
            return;
        }

        if (spider.getHealth() / spider.getMaxHealth() < 0.30F || MobMindData.getState(spider, StateType.PAIN) >= 4) {
            spider.setTarget(null);
            if (territory != null) {
                spider.getNavigation().moveTo(territory.core().getX() + 0.5D, territory.core().getY(),
                        territory.core().getZ() + 0.5D, 1.15D);
            }
            MobMindData.setResting(spider, false);
            return;
        }

        LivingEntity vanillaTarget = spider.getTarget();
        if (!BehaviorUtil.isValidCombatTarget(vanillaTarget)) vanillaTarget = null;
        LivingEntity ecologicalTarget = null;

        // Spider <-> Zombie symbiosis. This is checked before the vanilla target so the player no longer
        // monopolizes every hostile interaction simply by standing nearby.
        for (Mob zombie : level.getEntitiesOfClass(Mob.class, spider.getBoundingBox().inflate(range), m -> {
            SpeciesType type = SpeciesType.from(m).orElse(null);
            return type == SpeciesType.ZOMBIE || type == SpeciesType.ZOMBIE_VILLAGER || type == SpeciesType.HUSK
                    || type == SpeciesType.DROWNED || type == SpeciesType.GIANT;
        })) {
            Optional<LivingEntity> allyThreat = MobMindData.resolveThreat(zombie, level);
            if (allyThreat.isPresent()) {
                LivingEntity threat = allyThreat.get();
                if (BehaviorUtil.isValidCombatTarget(threat)
                        && (SpeciesType.from(threat).orElse(null) == SpeciesType.WOLF || threat instanceof Player)
                        && spider.distanceTo(threat) <= range * 1.2D) {
                    ecologicalTarget = threat;
                    TerritoryManager.recordCooperation(spider, zombie, level, 1);
                    break;
                }
            }
        }

        if (territory != null) {
            // A Wolf deep in the nest has priority over a generic vanilla Player target.
            Optional<Mob> wolfIntruder = BehaviorUtil.nearestSpecies(spider, level, SpeciesType.WOLF, range)
                    .filter(w -> {
                        TerritoryZone z = TerritoryManager.zoneFor(territory, w.blockPosition());
                        return z == TerritoryZone.CORE || (z == TerritoryZone.INNER
                                && MobMindData.getAttribute(spider, AttributeType.TERRITORY) >= 55);
                    });
            if (wolfIntruder.isPresent()) ecologicalTarget = wolfIntruder.get();

            if (ecologicalTarget == null) {
                Optional<LivingEntity> intruder = BehaviorUtil.nearestLiving(spider, level, range, e -> {
                    if (!(e instanceof Player)) return false;
                    TerritoryZone z = TerritoryManager.zoneFor(territory, e.blockPosition());
                    return z == TerritoryZone.CORE || (z == TerritoryZone.INNER
                            && MobMindData.getAttribute(spider, AttributeType.TERRITORY) >= 55);
                });
                ecologicalTarget = intruder.orElse(null);
            }
        }

        LivingEntity target = ecologicalTarget != null ? ecologicalTarget : vanillaTarget;

        if (context.noMansLand() && target == null && territory != null) {
            spider.getNavigation().moveTo(territory.core().getX() + 0.5D, territory.core().getY(),
                    territory.core().getZ() + 0.5D, 0.95D);
            MobMindData.setResting(spider, false);
            return;
        }

        boolean exposedDay = level.isDay() && level.canSeeSky(spider.blockPosition());
        if (target != null) {
            boolean weakReasonToFight = exposedDay && context.ownZone() == TerritoryZone.OUTER
                    && MobMindData.threatScore(spider) < 30 && context.tension() < 60
                    && !(target instanceof Mob m && SpeciesType.from(m).orElse(null) == SpeciesType.WOLF);
            if (weakReasonToFight) {
                spider.setTarget(null);
            } else {
                spider.setTarget(target);
                MobMindData.rememberThreat(spider, target, 5, level);
                MobMindData.setResting(spider, false);
                boolean stalled = MobMindData.movementStalled(spider, level, 30, 0.50D);
                if (adaptation >= 60 && spider.distanceTo(target) > 3.5F
                        && (spider.getNavigation().isDone() || stalled)) {
                    if (stalled) spider.getNavigation().stop();
                    spider.getNavigation().moveTo(target, 1.15D);
                }
                return;
            }
        }

        boolean canRest = exposedDay && territory != null
                && (context.ownZone() == TerritoryZone.CORE || context.ownZone() == TerritoryZone.INNER)
                && MobMindData.getState(spider, StateType.FEAR) == 0;
        MobMindData.setResting(spider, canRest);
        if (canRest) spider.getNavigation().stop();
    }
}
