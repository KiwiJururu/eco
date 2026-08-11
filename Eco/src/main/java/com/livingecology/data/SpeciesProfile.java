package com.livingecology.data;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SpeciesProfile {
    public record IntRange(int min, int max) {
        public IntRange {
            if (min < 0 || max > 100 || min > max) throw new IllegalArgumentException("Faixa inválida: " + min + "-" + max);
        }
    }

    private final SpeciesType species;
    private final EnumMap<AttributeType, IntRange> attributes;
    private final List<Personality> personalities;
    private final boolean bossEligible;
    private final int territoryFormationMinimum;
    private final int territorySearchRadius;

    private SpeciesProfile(SpeciesType species,
                           Map<AttributeType, IntRange> attributes,
                           List<Personality> personalities,
                           boolean bossEligible,
                           int territoryFormationMinimum,
                           int territorySearchRadius) {
        this.species = species;
        this.attributes = new EnumMap<>(attributes);
        this.personalities = List.copyOf(personalities);
        this.bossEligible = bossEligible;
        this.territoryFormationMinimum = territoryFormationMinimum;
        this.territorySearchRadius = territorySearchRadius;
    }

    public SpeciesType species() { return species; }
    public IntRange range(AttributeType type) { return attributes.get(type); }
    public List<Personality> personalities() { return personalities; }
    public boolean bossEligible() { return bossEligible; }
    public int territoryFormationMinimum() { return territoryFormationMinimum; }
    public int territorySearchRadius() { return territorySearchRadius; }

    public static SpeciesProfile of(SpeciesType species) {
        return switch (species) {
            case COW -> cow();
            case WOLF -> wolf();
            case SPIDER -> spider();
            case ZOMBIE -> zombie();
        };
    }

    private static SpeciesProfile cow() {
        EnumMap<AttributeType, IntRange> a = base(
                30, 50, 45, 65, 55, 70, 45, 65, 80, 95, 55, 70, 55, 70);
        return new SpeciesProfile(SpeciesType.COW, a,
                List.of(Personality.NORMAL, Personality.LAZY, Personality.WORRIED, Personality.SOCIABLE, Personality.CAUTIOUS, Personality.BOLD),
                false, 3, 28);
    }

    private static SpeciesProfile wolf() {
        EnumMap<AttributeType, IntRange> a = base(
                60, 80, 75, 95, 80, 95, 70, 90, 85, 100, 85, 100, 55, 75);
        return new SpeciesProfile(SpeciesType.WOLF, a,
                List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.BOLD, Personality.PROTECTIVE, Personality.SOCIABLE, Personality.AGGRESSIVE),
                true, 2, 36);
    }

    private static SpeciesProfile spider() {
        EnumMap<AttributeType, IntRange> a = base(
                45, 65, 35, 55, 75, 90, 70, 90, 20, 40, 80, 95, 40, 55);
        return new SpeciesProfile(SpeciesType.SPIDER, a,
                List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.AGGRESSIVE, Personality.INDEPENDENT),
                true, 3, 28);
    }

    private static SpeciesProfile zombie() {
        EnumMap<AttributeType, IntRange> a = base(
                10, 30, 15, 35, 50, 70, 55, 75, 75, 95, 45, 60, 65, 80);
        return new SpeciesProfile(SpeciesType.ZOMBIE, a,
                List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN, Personality.SOCIABLE),
                true, 5, 32);
    }

    private static EnumMap<AttributeType, IntRange> base(
            int t0, int t1, int m0, int m1, int p0, int p1, int a0, int a1,
            int s0, int s1, int i0, int i1, int c0, int c1) {
        EnumMap<AttributeType, IntRange> map = new EnumMap<>(AttributeType.class);
        map.put(AttributeType.TERRITORY, new IntRange(t0, t1));
        map.put(AttributeType.MEMORY, new IntRange(m0, m1));
        map.put(AttributeType.PERCEPTION, new IntRange(p0, p1));
        map.put(AttributeType.ALERT, new IntRange(a0, a1));
        map.put(AttributeType.SOCIABILITY, new IntRange(s0, s1));
        map.put(AttributeType.INSTINCT, new IntRange(i0, i1));
        map.put(AttributeType.CONSTITUTION, new IntRange(c0, c1));
        return map;
    }
}
