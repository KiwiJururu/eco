package com.livingecology.environment;

import com.livingecology.data.MobMindData;
import com.livingecology.data.ReproductionMode;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ecological reproduction coordinator. It uses vanilla love/breeding rather than spawning babies
 * directly, so species-specific child creation remains Minecraft's responsibility.
 */
public final class ReproductionManager {
    private ReproductionManager() {}

    public static void tickLevel(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 100L != 0L || level.players().isEmpty()) return;

        Set<Integer> handled = new HashSet<>();
        level.players().forEach(player -> triggerEligiblePairs(level, player.getBoundingBox().inflate(64.0D), handled));
    }

    /**
     * Runs one bounded reproduction evaluation. This is intentionally deterministic enough to be
     * used by GameTests and debug tooling without requiring a fake global player list.
     *
     * @return number of pairs that entered vanilla love mode.
     */
    public static int triggerEligiblePairs(ServerLevel level, AABB bounds) {
        return triggerEligiblePairs(level, bounds, new HashSet<>());
    }

    private static int triggerEligiblePairs(ServerLevel level, AABB bounds, Set<Integer> handled) {
        int startedPairs = 0;
        List<Animal> candidates = level.getEntitiesOfClass(Animal.class, bounds,
                ReproductionManager::isEligibleBase);
        for (Animal first : candidates) {
            if (!handled.add(first.getId())) continue;
            SpeciesType species = SpeciesType.from(first).orElse(null);
            if (species == null) continue;
            SpeciesProfile profile = SpeciesProfile.of(species);
            if (profile.reproductionMode() != ReproductionMode.VANILLA_LOVE) continue;

            MobMindData.initialize(first, level);
            if (!MobMindData.reproductionDue(first, level)) continue;
            TerritoryRecord territory = TerritoryManager.ensureTerritory(first, level);
            if (territory == null) {
                MobMindData.scheduleNextReproduction(first, level, 1800L, TerritoryManager.simulationScale(level));
                continue;
            }

            EnvironmentSnapshot env = EnvironmentManager.snapshot(level, territory.center(), species);
            int capacity = EcologyMath.carryingCapacity(profile, territory.radiusChunks(), env.habitability());
            if (!EcologyMath.reproductionAllowed(territory.population(), capacity,
                    env.habitability(), env.stability(), env.resources())) {
                MobMindData.scheduleNextReproduction(first, level, 2400L, TerritoryManager.simulationScale(level));
                continue;
            }

            Animal mate = candidates.stream()
                    .filter(other -> other != first && !handled.contains(other.getId()))
                    .filter(other -> species.matches(other))
                    .filter(ReproductionManager::isEligibleBase)
                    .filter(other -> first.distanceToSqr(other) <= 14.0D * 14.0D)
                    .filter(other -> {
                        MobMindData.initialize(other, level);
                        return MobMindData.reproductionDue(other, level);
                    })
                    .filter(other -> {
                        long otherTerritory = MobMindData.territoryId(other);
                        return otherTerritory == 0L || otherTerritory == territory.id();
                    })
                    .min((a, b) -> Double.compare(first.distanceToSqr(a), first.distanceToSqr(b)))
                    .orElse(null);

            if (mate == null) {
                MobMindData.scheduleNextReproduction(first, level, 800L, TerritoryManager.simulationScale(level));
                continue;
            }

            handled.add(mate.getId());
            MobMindData.setTerritoryId(mate, territory.id());
            first.setInLove(null);
            mate.setInLove(null);
            double scale = TerritoryManager.simulationScale(level);
            MobMindData.scheduleNextReproduction(first, level, 12000L, scale);
            MobMindData.scheduleNextReproduction(mate, level, 12000L, scale);
            startedPairs++;
        }
        return startedPairs;
    }

    /** Shared base gate kept public for integration/debug checks of vanilla ownership states. */
    public static boolean isEligibleBase(Animal animal) {
        if (!animal.isAlive() || animal.isBaby() || animal.isInLove()) return false;
        if (animal.isPassenger() || animal.isVehicle() || animal.isLeashed()
                || animal.isPersistenceRequired()) return false;
        if (animal instanceof TamableAnimal tame && tame.isTame()) return false;
        // Camel overrides isTamed() to always return true because it has no taming/owner phase.
        if (animal instanceof AbstractHorse horse && !(horse instanceof Camel) && horse.isTamed()) return false;
        // Hoglin disables vanilla love while pacified by a nearby repellent. Calling setInLove
        // directly without this species gate would bypass that vanilla Brain restriction.
        if (animal instanceof Hoglin hoglin && !hoglin.canFallInLove()) return false;
        return MobMindData.supports(animal);
    }
}
