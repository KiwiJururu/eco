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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Vanilla-safe passive land overlay; all movement is idle-only and domain-checked. */
public final class PassiveLandBehavior {
    private static final int MAX_GROUP_SAMPLE = 12;

    private PassiveLandBehavior() {}

    public static void tick(Mob mob, ServerLevel level) {
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return;
        PassiveLandSpeciesPolicy policy = PassiveLandSpeciesPolicy.of(species).orElse(null);
        if (policy == null) return;

        SpeciesProfile profile = SpeciesProfile.of(species);
        TerritoryRecord territory = TerritoryManager.ensureTerritory(mob, level);
        EnvironmentSnapshot environment = EnvironmentManager.snapshot(level, mob.blockPosition(), species);
        if (environment.habitability() < 25) MobMindData.addState(mob, StateType.STRESS, 1);
        MobMindData.setResting(mob, false);

        switch (policy.overlayMode()) {
            case IDLE_HERD_COHESION -> idleHerd((Animal) mob, level, profile, territory);
            case RABBIT_HOP_SAFE_HOME_RANGE -> rabbit((Rabbit) mob, level, profile, territory);
            case WILD_HORSE_HOME_RANGE -> wildHorse((AbstractHorse) mob, level, profile, territory);
            case CAMEL_BRAIN_OBSERVATION -> observeCamel((Camel) mob);
            case GOAT_BRAIN_OBSERVATION -> observeGoat((Goat) mob);
            case LLAMA_CARAVAN_SAFE_HOME_RANGE -> llama((Llama) mob, level, profile, territory);
            case TRADER_LIFECYCLE_OBSERVATION -> observeTraderLlama((TraderLlama) mob);
            case SNIFFER_BRAIN_OBSERVATION -> observeSniffer((Sniffer) mob);
        }
    }

    private static void idleHerd(Animal animal, ServerLevel level, SpeciesProfile profile,
                                 TerritoryRecord territory) {
        if (animal.isPassenger() || animal.isLeashed() || animal.isInLove()
                || !animal.getNavigation().isDone()) return;
        if (animal instanceof Pig pig && pig.isVehicle()) return;
        if (animal instanceof Chicken chicken && chicken.isChickenJockey()) return;
        if (animal instanceof Sheep sheep && sheep.getHeadEatPositionScale(0.0F) > 0.0F) return;

        LivingEntity threat = perceivedThreat(animal, level);
        if (threat != null && MobMindData.getState(animal, StateType.FEAR) >= 2) {
            orderEscape(animal, level, profile, threat);
            return;
        }
        if (!allowsIdleOverlay(profile.activityPattern(), level.getDayTime())) return;

        Optional<Vec3> center = groupCenter(animal, level);
        if (center.isPresent() && animal.distanceToSqr(center.get()) > 64.0D
                && validLandTarget(level, center.get())) {
            move(animal, center.get(), 0.88D);
            return;
        }
        returnHomeIfFarOutside(animal, level, profile, territory, 0.84D);
    }

    private static void rabbit(Rabbit rabbit, ServerLevel level, SpeciesProfile profile,
                               TerritoryRecord territory) {
        if (rabbit.getVariant() == Rabbit.Variant.EVIL
                || BehaviorUtil.isValidCombatTarget(rabbit.getTarget())) return;
        if (rabbit.isPassenger() || rabbit.isLeashed() || rabbit.isInLove()
                || !rabbit.getNavigation().isDone()) return;

        LivingEntity threat = perceivedThreat(rabbit, level);
        if (threat != null && MobMindData.getState(rabbit, StateType.FEAR) >= 1) {
            orderEscape(rabbit, level, profile, threat);
            return;
        }
        if (allowsIdleOverlay(profile.activityPattern(), level.getDayTime())) {
            returnHomeIfFarOutside(rabbit, level, profile, territory, 0.90D);
        }
    }

