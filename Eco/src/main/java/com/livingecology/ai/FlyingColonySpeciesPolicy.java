package com.livingecology.ai;

import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Explicit vanilla-ownership audit for issue #8's flying-passive and colony batch.
 *
 * <p>These species all use the air domain, but their movement contracts are very different. The
 * policy keeps that distinction explicit so a broad flying controller cannot accidentally replace
 * Bat hanging, Parrot ownership, Allay brain work, or Bee hive/pollination goals.</p>
 */
public record FlyingColonySpeciesPolicy(VanillaOwner vanillaOwner, OverlayMode overlayMode,
                                        boolean acceptsSocialAlarm) {
    public enum VanillaOwner {
        BAT_FLIGHT_AND_HANGING,
        PARROT_TAME_SHOULDER_AND_JUKEBOX_GOALS,
        ALLAY_ITEM_NOTE_BLOCK_AND_DANCE_BRAIN,
        BEE_HIVE_FLOWER_POLLINATION_AND_STING_GOALS
    }

    public enum OverlayMode {
        BAT_STATE_OBSERVATION,
        WILD_PARROT_IDLE_COHESION,
        ALLAY_BRAIN_OBSERVATION,
        BEE_HIVE_COLONY_MEMORY
    }

    private static final EnumMap<SpeciesType, FlyingColonySpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.BAT, VanillaOwner.BAT_FLIGHT_AND_HANGING,
                OverlayMode.BAT_STATE_OBSERVATION, true);
        put(SpeciesType.PARROT, VanillaOwner.PARROT_TAME_SHOULDER_AND_JUKEBOX_GOALS,
                OverlayMode.WILD_PARROT_IDLE_COHESION, true);
        put(SpeciesType.ALLAY, VanillaOwner.ALLAY_ITEM_NOTE_BLOCK_AND_DANCE_BRAIN,
                OverlayMode.ALLAY_BRAIN_OBSERVATION, false);
        put(SpeciesType.BEE, VanillaOwner.BEE_HIVE_FLOWER_POLLINATION_AND_STING_GOALS,
                OverlayMode.BEE_HIVE_COLONY_MEMORY, false);
    }

    public static Optional<FlyingColonySpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static FlyingColonySpeciesPolicy require(SpeciesType species) {
        FlyingColonySpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) {
            throw new IllegalArgumentException("Species is outside flying/colony batch: " + species);
        }
        return policy;
    }

    public static Set<SpeciesType> species() {
        return Set.copyOf(POLICIES.keySet());
    }

    public static Map<SpeciesType, FlyingColonySpeciesPolicy> all() {
        return Map.copyOf(POLICIES);
    }

    private static void put(SpeciesType species, VanillaOwner owner, OverlayMode overlay,
                            boolean acceptsSocialAlarm) {
        POLICIES.put(species, new FlyingColonySpeciesPolicy(owner, overlay, acceptsSocialAlarm));
    }
}
