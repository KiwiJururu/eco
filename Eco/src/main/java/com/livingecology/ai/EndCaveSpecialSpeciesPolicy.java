package com.livingecology.ai;

import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Explicit vanilla-ownership and community audit for issue #13. */
public record EndCaveSpecialSpeciesPolicy(VanillaOwner vanillaOwner,
                                          ObservationMode observationMode,
                                          CommunityContract communityContract,
                                          MovementDomain expectedDomain,
                                          boolean sharesNestMemory) {
    public enum VanillaOwner {
        ENDERMAN_STARE_ANGER_TELEPORT_BLOCK_AND_ENDERMITE_TARGETING,
        ENDERMITE_LIFETIME_DESPAWN_AND_MELEE,
        SILVERFISH_INFEST_WAKE_FRIENDS_HIDE_AND_MELEE,
        SHULKER_ATTACHMENT_PEEK_TELEPORT_BULLETS_AND_DUPLICATION
    }

    public enum ObservationMode {
        ENDERMAN_WATER_AND_ANGER_OBSERVATION,
        ENDERMITE_TRANSIENT_OBSERVATION,
        SILVERFISH_NEST_MEMORY_OBSERVATION,
        SHULKER_STATIC_COMBAT_OBSERVATION
    }

    public enum CommunityContract {
        MOBILE_SOLITARY,
        TRANSIENT_SOLITARY,
        INFESTED_BLOCK_NEST,
        END_CITY_STRUCTURE
    }

    private static final EnumMap<SpeciesType, EndCaveSpecialSpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.ENDERMAN,
                VanillaOwner.ENDERMAN_STARE_ANGER_TELEPORT_BLOCK_AND_ENDERMITE_TARGETING,
                ObservationMode.ENDERMAN_WATER_AND_ANGER_OBSERVATION,
                CommunityContract.MOBILE_SOLITARY, MovementDomain.LAND, false);
        put(SpeciesType.ENDERMITE, VanillaOwner.ENDERMITE_LIFETIME_DESPAWN_AND_MELEE,
                ObservationMode.ENDERMITE_TRANSIENT_OBSERVATION,
                CommunityContract.TRANSIENT_SOLITARY, MovementDomain.LAND, false);
        put(SpeciesType.SILVERFISH, VanillaOwner.SILVERFISH_INFEST_WAKE_FRIENDS_HIDE_AND_MELEE,
                ObservationMode.SILVERFISH_NEST_MEMORY_OBSERVATION,
                CommunityContract.INFESTED_BLOCK_NEST, MovementDomain.LAND, true);
        put(SpeciesType.SHULKER,
                VanillaOwner.SHULKER_ATTACHMENT_PEEK_TELEPORT_BULLETS_AND_DUPLICATION,
                ObservationMode.SHULKER_STATIC_COMBAT_OBSERVATION,
                CommunityContract.END_CITY_STRUCTURE, MovementDomain.STATIC, false);
    }

    public static Optional<EndCaveSpecialSpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static EndCaveSpecialSpeciesPolicy require(SpeciesType species) {
        EndCaveSpecialSpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) throw new IllegalArgumentException(
                "Species is outside End/cave special batch: " + species);
        return policy;
    }

    public static Set<SpeciesType> species() {
        return Set.copyOf(POLICIES.keySet());
    }

    public static Map<SpeciesType, EndCaveSpecialSpeciesPolicy> all() {
        return Map.copyOf(POLICIES);
    }

    private static void put(SpeciesType species, VanillaOwner owner, ObservationMode observation,
                            CommunityContract community, MovementDomain domain,
                            boolean sharesNestMemory) {
        POLICIES.put(species, new EndCaveSpecialSpeciesPolicy(
                owner, observation, community, domain, sharesNestMemory));
    }
}
