package com.livingecology.ai;

import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Vanilla-ownership and bounded influence policy for issue #17. */
public record GuardianSocietySpeciesPolicy(VanillaOwner vanillaOwner,
                                           MonumentRole monumentRole,
                                           MovementDomain expectedDomain,
                                           int memoryRange,
                                           int maxMemoryReceivers) {
    public enum VanillaOwner {
        GUARDIAN_BEAM_THORNS_SWIM_AND_TARGETING,
        ELDER_GUARDIAN_BEAM_THORNS_MINING_FATIGUE_SWIM_AND_TARGETING
    }

    public enum MonumentRole { SENTINEL, ELDER }

    private static final EnumMap<SpeciesType, GuardianSocietySpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        POLICIES.put(SpeciesType.GUARDIAN,
                new GuardianSocietySpeciesPolicy(
                        VanillaOwner.GUARDIAN_BEAM_THORNS_SWIM_AND_TARGETING,
                        MonumentRole.SENTINEL, MovementDomain.WATER, 16, 8));
        POLICIES.put(SpeciesType.ELDER_GUARDIAN,
                new GuardianSocietySpeciesPolicy(
                        VanillaOwner.ELDER_GUARDIAN_BEAM_THORNS_MINING_FATIGUE_SWIM_AND_TARGETING,
                        MonumentRole.ELDER, MovementDomain.WATER, 20, 12));
    }

    public static Optional<GuardianSocietySpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static GuardianSocietySpeciesPolicy require(SpeciesType species) {
        GuardianSocietySpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) throw new IllegalArgumentException("Species is outside Guardian batch: " + species);
        return policy;
    }

    public static Set<SpeciesType> species() { return Set.copyOf(POLICIES.keySet()); }
    public static Map<SpeciesType, GuardianSocietySpeciesPolicy> all() { return Map.copyOf(POLICIES); }
}
