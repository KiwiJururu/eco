package com.livingecology.ai;

import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Explicit vanilla-ownership audit for issue #9's common passive land animals. */
public record PassiveLandSpeciesPolicy(VanillaOwner vanillaOwner, OverlayMode overlayMode,
                                       boolean acceptsSocialAlarm) {
    public enum VanillaOwner {
        SHEEP_WOOL_SHEARING_AND_GRAZING,
        PIG_SADDLE_BOOST_AND_RIDING,
        CHICKEN_EGG_FLAP_AND_JOCKEY,
        RABBIT_HOP_GARDEN_AND_KILLER_VARIANT,
        HORSE_TAMING_RIDING_ARMOR_AND_BREEDING,
        DONKEY_TAMING_CHEST_RIDING_AND_HYBRID_BREEDING,
        MULE_TAMING_CHEST_AND_RIDING,
        CAMEL_BRAIN_SITTING_DASH_AND_RIDING,
        GOAT_BRAIN_RAM_LONG_JUMP_AND_HORNS,
        LLAMA_TAMING_CHEST_CARAVAN_AND_SPIT,
        TRADER_LLAMA_TRADER_LEASH_DEFENSE_AND_DESPAWN,
        SNIFFER_BRAIN_SNIFF_SEARCH_DIG_AND_SEED
    }

    public enum OverlayMode {
        IDLE_HERD_COHESION,
        RABBIT_HOP_SAFE_HOME_RANGE,
        WILD_HORSE_HOME_RANGE,
        CAMEL_BRAIN_OBSERVATION,
        GOAT_BRAIN_OBSERVATION,
        LLAMA_CARAVAN_SAFE_HOME_RANGE,
        TRADER_LIFECYCLE_OBSERVATION,
        SNIFFER_BRAIN_OBSERVATION
    }

    private static final EnumMap<SpeciesType, PassiveLandSpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.SHEEP, VanillaOwner.SHEEP_WOOL_SHEARING_AND_GRAZING,
                OverlayMode.IDLE_HERD_COHESION, true);
        put(SpeciesType.PIG, VanillaOwner.PIG_SADDLE_BOOST_AND_RIDING,
                OverlayMode.IDLE_HERD_COHESION, true);
        put(SpeciesType.CHICKEN, VanillaOwner.CHICKEN_EGG_FLAP_AND_JOCKEY,
                OverlayMode.IDLE_HERD_COHESION, true);
        put(SpeciesType.RABBIT, VanillaOwner.RABBIT_HOP_GARDEN_AND_KILLER_VARIANT,
                OverlayMode.RABBIT_HOP_SAFE_HOME_RANGE, true);
        put(SpeciesType.HORSE, VanillaOwner.HORSE_TAMING_RIDING_ARMOR_AND_BREEDING,
                OverlayMode.WILD_HORSE_HOME_RANGE, true);
        put(SpeciesType.DONKEY, VanillaOwner.DONKEY_TAMING_CHEST_RIDING_AND_HYBRID_BREEDING,
                OverlayMode.WILD_HORSE_HOME_RANGE, true);
        put(SpeciesType.MULE, VanillaOwner.MULE_TAMING_CHEST_AND_RIDING,
                OverlayMode.WILD_HORSE_HOME_RANGE, true);
        put(SpeciesType.CAMEL, VanillaOwner.CAMEL_BRAIN_SITTING_DASH_AND_RIDING,
                OverlayMode.CAMEL_BRAIN_OBSERVATION, true);
        put(SpeciesType.GOAT, VanillaOwner.GOAT_BRAIN_RAM_LONG_JUMP_AND_HORNS,
                OverlayMode.GOAT_BRAIN_OBSERVATION, true);
        put(SpeciesType.LLAMA, VanillaOwner.LLAMA_TAMING_CHEST_CARAVAN_AND_SPIT,
                OverlayMode.LLAMA_CARAVAN_SAFE_HOME_RANGE, true);
        put(SpeciesType.TRADER_LLAMA, VanillaOwner.TRADER_LLAMA_TRADER_LEASH_DEFENSE_AND_DESPAWN,
                OverlayMode.TRADER_LIFECYCLE_OBSERVATION, false);
        put(SpeciesType.SNIFFER, VanillaOwner.SNIFFER_BRAIN_SNIFF_SEARCH_DIG_AND_SEED,
                OverlayMode.SNIFFER_BRAIN_OBSERVATION, true);
    }

    public static Optional<PassiveLandSpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static PassiveLandSpeciesPolicy require(SpeciesType species) {
        PassiveLandSpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) throw new IllegalArgumentException("Species is outside passive land batch: " + species);
        return policy;
    }

    public static Set<SpeciesType> species() {
        return Set.copyOf(POLICIES.keySet());
    }

    public static Map<SpeciesType, PassiveLandSpeciesPolicy> all() {
        return Map.copyOf(POLICIES);
    }

    private static void put(SpeciesType species, VanillaOwner owner, OverlayMode overlay,
                            boolean acceptsSocialAlarm) {
        POLICIES.put(species, new PassiveLandSpeciesPolicy(owner, overlay, acceptsSocialAlarm));
    }
}
