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

        LivingEntity target = spider.getTarget();
        if (target == null || !target.isAlive()) target = null;

        // Spider <-> Zombie symbiosis: local, incomplete information; helping strengthens acquired affinity.
        if (target == null) {
            for (Mob zombie : BehaviorUtil.species(level, spider.blockPosition(), SpeciesType.ZOMBIE, range)) {
                Optional<LivingEntity> allyThreat = MobMindData.resolveThreat(zombie, level);
                if (allyThreat.isPresent()) {
                    LivingEntity threat = allyThreat.get();
                    if ((SpeciesType.from(threat).orElse(null) == SpeciesType.WOLF || threat instanceof Player)
                            && spider.distanceTo(threat) <= range * 1.2D) {
                        target = threat;
                        TerritoryManager.recordCooperation(spider, zombie, level, 1);
                        break;
                    }
                }
            }
        }

        if (target == null && territory != null) {
            // Territorial response scales strongly from inner zone to core.
            Optional<LivingEntity> intruder = BehaviorUtil.nearestLiving(spider, level, range, e -> {
                boolean relevant = SpeciesType.from(e).orElse(null) == SpeciesType.WOLF || e instanceof Player;
                if (!relevant) return false;
                TerritoryZone z = TerritoryManager.zoneFor(territory, e.blockPosition());
                return z == TerritoryZone.CORE || (z == TerritoryZone.INNER
                        && MobMindData.getAttribute(spider, AttributeType.TERRITORY) >= 55);
            });
            target = intruder.orElse(null);
        }

        if (context.noMansLand() && target == null && territory != null) {
            spider.getNavigation().moveTo(territory.core().getX() + 0.5D, territory.core().getY(),
                    territory.core().getZ() + 0.5D, 0.95D);
            MobMindData.setResting(spider, false);
            return;
        }

        boolean exposedDay = level.isDay() && level.canSeeSky(spider.blockPosition());
        if (target != null) {
            boolean weakReasonToFight = exposedDay && context.ownZone() == TerritoryZone.OUTER
                    && MobMindData.threatScore(spider) < 30 && context.tension() < 60;
            if (weakReasonToFight) {
                spider.setTarget(null);
            } else {
                spider.setTarget(target);
                MobMindData.rememberThreat(spider, target, 5, level);
                MobMindData.setResting(spider, false);
                if (adaptation >= 60 && spider.distanceTo(target) > 3.5F) {
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
