package com.livingecology.environment;

import com.livingecology.data.BehaviorFamily;
import com.livingecology.data.SpeciesProfile;
import net.minecraft.util.Mth;

/** Pure deterministic ecology formulas; intentionally separated so CI can unit-test them without a world. */
public final class EcologyMath {
    private EcologyMath() {}

    public static int carryingCapacity(SpeciesProfile profile, int radiusChunks, int habitability) {
        int h = Mth.clamp(habitability, 0, 100);
        int radius = Math.max(1, radiusChunks);
        double density = switch (profile.behaviorFamily()) {
            case AQUATIC -> 2.4D;
            case PASSIVE_HERD -> 1.8D;
            case PASSIVE_WANDERER, FLYING_PASSIVE, SPECIAL_MOUNT -> 1.35D;
            case PREDATOR -> 0.75D;
            case COLONY, ARTHROPOD -> 2.2D;
            case UNDEAD_HORDE -> 2.5D;
            case UNDEAD_COMBAT, HOSTILE_MELEE, HOSTILE_RANGED, FLYING_HOSTILE, EXPLOSIVE -> 1.25D;
            case SOCIETY_PEACEFUL, SOCIETY_HOSTILE, GUARDIAN -> 2.0D;
            case BOSS -> 0.25D;
        };
        double areaScale = Math.max(1.0D, radius * radius * 0.55D);
        double habitatScale = 0.35D + h / 100.0D;
        return Mth.clamp((int) Math.round(1.0D + density * areaScale * habitatScale), 1, 96);
    }

    public static int pressureTarget(int population, int maturity, int habitability, boolean abandoned) {
        int target = Mth.clamp(18 + Math.max(0, population) * 5 + Mth.clamp(maturity, 0, 100) / 3
                + (Mth.clamp(habitability, 0, 100) - 50) / 3, 0, 100);
        return abandoned ? Math.min(target, 10) : target;
    }

    public static int desiredRadius(SpeciesProfile profile, int currentRadius, int population, int habitability) {
        int max = Math.max(1, profile.maxTerritoryRadius());
        if (max <= 0) return 0;
        int current = Mth.clamp(currentRadius, 1, max);
        int capacity = carryingCapacity(profile, current, habitability);
        if (habitability >= 48 && population >= Math.max(2, (int) Math.floor(capacity * 0.78D)) && current < max) {
            return current + 1;
        }
        if ((population <= Math.max(1, capacity / 5) || habitability < 25) && current > 2) {
            return current - 1;
        }
        return current;
    }

    public static boolean reproductionAllowed(int population, int capacity, int habitability, int stability, int resources) {
        return population >= 2
                && population < Math.max(2, capacity)
                && habitability >= 52
                && stability >= 42
                && resources >= 38;
    }
}
