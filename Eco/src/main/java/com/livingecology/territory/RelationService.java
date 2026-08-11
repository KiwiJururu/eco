package com.livingecology.territory;

import com.livingecology.data.SpeciesType;
import net.minecraft.util.Mth;

public final class RelationService {
    private RelationService() {}

    public static RelationshipProfile natural(SpeciesType from, SpeciesType to) {
        if (from == to) {
            return switch (from) {
                case COW -> new RelationshipProfile(0, 90, RelationKind.NEUTRAL);
                case WOLF -> new RelationshipProfile(60, 35, RelationKind.BORDERED);
                case SPIDER -> new RelationshipProfile(30, 25, RelationKind.BORDERED);
                case ZOMBIE -> new RelationshipProfile(5, 80, RelationKind.NEUTRAL);
            };
        }

        if (pair(from, to, SpeciesType.SPIDER, SpeciesType.ZOMBIE))
            return new RelationshipProfile(5, 70, RelationKind.SYMBIOTIC);
        if (pair(from, to, SpeciesType.WOLF, SpeciesType.ZOMBIE))
            return new RelationshipProfile(90, 0, RelationKind.WARLIKE);
        if (pair(from, to, SpeciesType.WOLF, SpeciesType.SPIDER))
            return new RelationshipProfile(35, 5, RelationKind.BORDERED);

        // Cows are deliberately a control species in the prototype.
        return new RelationshipProfile(0, 10, RelationKind.NEUTRAL);
    }

    private static boolean pair(SpeciesType a, SpeciesType b, SpeciesType x, SpeciesType y) {
        return (a == x && b == y) || (a == y && b == x);
    }

    public static int effectiveRivalry(TerritorySavedData data, TerritoryRecord a, TerritoryRecord b) {
        RelationRecord acquired = data.getRelation(a.id(), b.id());
        int delta = acquired == null ? 0 : acquired.rivalryDelta();
        return Mth.clamp(natural(a.species(), b.species()).rivalry() + delta, 0, 100);
    }

    public static int effectiveAffinity(TerritorySavedData data, TerritoryRecord a, TerritoryRecord b) {
        RelationRecord acquired = data.getRelation(a.id(), b.id());
        int delta = acquired == null ? 0 : acquired.affinityDelta();
        return Mth.clamp(natural(a.species(), b.species()).affinity() + delta, 0, 100);
    }

    public static int tension(TerritorySavedData data, TerritoryRecord a, TerritoryRecord b, boolean overlap) {
        int rivalry = effectiveRivalry(data, a, b);
        int affinity = effectiveAffinity(data, a, b);
        int pressure = Math.max(0, a.pressure() + b.pressure() - 110) / 3;
        int overlapBonus = overlap ? 20 : 0;
        int corePressure = a.center().closerThan(b.center(), 48.0D) ? 10 : 0;
        return Mth.clamp(rivalry + overlapBonus + pressure + corePressure - affinity / 2, 0, 100);
    }

    public static int cooperation(TerritorySavedData data, TerritoryRecord a, TerritoryRecord b) {
        int affinity = effectiveAffinity(data, a, b);
        int rivalry = effectiveRivalry(data, a, b);
        return Mth.clamp(affinity - rivalry / 3, 0, 100);
    }
}
