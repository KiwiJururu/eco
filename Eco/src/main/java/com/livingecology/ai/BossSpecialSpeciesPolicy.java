package com.livingecology.ai;

import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Explicit vanilla ownership for issue #18 special/boss mobs. */
public record BossSpecialSpeciesPolicy(VanillaOwner vanillaOwner, OverlayMode overlayMode) {
    public enum VanillaOwner {
        WARDEN_BRAIN_ANGER_SONIC_BOOM_AND_DIG_EMERGE,
        WITHER_PHASE_INVULNERABILITY_FLIGHT_HEADS_AND_TARGETING,
        ENDER_DRAGON_PHASE_MANAGER_FLIGHT_CRYSTALS_AND_DEATH,
        GIANT_INTERNAL_VANILLA_BASELINE,
        SKELETON_HORSE_TRAP_TAME_RIDE_AND_HORSE_STATE,
        ZOMBIE_HORSE_TAME_RIDE_AND_HORSE_STATE
    }
    public enum OverlayMode { OBSERVATION_ONLY, MOUNT_OBSERVATION }

    private static final EnumMap<SpeciesType, BossSpecialSpeciesPolicy> POLICIES = new EnumMap<>(SpeciesType.class);
    static {
        put(SpeciesType.WARDEN, VanillaOwner.WARDEN_BRAIN_ANGER_SONIC_BOOM_AND_DIG_EMERGE, OverlayMode.OBSERVATION_ONLY);
        put(SpeciesType.WITHER, VanillaOwner.WITHER_PHASE_INVULNERABILITY_FLIGHT_HEADS_AND_TARGETING, OverlayMode.OBSERVATION_ONLY);
        put(SpeciesType.ENDER_DRAGON, VanillaOwner.ENDER_DRAGON_PHASE_MANAGER_FLIGHT_CRYSTALS_AND_DEATH, OverlayMode.OBSERVATION_ONLY);
        put(SpeciesType.GIANT, VanillaOwner.GIANT_INTERNAL_VANILLA_BASELINE, OverlayMode.OBSERVATION_ONLY);
        put(SpeciesType.SKELETON_HORSE, VanillaOwner.SKELETON_HORSE_TRAP_TAME_RIDE_AND_HORSE_STATE, OverlayMode.MOUNT_OBSERVATION);
        put(SpeciesType.ZOMBIE_HORSE, VanillaOwner.ZOMBIE_HORSE_TAME_RIDE_AND_HORSE_STATE, OverlayMode.MOUNT_OBSERVATION);
    }
    private static void put(SpeciesType s, VanillaOwner o, OverlayMode m) { POLICIES.put(s, new BossSpecialSpeciesPolicy(o, m)); }
    public static Optional<BossSpecialSpeciesPolicy> of(SpeciesType s) { return Optional.ofNullable(POLICIES.get(s)); }
    public static BossSpecialSpeciesPolicy require(SpeciesType s) {
        var p = POLICIES.get(s); if (p == null) throw new IllegalArgumentException("Outside boss/special batch: " + s); return p;
    }
    public static Set<SpeciesType> species() { return Set.copyOf(POLICIES.keySet()); }
    public static Map<SpeciesType, BossSpecialSpeciesPolicy> all() { return Map.copyOf(POLICIES); }
}
