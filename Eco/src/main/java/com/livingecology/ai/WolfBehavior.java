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
        if (target == null || !target.isAlive()) target = null;

        // High sociability: copy a meaningful target from a packmate.
        if (target == null && MobMindData.getAttribute(wolf, AttributeType.SOCIABILITY) >= 75) {
            for (Mob mate : pack) {
                LivingEntity shared = mate.getTarget();
                if (shared != null && shared.isAlive() && wolf.distanceTo(shared) <= range * 1.3D) {
                    target = shared;
                    break;
                }
            }
        }

        Optional<Mob> zombie = BehaviorUtil.nearestSpecies(wolf, level, SpeciesType.ZOMBIE, range);
        if (target == null && zombie.isPresent()) {
            Mob z = zombie.get();
            TerritoryContext zContext = TerritoryManager.contextAt(level, z.blockPosition(), MobMindData.territoryId(wolf));
            boolean homeDefense = context.ownZone() == TerritoryZone.INNER || context.ownZone() == TerritoryZone.CORE;
            boolean packConfidence = pack.size() >= 1 || adaptation >= 70;
            boolean remembered = MobMindData.resolveThreat(wolf, level).filter(t -> t.getUUID().equals(z.getUUID())).isPresent();
            if (homeDefense || packConfidence || remembered || zContext.contested()) target = z;
        }

        Optional<Mob> spider = BehaviorUtil.nearestSpecies(wolf, level, SpeciesType.SPIDER, range);
        if (target == null && spider.isPresent()) {
            Mob s = spider.get();
            TerritoryZone spiderInWolfHome = territory == null ? TerritoryZone.OUTSIDE : TerritoryManager.zoneFor(territory, s.blockPosition());
            if (spiderInWolfHome == TerritoryZone.CORE || spiderInWolfHome == TerritoryZone.INNER
                    || context.tension() >= 70) {
                target = s;
            }
        }

        if (context.noMansLand() && target == null && territory != null) {
            // Regulated coexistence: both species learn not to live in the border strip.
            wolf.getNavigation().moveTo(territory.core().getX() + 0.5D, territory.core().getY(),
                    territory.core().getZ() + 0.5D, 1.0D);
            MobMindData.setResting(wolf, false);
            return;
        }

        if (target != null) {
            wolf.setTarget(target);
            MobMindData.setResting(wolf, false);
            MobMindData.rememberThreat(wolf, target, 5, level);
            if (adaptation >= 70 && pack.size() >= 1 && wolf.distanceTo(target) > 4.0F) {
                double angle = (wolf.getId() * 1.61803398875D) % (Math.PI * 2.0D);
                double flank = 2.5D + Math.min(3.0D, pack.size() * 0.4D);
                Vec3 p = target.position().add(Math.cos(angle) * flank, 0.0D, Math.sin(angle) * flank);
                wolf.getNavigation().moveTo(p.x, p.y, p.z, 1.20D);
            }
            return;
        }

        if (territory != null && context.ownZone() == TerritoryZone.OUTSIDE) {
            wolf.getNavigation().moveTo(territory.core().getX() + 0.5D, territory.core().getY(),
                    territory.core().getZ() + 0.5D, 1.05D);
            MobMindData.setResting(wolf, false);
            return;
        }

        if (pack.size() >= 2) {
            Vec3 packCenter = BehaviorUtil.centroid(new ArrayList<>(pack), wolf.position());
            if (wolf.position().distanceToSqr(packCenter) > 12.0D * 12.0D) {
                wolf.getNavigation().moveTo(packCenter.x, packCenter.y, packCenter.z, 1.0D);
                MobMindData.setResting(wolf, false);
                return;
            }
        }

        // Fragmented rest: deterministic per individual and logical game time, therefore tick-rate safe.
        long restPhase = Math.floorMod(level.getGameTime() + Math.abs(wolf.getUUID().getLeastSignificantBits()), 900L);
        boolean canRest = restPhase < 180L && MobMindData.getState(wolf, StateType.FEAR) == 0
                && MobMindData.getState(wolf, StateType.STRESS) <= 1
                && context.ownZone() != TerritoryZone.OUTSIDE;
        MobMindData.setResting(wolf, canRest);
        if (canRest) wolf.getNavigation().stop();
    }

    private static void tickTamed(Wolf wolf, ServerLevel level) {
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
