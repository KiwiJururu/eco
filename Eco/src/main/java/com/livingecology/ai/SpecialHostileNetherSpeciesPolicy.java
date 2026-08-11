package com.livingecology.ai;

import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Explicit vanilla-ownership and movement audit for issue #12. */
public record SpecialHostileNetherSpeciesPolicy(VanillaOwner vanillaOwner,
                                                 ObservationMode observationMode,
                                                 HazardContract hazardContract,
                                                 MovementDomain expectedDomain,
                                                 boolean acceptsSocialAlarm) {
    public enum VanillaOwner {
        CREEPER_SWELL_FUSE_EXPLOSION_AND_CAT_AVOIDANCE,
        SLIME_SIZE_SQUISH_SPLIT_JUMP_AND_MOVE_CONTROL,
        MAGMA_CUBE_SIZE_SQUISH_SPLIT_LAVA_JUMP_AND_MOVE_CONTROL,
        BLAZE_FIREBALL_VOLLEY_CHARGE_HEIGHT_AND_WATER_DAMAGE,
        GHAST_RANDOM_FLOAT_FIREBALL_CHARGE_AND_EXPLOSION_POWER,
        PHANTOM_ANCHOR_CIRCLE_SWOOP_SIZE_AND_DAYLIGHT_BURNING,
        VEX_OWNER_BOUND_ORIGIN_CHARGE_PHASE_AND_LIMITED_LIFE,
        HOGLIN_BRAIN_REPELLENT_HUNT_RETREAT_CONVERSION_AND_BREEDING,
        ZOGLIN_BRAIN_UNIVERSAL_HOSTILITY_ATTACK_AND_BABY_STATE,
        STRIDER_LAVA_WALK_COLD_STATE_SADDLE_BOOST_TEMPT_AND_BREEDING
    }

    public enum ObservationMode {
        CREEPER_FUSE_OBSERVATION,
        SLIME_JUMP_OBSERVATION,
        MAGMA_JUMP_OBSERVATION,
        BLAZE_RANGED_FLIGHT_OBSERVATION,
        GHAST_CHARGE_FLIGHT_OBSERVATION,
        PHANTOM_SWEEP_ANCHOR_OBSERVATION,
        VEX_OWNER_CHARGE_BOUND_OBSERVATION,
        HOGLIN_BRAIN_CONVERSION_OBSERVATION,
        ZOGLIN_BRAIN_OBSERVATION,
        STRIDER_RIDE_TEMPERATURE_OBSERVATION
    }

    public enum HazardContract {
        OVERWORLD_LAND,
        SWAMP_LAND,
        NETHER_FIRE_NATIVE,
        DAYLIGHT_SENSITIVE_AIR,
        PHASING_AIR,
        NETHER_REPELLENT_AND_ZOMBIFICATION,
        NETHER_UNDEAD_LAND,
        LAVA_NATIVE_WATER_SENSITIVE
    }

    private static final EnumMap<SpeciesType, SpecialHostileNetherSpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.CREEPER, VanillaOwner.CREEPER_SWELL_FUSE_EXPLOSION_AND_CAT_AVOIDANCE,
                ObservationMode.CREEPER_FUSE_OBSERVATION, HazardContract.OVERWORLD_LAND,
                MovementDomain.LAND);
        put(SpeciesType.SLIME, VanillaOwner.SLIME_SIZE_SQUISH_SPLIT_JUMP_AND_MOVE_CONTROL,
                ObservationMode.SLIME_JUMP_OBSERVATION, HazardContract.SWAMP_LAND,
                MovementDomain.LAND);
        put(SpeciesType.MAGMA_CUBE,
                VanillaOwner.MAGMA_CUBE_SIZE_SQUISH_SPLIT_LAVA_JUMP_AND_MOVE_CONTROL,
                ObservationMode.MAGMA_JUMP_OBSERVATION, HazardContract.NETHER_FIRE_NATIVE,
                MovementDomain.LAND);
        put(SpeciesType.BLAZE, VanillaOwner.BLAZE_FIREBALL_VOLLEY_CHARGE_HEIGHT_AND_WATER_DAMAGE,
                ObservationMode.BLAZE_RANGED_FLIGHT_OBSERVATION,
                HazardContract.NETHER_FIRE_NATIVE, MovementDomain.AIR);
        put(SpeciesType.GHAST,
                VanillaOwner.GHAST_RANDOM_FLOAT_FIREBALL_CHARGE_AND_EXPLOSION_POWER,
                ObservationMode.GHAST_CHARGE_FLIGHT_OBSERVATION,
                HazardContract.NETHER_FIRE_NATIVE, MovementDomain.AIR);
        put(SpeciesType.PHANTOM,
                VanillaOwner.PHANTOM_ANCHOR_CIRCLE_SWOOP_SIZE_AND_DAYLIGHT_BURNING,
                ObservationMode.PHANTOM_SWEEP_ANCHOR_OBSERVATION,
                HazardContract.DAYLIGHT_SENSITIVE_AIR, MovementDomain.AIR);
        put(SpeciesType.VEX, VanillaOwner.VEX_OWNER_BOUND_ORIGIN_CHARGE_PHASE_AND_LIMITED_LIFE,
                ObservationMode.VEX_OWNER_CHARGE_BOUND_OBSERVATION,
                HazardContract.PHASING_AIR, MovementDomain.AIR);
        put(SpeciesType.HOGLIN,
                VanillaOwner.HOGLIN_BRAIN_REPELLENT_HUNT_RETREAT_CONVERSION_AND_BREEDING,
                ObservationMode.HOGLIN_BRAIN_CONVERSION_OBSERVATION,
                HazardContract.NETHER_REPELLENT_AND_ZOMBIFICATION, MovementDomain.LAND);
        put(SpeciesType.ZOGLIN,
                VanillaOwner.ZOGLIN_BRAIN_UNIVERSAL_HOSTILITY_ATTACK_AND_BABY_STATE,
                ObservationMode.ZOGLIN_BRAIN_OBSERVATION,
                HazardContract.NETHER_UNDEAD_LAND, MovementDomain.LAND);
        put(SpeciesType.STRIDER,
                VanillaOwner.STRIDER_LAVA_WALK_COLD_STATE_SADDLE_BOOST_TEMPT_AND_BREEDING,
                ObservationMode.STRIDER_RIDE_TEMPERATURE_OBSERVATION,
                HazardContract.LAVA_NATIVE_WATER_SENSITIVE, MovementDomain.LAVA);
    }

    public static Optional<SpecialHostileNetherSpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static SpecialHostileNetherSpeciesPolicy require(SpeciesType species) {
        SpecialHostileNetherSpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) throw new IllegalArgumentException(
                "Species is outside common hostile/Nether batch: " + species);
        return policy;
    }

    public static Set<SpeciesType> species() {
        return Set.copyOf(POLICIES.keySet());
    }

    public static Map<SpeciesType, SpecialHostileNetherSpeciesPolicy> all() {
        return Map.copyOf(POLICIES);
    }

    private static void put(SpeciesType species, VanillaOwner owner, ObservationMode observation,
                            HazardContract hazard, MovementDomain domain) {
        POLICIES.put(species, new SpecialHostileNetherSpeciesPolicy(
                owner, observation, hazard, domain, false));
    }
}
