package com.livingecology.data;

public enum UniqueTrait {
    NONE("Nenhum"),
    MATRIARCH("Matriarca"),
    PACK_VETERAN("Veterano da Matilha"),
    AMBUSHER("Emboscadora"),
    HORDE_BEARER("Portador da Horda"),
    ROBUST("Robusto");

    private final String displayName;

    UniqueTrait(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() { return displayName; }

    public static UniqueTrait defaultFor(SpeciesType species) {
        return switch (species) {
            case COW -> MATRIARCH;
            case WOLF -> PACK_VETERAN;
            case SPIDER -> AMBUSHER;
            case ZOMBIE -> HORDE_BEARER;
        };
    }
}
