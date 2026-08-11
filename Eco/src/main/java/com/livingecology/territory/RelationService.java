package com.livingecology.territory;

import com.livingecology.data.AttributeType;
import com.livingecology.data.BehaviorFamily;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import net.minecraft.util.Mth;

import java.util.EnumSet;
import java.util.Set;

/** Natural relationship matrix plus acquired per-territory deltas. */
public final class RelationService {
    private RelationService() {}

    private static final Set<SpeciesType> ZOMBIE_FAMILY = EnumSet.of(
            SpeciesType.ZOMBIE, SpeciesType.ZOMBIE_VILLAGER, SpeciesType.HUSK,
            SpeciesType.DROWNED, SpeciesType.GIANT);
    private static final Set<SpeciesType> SPIDER_FAMILY = EnumSet.of(SpeciesType.SPIDER, SpeciesType.CAVE_SPIDER);
    private static final Set<SpeciesType> ILLAGERS = EnumSet.of(
            SpeciesType.EVOKER, SpeciesType.PILLAGER, SpeciesType.VINDICATOR,
            SpeciesType.ILLUSIONER, SpeciesType.RAVAGER, SpeciesType.WITCH);
    private static final Set<SpeciesType> PIGLIN_SOCIETY = EnumSet.of(SpeciesType.PIGLIN, SpeciesType.PIGLIN_BRUTE);
    private static final Set<SpeciesType> GUARDIAN_SOCIETY = EnumSet.of(SpeciesType.GUARDIAN, SpeciesType.ELDER_GUARDIAN);

    public static RelationshipProfile natural(SpeciesType from, SpeciesType to) {
        if (from == to) {
            SpeciesProfile p = SpeciesProfile.of(from);
            int rivalry = Mth.clamp(p.midpoint(AttributeType.TERRITORY) / 2 - p.midpoint(AttributeType.SOCIABILITY) / 4, 0, 70);
            int affinity = Mth.clamp(30 + p.midpoint(AttributeType.SOCIABILITY) / 2, 25, 95);
            if (from == SpeciesType.WOLF) return new RelationshipProfile(60, 35, RelationKind.BORDERED);
            if (SPIDER_FAMILY.contains(from)) return new RelationshipProfile(30, 30, RelationKind.BORDERED);
            return new RelationshipProfile(rivalry, affinity, rivalry >= 45 ? RelationKind.BORDERED : RelationKind.NEUTRAL);
        }

        // Prototype anchor relationships retained exactly from the design discussion.
        if (oneInEach(from, to, SPIDER_FAMILY, ZOMBIE_FAMILY))
            return new RelationshipProfile(5, 70, RelationKind.SYMBIOTIC);
        if ((from == SpeciesType.WOLF && ZOMBIE_FAMILY.contains(to))
                || (to == SpeciesType.WOLF && ZOMBIE_FAMILY.contains(from)))
            return new RelationshipProfile(90, 0, RelationKind.WARLIKE);
        if ((from == SpeciesType.WOLF && SPIDER_FAMILY.contains(to))
                || (to == SpeciesType.WOLF && SPIDER_FAMILY.contains(from)))
            return new RelationshipProfile(35, 5, RelationKind.BORDERED);

        // Strong vanilla/community relationships.
        if (pair(from, to, SpeciesType.VILLAGER, SpeciesType.IRON_GOLEM))
            return new RelationshipProfile(0, 100, RelationKind.SYMBIOTIC);
        if (PIGLIN_SOCIETY.contains(from) && PIGLIN_SOCIETY.contains(to))
            return new RelationshipProfile(5, 90, RelationKind.SYMBIOTIC);
        if (GUARDIAN_SOCIETY.contains(from) && GUARDIAN_SOCIETY.contains(to))
            return new RelationshipProfile(0, 95, RelationKind.SYMBIOTIC);
        if (ILLAGERS.contains(from) && ILLAGERS.contains(to))
            return new RelationshipProfile(5, 85, RelationKind.SYMBIOTIC);
        if (ZOMBIE_FAMILY.contains(from) && ZOMBIE_FAMILY.contains(to))
            return new RelationshipProfile(5, 85, RelationKind.SYMBIOTIC);

        // High-conflict natural/vanilla oppositions.
        if ((ILLAGERS.contains(from) && (to == SpeciesType.VILLAGER || to == SpeciesType.IRON_GOLEM))
                || (ILLAGERS.contains(to) && (from == SpeciesType.VILLAGER || from == SpeciesType.IRON_GOLEM)))
            return new RelationshipProfile(95, 0, RelationKind.WARLIKE);
        if (pair(from, to, SpeciesType.ENDERMAN, SpeciesType.ENDERMITE))
            return new RelationshipProfile(100, 0, RelationKind.WARLIKE);
        if ((PIGLIN_SOCIETY.contains(from) && to == SpeciesType.WITHER_SKELETON)
                || (PIGLIN_SOCIETY.contains(to) && from == SpeciesType.WITHER_SKELETON))
            return new RelationshipProfile(85, 0, RelationKind.WARLIKE);
        if ((ZOMBIE_FAMILY.contains(from) && to == SpeciesType.IRON_GOLEM)
                || (ZOMBIE_FAMILY.contains(to) && from == SpeciesType.IRON_GOLEM))
            return new RelationshipProfile(90, 0, RelationKind.WARLIKE);

        // Similar territorial predators may coexist at distance but contest heavy overlap.
        BehaviorFamily a = SpeciesProfile.of(from).behaviorFamily();
        BehaviorFamily b = SpeciesProfile.of(to).behaviorFamily();
        if (a == BehaviorFamily.PREDATOR && b == BehaviorFamily.PREDATOR)
            return new RelationshipProfile(35, 5, RelationKind.BORDERED);

        // Most species are neither diplomatic allies nor sworn enemies. Acquired history can still move this.
        int tolerance = (SpeciesProfile.of(from).midpoint(AttributeType.SOCIABILITY)
                + SpeciesProfile.of(to).midpoint(AttributeType.SOCIABILITY)) / 8;
        return new RelationshipProfile(0, Mth.clamp(tolerance, 5, 25), RelationKind.NEUTRAL);
    }

    private static boolean pair(SpeciesType a, SpeciesType b, SpeciesType x, SpeciesType y) {
        return (a == x && b == y) || (a == y && b == x);
    }

    private static boolean oneInEach(SpeciesType a, SpeciesType b, Set<SpeciesType> x, Set<SpeciesType> y) {
        return (x.contains(a) && y.contains(b)) || (x.contains(b) && y.contains(a));
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
