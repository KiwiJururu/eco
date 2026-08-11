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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Conservative overlay for Bat, Parrot, Allay and Bee. Vanilla remains the movement owner. */
public final class FlyingColonyBehavior {
    private static final int MAX_GROUP_SAMPLE = 12;
    private static final int MAX_COLONY_RECEIVERS = 12;
    private static final double BEE_SIGNAL_RANGE = 12.0D;
    private static final double BEE_CORE_RANGE = 24.0D;
    private static final double MAX_HIVE_RECENTER_DISTANCE = 48.0D;

    private FlyingColonyBehavior() {}

    public static void tick(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return;
        FlyingColonySpeciesPolicy policy = FlyingColonySpeciesPolicy.of(species).orElse(null);
        if (policy == null) return;

        SpeciesProfile profile = SpeciesProfile.of(species);
        TerritoryRecord territory = TerritoryManager.ensureTerritory(mob, level);
        EnvironmentSnapshot environment = EnvironmentManager.snapshot(level, mob.blockPosition(), species);
        if (environment.habitability() < 25) MobMindData.addState(mob, StateType.STRESS, 1);

        switch (policy.overlayMode()) {
            case BAT_STATE_OBSERVATION -> observeBat((Bat) mob);
            case WILD_PARROT_IDLE_COHESION -> wildParrot((Parrot) mob, level, profile);
            case ALLAY_BRAIN_OBSERVATION -> observeAllay((Allay) mob);
            case BEE_HIVE_COLONY_MEMORY -> beeColony((Bee) mob, level, territory);
        }
    }

    private static void observeBat(Bat bat) {
        // Bat.customServerAiStep owns its targetPosition and hanging transitions. Mirroring the
        // vanilla flag exposes meaningful debug state without issuing a PathNavigation order.
        MobMindData.setResting(bat, bat.isResting());
    }

    private static void wildParrot(Parrot parrot, ServerLevel level, SpeciesProfile profile) {
        if (parrot.isTame() || parrot.isOrderedToSit() || parrot.isPartyParrot()
                || parrot.isPassenger() || parrot.isLeashed()) {
            MobMindData.setTerritoryId(parrot, 0L);
            MobMindData.setResting(parrot, false);
            return;
        }
        if (!allowsIdleOverlay(profile.activityPattern(), level.getDayTime())) return;

        LivingEntity threat = perceivedThreat(parrot, level);
        if (threat != null && MobMindData.getState(parrot, StateType.FEAR) >= 2
                && parrot.getNavigation().isDone()) {
            Vec3 socialBias = groupCenter(parrot, level, profile).orElse(null);
            Vec3 target = BehaviorUtil.targetAwayForDomain(parrot, level, profile,
                    threat.position(), 8.0D, socialBias);
            if (target.distanceToSqr(parrot.position()) >= 1.0D) {
                parrot.getNavigation().moveTo(target.x, target.y, target.z, 1.02D);
                MobMindData.setResting(parrot, false);
            }
            return;
        }

        if (!parrot.getNavigation().isDone()) return;
        groupCenter(parrot, level, profile)
                .filter(center -> parrot.distanceToSqr(center) > 36.0D)
                .filter(center -> {
                    BlockPos pos = BlockPos.containing(center);
                    return level.hasChunkAt(pos)
                            && BehaviorUtil.isValidForDomain(level, pos, profile.movementDomain());
                })
                .ifPresent(center -> parrot.getNavigation().moveTo(
                        center.x, center.y, center.z, 0.86D));
    }

    private static void observeAllay(Allay allay) {
        // AllayAi owns WALK_TARGET, liked-player/note-block memories, pickup/delivery and dancing.
        // The ecological overlay records panic only and never calls navigation here.
        if (allay.isPanicking()) {
            MobMindData.addState(allay, StateType.FEAR, 1);
            MobMindData.addState(allay, StateType.STRESS, 1);
        }
        MobMindData.setResting(allay, false);
    }

    private static void beeColony(Bee bee, ServerLevel level, TerritoryRecord territory) {
        TerritoryRecord anchored = anchorToLoadedHive(bee, level, territory);
        if (bee.isAngry()) MobMindData.addState(bee, StateType.RAGE, 1);

        LivingEntity threat = bee.getTarget();
        if (anchored == null || !BehaviorUtil.isValidCombatTarget(threat)) return;
        shareBoundedColonyMemory(bee, level, anchored, threat);
        // BeeHurtByOtherGoal and the neutral-mob anger system remain solely responsible for targets.
    }

    private static TerritoryRecord anchorToLoadedHive(Bee bee, ServerLevel level,
                                                       TerritoryRecord territory) {
        if (territory == null || !bee.hasHive()) return territory;
        BlockPos hive = bee.getHivePos();
        if (!isLoadedHive(level, hive)) return territory;

        // A valid existing hive wins over a different bee's association, preventing nearby hives
        // from moving one shared territory back and forth every brain tick.
        if (isLoadedHive(level, territory.core())) return territory;
        TerritoryManager.recenterOnLoadedCore(
                territory, level, hive, MAX_HIVE_RECENTER_DISTANCE);
        return territory;
    }

    private static void shareBoundedColonyMemory(Bee source, ServerLevel level,
                                                 TerritoryRecord territory, LivingEntity threat) {
        int informed = 0;
        List<Bee> nearby = level.getEntitiesOfClass(Bee.class,
                source.getBoundingBox().inflate(BEE_SIGNAL_RANGE),
                bee -> bee != source && bee.isAlive());
        for (Bee ally : nearby) {
            if (informed >= MAX_COLONY_RECEIVERS) break;
            if (MobMindData.territoryId(ally) != territory.id()) continue;
            if (ally.distanceToSqr(Vec3.atCenterOf(territory.core()))
                    > BEE_CORE_RANGE * BEE_CORE_RANGE) continue;
            if (!ally.hasLineOfSight(source) && ally.distanceToSqr(source) > 36.0D) continue;

            MobMindData.rememberThreat(ally, threat, 18, level);
            MobMindData.addState(ally, StateType.RAGE, 1);
            informed++;
        }
    }

    private static Optional<Vec3> groupCenter(Mob mob, ServerLevel level, SpeciesProfile profile) {
        int social = MobMindData.getAttribute(mob, AttributeType.SOCIABILITY);
        if (social < 45) return Optional.empty();
        double radius = 5.0D + social * 0.08D;
        List<Mob> nearby = BehaviorUtil.nearbySameSpecies(mob, level, radius);
        ArrayList<LivingEntity> sample = new ArrayList<>();
        sample.add(mob);
        for (Mob other : nearby) {
            if (sample.size() >= MAX_GROUP_SAMPLE) break;
            if (other instanceof TamableAnimal tame && tame.isTame()) continue;
            sample.add(other);
        }
        if (sample.size() < 2) return Optional.empty();
        return Optional.of(BehaviorUtil.centroid(sample, mob.position()));
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

    static boolean isLoadedHive(ServerLevel level, BlockPos pos) {
        return pos != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof BeehiveBlockEntity;
    }

    /** Activity gate used only for optional idle overlays; vanilla state machines always keep ticking. */
    public static boolean allowsIdleOverlay(ActivityPattern pattern, long rawDayTime) {
        long day = Math.floorMod(rawDayTime, 24000L);
        return switch (pattern) {
            case DIURNAL -> day < 13000L || day > 22500L;
            case NOCTURNAL -> day > 10500L;
            case VARIABLE -> true;
        };
    }
}
