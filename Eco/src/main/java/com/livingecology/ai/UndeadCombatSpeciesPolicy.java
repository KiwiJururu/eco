package com.livingecology.ai;

import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Explicit vanilla-combat ownership audit for issue #11. */
public record UndeadCombatSpeciesPolicy(VanillaOwner vanillaOwner, CombatMode combatMode,
                                        KnowledgeGroup knowledgeGroup, boolean daylightSensitive) {
    public enum VanillaOwner {
        SKELETON_BOW_STRAFE_MELEE_FALLBACK_AND_SUN_AVOIDANCE,
        STRAY_BOW_STRAFE_SLOW_ARROWS_MELEE_FALLBACK_AND_SUN_AVOIDANCE,
        WITHER_SKELETON_MELEE_WITHER_EFFECT_AND_NETHER_TARGET_GOALS,
        ZOMBIFIED_PIGLIN_NEUTRAL_ANGER_ALERT_AND_MELEE
    }

    public enum CombatMode {
        WEAPON_AWARE_SKELETON,
        MELEE,
        NEUTRAL_MELEE
    }

    public enum KnowledgeGroup {
        SKELETON_ARCHERS,
        WITHER_SKELETONS,
        ZOMBIFIED_PIGLINS
    }

    private static final EnumMap<SpeciesType, UndeadCombatSpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.SKELETON,
                VanillaOwner.SKELETON_BOW_STRAFE_MELEE_FALLBACK_AND_SUN_AVOIDANCE,
                CombatMode.WEAPON_AWARE_SKELETON, KnowledgeGroup.SKELETON_ARCHERS, true);
        put(SpeciesType.STRAY,
                VanillaOwner.STRAY_BOW_STRAFE_SLOW_ARROWS_MELEE_FALLBACK_AND_SUN_AVOIDANCE,
                CombatMode.WEAPON_AWARE_SKELETON, KnowledgeGroup.SKELETON_ARCHERS, true);
        put(SpeciesType.WITHER_SKELETON,
                VanillaOwner.WITHER_SKELETON_MELEE_WITHER_EFFECT_AND_NETHER_TARGET_GOALS,
                CombatMode.MELEE, KnowledgeGroup.WITHER_SKELETONS, false);
        put(SpeciesType.ZOMBIFIED_PIGLIN,
                VanillaOwner.ZOMBIFIED_PIGLIN_NEUTRAL_ANGER_ALERT_AND_MELEE,
                CombatMode.NEUTRAL_MELEE, KnowledgeGroup.ZOMBIFIED_PIGLINS, false);
    }

    public static Optional<UndeadCombatSpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static UndeadCombatSpeciesPolicy require(SpeciesType species) {
        UndeadCombatSpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) throw new IllegalArgumentException(
                "Species is outside skeleton/undead-combat batch: " + species);
        return policy;
    }

    public static Set<SpeciesType> species() {
        return Set.copyOf(POLICIES.keySet());
    }

    public static Map<SpeciesType, UndeadCombatSpeciesPolicy> all() {
        return Map.copyOf(POLICIES);
    }

    private static void put(SpeciesType species, VanillaOwner owner, CombatMode combatMode,
                            KnowledgeGroup knowledgeGroup, boolean daylightSensitive) {
        POLICIES.put(species, new UndeadCombatSpeciesPolicy(
                owner, combatMode, knowledgeGroup, daylightSensitive));
    }
}
