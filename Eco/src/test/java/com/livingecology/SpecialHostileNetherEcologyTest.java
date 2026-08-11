package com.livingecology;

import com.livingecology.ai.SpecialHostileNetherSpeciesPolicy;
import com.livingecology.data.FootprintType;
import com.livingecology.data.HabitatClass;
import com.livingecology.data.MovementDomain;
import com.livingecology.data.ReproductionMode;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.environment.HabitatRules;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialHostileNetherEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.CREEPER, SpeciesType.SLIME, SpeciesType.MAGMA_CUBE,
            SpeciesType.BLAZE, SpeciesType.GHAST, SpeciesType.PHANTOM, SpeciesType.VEX,
            SpeciesType.HOGLIN, SpeciesType.ZOGLIN, SpeciesType.STRIDER);

    @Test
    void ownershipAuditCoversExactlyTheTenIssueSpecies() {
        assertEquals(BATCH, SpecialHostileNetherSpeciesPolicy.species());
        for (SpeciesType species : BATCH) {
            SpecialHostileNetherSpeciesPolicy policy =
                    SpecialHostileNetherSpeciesPolicy.require(species);
            assertTrue(policy.vanillaOwner() != null && policy.observationMode() != null
                    && policy.hazardContract() != null, species + " missing explicit ownership policy");
            assertFalse(policy.acceptsSocialAlarm(), species + " should not receive generic herd movement state");
        }
    }

    @Test
    void landAirAndLavaDomainsAreExplicitAndMatchProfiles() {
        Set<SpeciesType> land = EnumSet.of(SpeciesType.CREEPER, SpeciesType.SLIME,
                SpeciesType.MAGMA_CUBE, SpeciesType.HOGLIN, SpeciesType.ZOGLIN);
        Set<SpeciesType> air = EnumSet.of(SpeciesType.BLAZE, SpeciesType.GHAST,
                SpeciesType.PHANTOM, SpeciesType.VEX);
        for (SpeciesType species : BATCH) {
            MovementDomain expected = land.contains(species) ? MovementDomain.LAND
                    : air.contains(species) ? MovementDomain.AIR : MovementDomain.LAVA;
            assertEquals(expected, SpecialHostileNetherSpeciesPolicy.require(species).expectedDomain(),
                    species.toString());
            assertEquals(expected, SpeciesProfile.of(species).movementDomain(), species.toString());
        }
        assertEquals(Set.of(SpeciesType.STRIDER),
                BATCH.stream().filter(species -> SpeciesProfile.of(species).movementDomain()
                        == MovementDomain.LAVA).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void persistentEcologyExistsOnlyForProfiledStructureGroupAndHomeRangeSpecies() {
        Set<SpeciesType> persistent = EnumSet.of(
                SpeciesType.BLAZE, SpeciesType.HOGLIN, SpeciesType.ZOGLIN, SpeciesType.STRIDER);
        for (SpeciesType species : BATCH) {
            assertEquals(persistent.contains(species),
                    SpeciesProfile.of(species).formsPersistentTerritory(false), species.toString());
            assertEquals(FootprintType.NONE, SpeciesProfile.of(species).footprintType(), species.toString());
        }
    }

    @Test
    void vanillaLoveIsLimitedToHoglinAndStrider() {
        Set<SpeciesType> breeders = EnumSet.of(SpeciesType.HOGLIN, SpeciesType.STRIDER);
        for (SpeciesType species : BATCH) {
            ReproductionMode expected = breeders.contains(species)
                    ? ReproductionMode.VANILLA_LOVE : ReproductionMode.NONE;
            assertEquals(expected, SpeciesProfile.of(species).reproductionMode(), species.toString());
        }
    }

    @Test
    void habitatAndHazardContractsRetainOverworldNetherAndLavaDifferences() {
        assertEquals(HabitatClass.GENERAL, SpeciesProfile.of(SpeciesType.CREEPER).habitatClass());
        assertEquals(HabitatClass.SWAMP, SpeciesProfile.of(SpeciesType.SLIME).habitatClass());
        assertEquals(HabitatClass.AERIAL, SpeciesProfile.of(SpeciesType.PHANTOM).habitatClass());
        assertEquals(HabitatClass.NETHER, SpeciesProfile.of(SpeciesType.HOGLIN).habitatClass());
        assertEquals(HabitatClass.LAVA, SpeciesProfile.of(SpeciesType.STRIDER).habitatClass());

        assertEquals(SpecialHostileNetherSpeciesPolicy.HazardContract.DAYLIGHT_SENSITIVE_AIR,
                SpecialHostileNetherSpeciesPolicy.require(SpeciesType.PHANTOM).hazardContract());
        assertEquals(SpecialHostileNetherSpeciesPolicy.HazardContract.NETHER_REPELLENT_AND_ZOMBIFICATION,
                SpecialHostileNetherSpeciesPolicy.require(SpeciesType.HOGLIN).hazardContract());
        assertEquals(SpecialHostileNetherSpeciesPolicy.HazardContract.LAVA_NATIVE_WATER_SENSITIVE,
                SpecialHostileNetherSpeciesPolicy.require(SpeciesType.STRIDER).hazardContract());

        int slimeSwamp = HabitatRules.fit(HabitatClass.SWAMP,
                "swamp", "overworld", false, false, true);
        int slimeDesert = HabitatRules.fit(HabitatClass.SWAMP,
                "desert", "overworld", false, false, true);
        int hoglinNether = HabitatRules.fit(HabitatClass.NETHER,
                "crimson_forest", "the_nether", false, false, false);
        int hoglinOverworld = HabitatRules.fit(HabitatClass.NETHER,
                "forest", "overworld", false, false, true);
        int striderLava = HabitatRules.fit(HabitatClass.LAVA,
                "nether_wastes", "the_nether", false, true, false);
        int striderWater = HabitatRules.fit(HabitatClass.LAVA,
                "ocean", "overworld", true, false, true);
        assertTrue(slimeSwamp > slimeDesert + 50);
        assertTrue(hoglinNether > hoglinOverworld + 60);
        assertTrue(striderLava > striderWater + 80);
    }

    @Test
    void everyOverlayIsObservationOnlyAndSpeciesSpecific() {
        assertEquals(BATCH.size(), SpecialHostileNetherSpeciesPolicy.all().size());
        assertEquals(BATCH.size(), SpecialHostileNetherSpeciesPolicy.all().values().stream()
                .map(SpecialHostileNetherSpeciesPolicy::observationMode).distinct().count());
        assertEquals(SpecialHostileNetherSpeciesPolicy.ObservationMode.CREEPER_FUSE_OBSERVATION,
                SpecialHostileNetherSpeciesPolicy.require(SpeciesType.CREEPER).observationMode());
        assertEquals(SpecialHostileNetherSpeciesPolicy.ObservationMode.STRIDER_RIDE_TEMPERATURE_OBSERVATION,
                SpecialHostileNetherSpeciesPolicy.require(SpeciesType.STRIDER).observationMode());
    }
}
