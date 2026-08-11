package com.livingecology.data;

/** Rare individual traits. They modify existing capabilities; they never grant a species an alien repertoire. */
public enum UniqueTrait {
    NONE("Nenhum"),
    SURVIVOR("Sobrevivente"),
    VIGILANT("Vigilante"),
    NOMAD("Nômade"),
    GUARDIAN("Guardião"),
    ASTUTE("Astuto"),
    SHY("Arisco"),
    FRIENDLY("Amigável"),
    VETERAN("Veterano"),
    ROBUST("Robusto"),
    MATRIARCH("Matriarca"),
    PACK_VETERAN("Veterano da Matilha"),
    AMBUSHER("Emboscadora"),
    HORDE_BEARER("Portador da Horda"),
    SENTINEL("Sentinela"),
    COMMUNITY_ELDER("Ancião Comunitário");

    private final String displayName;

    UniqueTrait(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }

    public static UniqueTrait defaultFor(SpeciesType species) {
        if (species == SpeciesType.COW) return MATRIARCH;
        if (species == SpeciesType.WOLF) return PACK_VETERAN;
        if (species == SpeciesType.SPIDER || species == SpeciesType.CAVE_SPIDER) return AMBUSHER;
        if (species == SpeciesType.ZOMBIE || species == SpeciesType.ZOMBIE_VILLAGER
                || species == SpeciesType.HUSK || species == SpeciesType.DROWNED || species == SpeciesType.GIANT)
            return HORDE_BEARER;
        if (species == SpeciesType.BEE) return SENTINEL;
        if (species == SpeciesType.VILLAGER || species == SpeciesType.WANDERING_TRADER) return COMMUNITY_ELDER;

        SpeciesProfile profile = SpeciesProfile.of(species);
        return switch (profile.behaviorFamily()) {
            case PASSIVE_HERD, PASSIVE_WANDERER, AQUATIC, FLYING_PASSIVE, SPECIAL_MOUNT -> SURVIVOR;
            case PREDATOR, COLONY, SOCIETY_PEACEFUL, GUARDIAN -> VETERAN;
            case ARTHROPOD -> AMBUSHER;
            case UNDEAD_HORDE -> HORDE_BEARER;
            case SOCIETY_HOSTILE -> VETERAN;
            case UNDEAD_COMBAT, HOSTILE_MELEE, HOSTILE_RANGED, FLYING_HOSTILE, EXPLOSIVE, BOSS -> ROBUST;
        };
    }
}
