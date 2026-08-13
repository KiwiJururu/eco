package com.livingecology.ai;

import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Explicit vanilla-ownership and society audit for issue #15. */
public record PiglinSocietySpeciesPolicy(VanillaOwner vanillaOwner,
                                         OverlayMode overlayMode,
                                         SocietyRole societyRole,
                                         KnowledgeGroup knowledgeGroup,
                                         MovementDomain expectedDomain,
                                         boolean livingSociety,
                                         boolean persistentEcology) {
    public enum VanillaOwner {
        PIGLIN_BRAIN_BARTER_GOLD_FEAR_HUNT_DANCE_AND_COMBAT,
        PIGLIN_BRUTE_BRAIN_BASTION_GUARD_AND_COMBAT,
        ZOMBIFIED_PIGLIN_PERSISTENT_ANGER_AND_UNDEAD_ALERT
    }

    public enum OverlayMode {
        PIGLIN_BRAIN_OBSERVATION,
        PIGLIN_BRUTE_BRAIN_OBSERVATION,
        ZOMBIFIED_PIGLIN_SEPARATE_UNDEAD_CONTROLLER
    }

    public enum SocietyRole {
        SOCIETY_MEMBER,
        BASTION_GUARD,
        ZOMBIFIED_OUTSIDER
    }

    public enum KnowledgeGroup {
        LIVING_PIGLINS,
        ZOMBIFIED_PIGLINS
    }

    private static final EnumMap<SpeciesType, PiglinSocietySpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.PIGLIN,
                VanillaOwner.PIGLIN_BRAIN_BARTER_GOLD_FEAR_HUNT_DANCE_AND_COMBAT,
                OverlayMode.PIGLIN_BRAIN_OBSERVATION,
                SocietyRole.SOCIETY_MEMBER, KnowledgeGroup.LIVING_PIGLINS,
                true, true);
        put(SpeciesType.PIGLIN_BRUTE,
                VanillaOwner.PIGLIN_BRUTE_BRAIN_BASTION_GUARD_AND_COMBAT,
                OverlayMode.PIGLIN_BRUTE_BRAIN_OBSERVATION,
                SocietyRole.BASTION_GUARD, KnowledgeGroup.LIVING_PIGLINS,
                true, true);
        put(SpeciesType.ZOMBIFIED_PIGLIN,
                VanillaOwner.ZOMBIFIED_PIGLIN_PERSISTENT_ANGER_AND_UNDEAD_ALERT,
                OverlayMode.ZOMBIFIED_PIGLIN_SEPARATE_UNDEAD_CONTROLLER,
                SocietyRole.ZOMBIFIED_OUTSIDER, KnowledgeGroup.ZOMBIFIED_PIGLINS,
                false, true);
    }

    public static Optional<PiglinSocietySpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static PiglinSocietySpeciesPolicy require(SpeciesType species) {
        PiglinSocietySpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) throw new IllegalArgumentException(
                "Species is outside Piglin society batch: " + species);
        return policy;
    }

    public static Set<SpeciesType> species() {
        return Set.copyOf(POLICIES.keySet());
    }

    public static Map<SpeciesType, PiglinSocietySpeciesPolicy> all() {
        return Map.copyOf(POLICIES);
    }

    private static void put(SpeciesType species, VanillaOwner owner, OverlayMode overlay,
                            SocietyRole role, KnowledgeGroup group,
                            boolean livingSociety, boolean persistentEcology) {
        POLICIES.put(species, new PiglinSocietySpeciesPolicy(owner, overlay, role, group,
                MovementDomain.LAND, livingSociety, persistentEcology));
    }
}