    private static void wildHorse(AbstractHorse horse, ServerLevel level, SpeciesProfile profile,
                                  TerritoryRecord territory) {
        if (horse.isTamed()) {
            MobMindData.setTerritoryId(horse, 0L);
            return;
        }
        if (horse.isVehicle() || horse.isPassenger() || horse.isLeashed() || horse.isInLove()
                || horse.isEating() || horse.isStanding() || !horse.getNavigation().isDone()) return;

        LivingEntity threat = perceivedThreat(horse, level);
        if (threat != null && MobMindData.getState(horse, StateType.FEAR) >= 2) {
            orderEscape(horse, level, profile, threat);
            return;
        }
        if (!allowsIdleOverlay(profile.activityPattern(), level.getDayTime())) return;
        Optional<Vec3> center = groupCenter(horse, level);
        if (center.isPresent() && horse.distanceToSqr(center.get()) > 100.0D
                && validLandTarget(level, center.get())) {
            move(horse, center.get(), 0.90D);
            return;
        }
        returnHomeIfFarOutside(horse, level, profile, territory, 0.86D);
    }

    private static void llama(Llama llama, ServerLevel level, SpeciesProfile profile,
                              TerritoryRecord territory) {
        if (llama.isTamed()) {
            MobMindData.setTerritoryId(llama, 0L);
            return;
        }
        if (llama.inCaravan() || llama.hasCaravanTail()
                || BehaviorUtil.isValidCombatTarget(llama.getTarget())) return;
        wildHorse(llama, level, profile, territory);
    }

    private static void observeCamel(Camel camel) {
        // CamelAi owns panic/idle/follow/love movement; sitting, pose transitions and dash must not
        // be translated into generic AbstractHorse orders. Camel.isTamed() is always true by vanilla
        // design and does not mean it has a player owner.
        if (camel.isPanicking()) MobMindData.addState(camel, StateType.FEAR, 1);
    }

    private static void observeGoat(Goat goat) {
        // GoatAi owns ram preparation, ram target, long jump and horn collision.
        if (goat.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.RAM_TARGET)) {
            MobMindData.addState(goat, StateType.RAGE, 1);
        }
    }

    private static void observeTraderLlama(TraderLlama llama) {
        // Trader leash/defense/despawn goals are lifecycle-critical and receive no movement overlay.
        MobMindData.setTerritoryId(llama, 0L);
    }

    private static void observeSniffer(Sniffer sniffer) {
        // SnifferAi owns sniff/search/dig targets and explored-position memory.
        if (sniffer.isPanicking()) MobMindData.addState(sniffer, StateType.FEAR, 1);
    }

    private static void orderEscape(Mob mob, ServerLevel level, SpeciesProfile profile,
                                    LivingEntity threat) {
        Vec3 socialBias = groupCenter(mob, level).orElse(null);
        Vec3 target = BehaviorUtil.targetAwayForDomain(mob, level, profile,
                threat.position(), 7.0D + MobMindData.getState(mob, StateType.FEAR), socialBias);
        if (target.distanceToSqr(mob.position()) >= 1.0D) move(mob, target, 1.06D);
    }

    private static void returnHomeIfFarOutside(Mob mob, ServerLevel level, SpeciesProfile profile,
                                               TerritoryRecord territory, double speed) {
        if (territory == null || TerritoryManager.zoneFor(territory, mob.blockPosition()) != TerritoryZone.OUTSIDE)
            return;
        Vec3 core = Vec3.atCenterOf(territory.core());
        if (mob.distanceToSqr(core) <= 32.0D * 32.0D) return;

        if (validLandTarget(level, core)) {
            move(mob, core, speed);
            return;
        }
        BehaviorUtil.safePatrolPoint(mob, level, profile, core, 6.0D)
                .ifPresent(target -> move(mob, target, speed));
    }

    private static Optional<Vec3> groupCenter(Mob mob, ServerLevel level) {
        int social = MobMindData.getAttribute(mob, AttributeType.SOCIABILITY);
        if (social < 45) return Optional.empty();
        double radius = 5.0D + social * 0.08D;
        List<Mob> nearby = BehaviorUtil.nearbySameSpecies(mob, level, radius);
        ArrayList<LivingEntity> sample = new ArrayList<>();
        sample.add(mob);
        for (Mob other : nearby) {
            if (sample.size() >= MAX_GROUP_SAMPLE) break;
            if (isPlayerOwned(other)) continue;
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

    private static boolean isPlayerOwned(Mob mob) {
        if (mob instanceof TamableAnimal tame && tame.isTame()) return true;
        return mob instanceof AbstractHorse horse && !(horse instanceof Camel) && horse.isTamed();
    }

    private static boolean validLandTarget(ServerLevel level, Vec3 target) {
        BlockPos pos = BlockPos.containing(target);
        return level.hasChunkAt(pos) && BehaviorUtil.isSafeLand(level, pos);
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
