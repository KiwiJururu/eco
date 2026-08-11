package com.livingecology.ai;

import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Explicit vanilla-ownership audit for issue #10's predators and independent animals. */
public record IndependentLandSpeciesPolicy(VanillaOwner vanillaOwner, OverlayMode overlayMode) {
    public enum VanillaOwner {
        FOX_TRUST_ITEM_SLEEP_STALK_POUNCE_AND_DEFENSE,
        OCELOT_TRUST_TEMPT_AVOID_AND_PREY,
        CAT_TAME_OWNER_SIT_BED_GIFT_AND_PREY,
        POLAR_BEAR_NEUTRAL_ANGER_CUB_PROTECTION_AND_STANDING,
        PANDA_GENE_BAMBOO_SIT_EAT_SNEEZE_ROLL_AND_BREEDING
    }

    public enum OverlayMode {
        FOX_STATE_SAFE_HOME_RANGE,
        OCELOT_TRUST_SAFE_HOME_RANGE,
        WILD_CAT_HOME_RANGE,
        POLAR_BEAR_NEUTRAL_OBSERVATION,
        PANDA_GENE_STATE_OBSERVATION
    }

    private static final EnumMap<SpeciesType, IndependentLandSpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.FOX, VanillaOwner.FOX_TRUST_ITEM_SLEEP_STALK_POUNCE_AND_DEFENSE,
                OverlayMode.FOX_STATE_SAFE_HOME_RANGE);
        put(SpeciesType.OCELOT, VanillaOwner.OCELOT_TRUST_TEMPT_AVOID_AND_PREY,
                OverlayMode.OCELOT_TRUST_SAFE_HOME_RANGE);
        put(SpeciesType.CAT, VanillaOwner.CAT_TAME_OWNER_SIT_BED_GIFT_AND_PREY,
                OverlayMode.WILD_CAT_HOME_RANGE);
        put(SpeciesType.POLAR_BEAR,
                VanillaOwner.POLAR_BEAR_NEUTRAL_ANGER_CUB_PROTECTION_AND_STANDING,
                OverlayMode.POLAR_BEAR_NEUTRAL_OBSERVATION);
        put(SpeciesType.PANDA, VanillaOwner.PANDA_GENE_BAMBOO_SIT_EAT_SNEEZE_ROLL_AND_BREEDING,
                OverlayMode.PANDA_GENE_STATE_OBSERVATION);
    }

    public static Optional<IndependentLandSpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static IndependentLandSpeciesPolicy require(SpeciesType species) {
        IndependentLandSpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) throw new IllegalArgumentException(
                "Species is outside terrestrial predator/independent batch: " + species);
        return policy;
    }

    public static Set<SpeciesType> species() {
        return Set.copyOf(POLICIES.keySet());
    }

    public static Map<SpeciesType, IndependentLandSpeciesPolicy> all() {
        return Map.copyOf(POLICIES);
    }

    private static void put(SpeciesType species, VanillaOwner owner, OverlayMode overlay) {
        POLICIES.put(species, new IndependentLandSpeciesPolicy(owner, overlay));
    }
}
