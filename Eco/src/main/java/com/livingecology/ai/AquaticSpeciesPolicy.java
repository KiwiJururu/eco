package com.livingecology.ai;

import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Explicit ownership audit for the aquatic/amphibious batch.
 *
 * <p>The policy prevents a broad family controller from taking navigation away from vanilla
 * state machines. It also documents where Living Ecology may safely add local social behavior.</p>
 */
public record AquaticSpeciesPolicy(VanillaOwner vanillaOwner, OverlayMode overlayMode,
                                   boolean acceptsSocialAlarm) {
    public enum VanillaOwner {
        SCHOOLING_FISH_GOALS,
        PUFFERFISH_DEFENSE_GOAL,
        SQUID_VECTOR_GOALS,
        DOLPHIN_GOALS,
        AXOLOTL_BRAIN,
        TURTLE_HOME_AND_EGG_GOALS,
        TADPOLE_BRAIN_AND_METAMORPHOSIS,
        FROG_BRAIN
    }

    public enum OverlayMode {
        VANILLA_SCHOOL_THREAT_ONLY,
        PUFFER_DEFENSE_ONLY,
        SQUID_VECTOR_COHESION,
        IDLE_NAVIGATION_COHESION,
        BRAIN_IDLE_NAVIGATION_COHESION,
        VANILLA_OBSERVATION_ONLY
    }

    private static final EnumMap<SpeciesType, AquaticSpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.COD, VanillaOwner.SCHOOLING_FISH_GOALS,
                OverlayMode.VANILLA_SCHOOL_THREAT_ONLY, true);
        put(SpeciesType.SALMON, VanillaOwner.SCHOOLING_FISH_GOALS,
                OverlayMode.VANILLA_SCHOOL_THREAT_ONLY, true);
        put(SpeciesType.TROPICAL_FISH, VanillaOwner.SCHOOLING_FISH_GOALS,
                OverlayMode.VANILLA_SCHOOL_THREAT_ONLY, true);
        put(SpeciesType.PUFFERFISH, VanillaOwner.PUFFERFISH_DEFENSE_GOAL,
                OverlayMode.PUFFER_DEFENSE_ONLY, false);
        put(SpeciesType.SQUID, VanillaOwner.SQUID_VECTOR_GOALS,
                OverlayMode.SQUID_VECTOR_COHESION, true);
        put(SpeciesType.GLOW_SQUID, VanillaOwner.SQUID_VECTOR_GOALS,
                OverlayMode.SQUID_VECTOR_COHESION, true);
        put(SpeciesType.DOLPHIN, VanillaOwner.DOLPHIN_GOALS,
                OverlayMode.IDLE_NAVIGATION_COHESION, true);
        put(SpeciesType.AXOLOTL, VanillaOwner.AXOLOTL_BRAIN,
                OverlayMode.BRAIN_IDLE_NAVIGATION_COHESION, true);
        put(SpeciesType.TURTLE, VanillaOwner.TURTLE_HOME_AND_EGG_GOALS,
                OverlayMode.VANILLA_OBSERVATION_ONLY, false);
        put(SpeciesType.TADPOLE, VanillaOwner.TADPOLE_BRAIN_AND_METAMORPHOSIS,
                OverlayMode.BRAIN_IDLE_NAVIGATION_COHESION, true);
        put(SpeciesType.FROG, VanillaOwner.FROG_BRAIN,
                OverlayMode.VANILLA_OBSERVATION_ONLY, false);
    }

    public static Optional<AquaticSpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static AquaticSpeciesPolicy require(SpeciesType species) {
        AquaticSpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) throw new IllegalArgumentException("Species is outside aquatic batch: " + species);
        return policy;
    }

    public static Set<SpeciesType> species() {
        return Set.copyOf(POLICIES.keySet());
    }

    public static Map<SpeciesType, AquaticSpeciesPolicy> all() {
        return Map.copyOf(POLICIES);
    }

    private static void put(SpeciesType species, VanillaOwner owner, OverlayMode overlay,
                            boolean acceptsSocialAlarm) {
        POLICIES.put(species, new AquaticSpeciesPolicy(owner, overlay, acceptsSocialAlarm));
    }
}
