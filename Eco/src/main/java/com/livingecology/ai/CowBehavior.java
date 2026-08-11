package com.livingecology.ai;

import com.livingecology.data.AttributeType;
import com.livingecology.data.MobMindData;
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
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CowBehavior {
    private CowBehavior() {}

    public static void tick(Mob raw, ServerLevel level) {
        if (!(raw instanceof Cow cow)) return;
        TerritoryRecord territory = TerritoryManager.ensureTerritory(cow, level);
        TerritoryContext context = TerritoryManager.contextForMob(cow, level);
        int social = MobMindData.getAttribute(cow, AttributeType.SOCIABILITY);
        int perception = MobMindData.getAttribute(cow, AttributeType.PERCEPTION);
        double perceptionRange = BehaviorUtil.perceptionRange(cow, perception);

        if (BehaviorUtil.guardAgainstObviousLandHazard(cow, level)) {
            MobMindData.addState(cow, StateType.STRESS, 1);
            MobMindData.setResting(cow, false);
            return;
        }

        Optional<BlockPos> fire = BehaviorUtil.nearbyFire(cow, level, 2, 1);
        fire.ifPresent(pos -> {
            MobMindData.addState(cow, StateType.FEAR, 2);
            MobMindData.addState(cow, StateType.STRESS, 1);
            MobMindData.rememberThreatPosition(cow, pos, 20, level);
            MobMindData.setResting(cow, false);
        });

        Optional<LivingEntity> rememberedThreat = MobMindData.resolveThreat(cow, level);
        LivingEntity threat = rememberedThreat.filter(t -> cow.distanceTo(t) <= perceptionRange * 1.5D).orElse(null);
        if (threat != null && (cow.hasLineOfSight(threat) || MobMindData.threatScore(cow) >= 45)) {
            MobMindData.addState(cow, StateType.FEAR, 1);
        }

        List<Mob> nearby = BehaviorUtil.nearbySameSpecies(cow, level, 10.0D + social * 0.08D);
        ArrayList<LivingEntity> herd = new ArrayList<>();
        herd.add(cow);
        herd.addAll(nearby);
        Vec3 herdCenter = BehaviorUtil.centroid(herd, cow.position());

        int fear = MobMindData.getState(cow, StateType.FEAR);
        if (fear >= 2 || fire.isPresent()) {
            Vec3 source = threat != null ? threat.position()
                    : fire.<Vec3>map(Vec3::atCenterOf)
                    .orElseGet(() -> MobMindData.lastThreatPos(cow).map(Vec3::atCenterOf)
                            .orElse(cow.position().subtract(cow.getLookAngle())));
            Vec3 target = BehaviorUtil.safeTargetAwayFrom(cow, level, source, 7.0D + fear * 1.5D, herdCenter);
            cow.getNavigation().moveTo(target.x, target.y, target.z, 1.15D + fear * 0.05D);
            MobMindData.setResting(cow, false);
            return;
        }

        if (cow.isBaby()) {
            Cow adult = level.getEntitiesOfClass(Cow.class, cow.getBoundingBox().inflate(12.0D),
                            other -> other != cow && other.isAlive() && !other.isBaby()).stream()
                    .min((a, b) -> Double.compare(cow.distanceToSqr(a), cow.distanceToSqr(b))).orElse(null);
            if (adult != null && cow.distanceToSqr(adult) > 16.0D) {
                cow.getNavigation().moveTo(adult, 1.05D);
                MobMindData.setResting(cow, false);
                return;
            }
        }

        double cohesionDistance = 7.0D + (100 - social) * 0.06D;
        if (nearby.size() >= 2 && cow.position().distanceToSqr(herdCenter) > cohesionDistance * cohesionDistance) {
            cow.getNavigation().moveTo(herdCenter.x, herdCenter.y, herdCenter.z, 1.0D);
            MobMindData.setResting(cow, false);
            return;
        }

        if (territory != null && context.ownZone() == TerritoryZone.OUTSIDE
                && cow.distanceToSqr(Vec3.atCenterOf(territory.core())) > 20.0D * 20.0D) {
            cow.getNavigation().moveTo(territory.core().getX() + 0.5D, territory.core().getY(),
                    territory.core().getZ() + 0.5D, 0.95D);
            MobMindData.setResting(cow, false);
            return;
        }

        EnvironmentSnapshot environment = EnvironmentManager.snapshot(level, cow.blockPosition(), com.livingecology.data.SpeciesType.COW);
        if (environment.habitability() < 30) MobMindData.addState(cow, StateType.STRESS, 1);

        long dayTime = Math.floorMod(level.getDayTime(), 24000L);
        boolean restWindow = dayTime >= 13000L && dayTime <= 22500L;
        boolean safe = MobMindData.getState(cow, StateType.FEAR) == 0
                && MobMindData.getState(cow, StateType.STRESS) <= 1
                && context.ownZone() != TerritoryZone.OUTSIDE;
        if (restWindow && safe) {
            cow.getNavigation().stop();
            MobMindData.setResting(cow, true);
        } else {
            MobMindData.setResting(cow, false);
        }
    }
}
