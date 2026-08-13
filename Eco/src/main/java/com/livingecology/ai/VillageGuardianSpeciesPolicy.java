package com.livingecology.ai;

import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Explicit vanilla-ownership and community audit for issue #14. */
public record VillageGuardianSpeciesPolicy(VanillaOwner vanillaOwner,
                                           OverlayMode overlayMode,
                                           CommunityRole communityRole,
                                           KnowledgeGroup knowledgeGroup,
                                           MovementDomain expectedDomain,
                                           boolean persistentCommunity) {
    public enum VanillaOwner {
        VILLAGER_BRAIN_SCHEDULE_PROFESSION_GOSSIP_TRADE_AND_BREEDING,
        WANDERING_TRADER_TRADE_POTION_WANDER_TARGET_AND_DESPAWN,
        IRON_GOLEM_VILLAGE_DEFENSE_ANGER_ATTACK_FLOWER_AND_REPAIR,
        SNOW_GOLEM_RANGED_DEFENSE_PUMPKIN_SHEARING_AND_SNOW_TRAIL,
        TRADER_LLAMA_TRADER_LEASH_DEFENSE_SPIT_AND_DESPAWN
    }

    public enum OverlayMode {
        VILLAGER_BRAIN_OBSERVATION,
        WANDERING_TRADER_LIFECYCLE_OBSERVATION,
        IRON_GOLEM_DEFENSE_OBSERVATION,
        SNOW_GOLEM_DEFENSE_OBSERVATION,
        TRADER_LLAMA_CARAVAN_OBSERVATION
    }

    public enum CommunityRole {
        RESIDENT,
        TRANSIENT_TRADER,
        MELEE_GUARDIAN,
        RANGED_GUARDIAN,
        TRADER_COMPANION
    }

    public enum KnowledgeGroup {
        VILLAGE,
        TRADER_CARAVAN
    }

    private static final EnumMap<SpeciesType, VillageGuardianSpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.VILLAGER,
                VanillaOwner.VILLAGER_BRAIN_SCHEDULE_PROFESSION_GOSSIP_TRADE_AND_BREEDING,
                OverlayMode.VILLAGER_BRAIN_OBSERVATION, CommunityRole.RESIDENT,
                KnowledgeGroup.VILLAGE, true);
        put(SpeciesType.WANDERING_TRADER,
                VanillaOwner.WANDERING_TRADER_TRADE_POTION_WANDER_TARGET_AND_DESPAWN,
                OverlayMode.WANDERING_TRADER_LIFECYCLE_OBSERVATION,
                CommunityRole.TRANSIENT_TRADER, KnowledgeGroup.TRADER_CARAVAN, false);
        put(SpeciesType.IRON_GOLEM,
                VanillaOwner.IRON_GOLEM_VILLAGE_DEFENSE_ANGER_ATTACK_FLOWER_AND_REPAIR,
                OverlayMode.IRON_GOLEM_DEFENSE_OBSERVATION, CommunityRole.MELEE_GUARDIAN,
                KnowledgeGroup.VILLAGE, true);
        put(SpeciesType.SNOW_GOLEM,
                VanillaOwner.SNOW_GOLEM_RANGED_DEFENSE_PUMPKIN_SHEARING_AND_SNOW_TRAIL,
                OverlayMode.SNOW_GOLEM_DEFENSE_OBSERVATION, CommunityRole.RANGED_GUARDIAN,
                KnowledgeGroup.VILLAGE, true);
        put(SpeciesType.TRADER_LLAMA,
                VanillaOwner.TRADER_LLAMA_TRADER_LEASH_DEFENSE_SPIT_AND_DESPAWN,
                OverlayMode.TRADER_LLAMA_CARAVAN_OBSERVATION,
                CommunityRole.TRADER_COMPANION, KnowledgeGroup.TRADER_CARAVAN, false);
    }

    public static Optional<VillageGuardianSpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static VillageGuardianSpeciesPolicy require(SpeciesType species) {
        VillageGuardianSpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) throw new IllegalArgumentException(
                "Species is outside village/guardian batch: " + species);
        return policy;
    }

    public static Set<SpeciesType> species() {
        return Set.copyOf(POLICIES.keySet());
    }

    public static Map<SpeciesType, VillageGuardianSpeciesPolicy> all() {
        return Map.copyOf(POLICIES);
    }

    private static void put(SpeciesType species, VanillaOwner owner, OverlayMode overlay,
                            CommunityRole role, KnowledgeGroup group,
                            boolean persistentCommunity) {
        POLICIES.put(species, new VillageGuardianSpeciesPolicy(owner, overlay, role, group,
                MovementDomain.LAND, persistentCommunity));
    }
}
