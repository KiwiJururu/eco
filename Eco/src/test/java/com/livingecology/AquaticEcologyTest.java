package com.livingecology;

import com.livingecology.ai.AquaticSpeciesPolicy;
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

class AquaticEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.COD, SpeciesType.SALMON, SpeciesType.TROPICAL_FISH,
            SpeciesType.PUFFERFISH, SpeciesType.SQUID, SpeciesType.GLOW_SQUID,
            SpeciesType.DOLPHIN, SpeciesType.AXOLOTL, SpeciesType.TURTLE,
            SpeciesType.TADPOLE, SpeciesType.FROG);

    @Test
    void ownershipAuditCoversExactlyTheElevenIssueSpecies() {
        assertEquals(BATCH, AquaticSpeciesPolicy.species());
        for (SpeciesType species : BATCH) {
            AquaticSpeciesPolicy policy = AquaticSpeciesPolicy.require(species);
            assertTrue(policy.vanillaOwner() != null && policy.overlayMode() != null,
                    species + " missing explicit vanilla ownership policy");
        }
    }

    @Test
    void movementDomainsSeparateWaterOnlyFromAmphibiousSpecies() {
        Set<SpeciesType> amphibious = EnumSet.of(
                SpeciesType.AXOLOTL, SpeciesType.TURTLE, SpeciesType.FROG);
        for (SpeciesType species : BATCH) {
            MovementDomain expected = amphibious.contains(species)
                    ? MovementDomain.AMPHIBIOUS : MovementDomain.WATER;
            assertEquals(expected, SpeciesProfile.of(species).movementDomain(), species.toString());
        }
    }

    @Test
    void onlyHomeRangeSpeciesFormPersistentAquaticTerritory() {
        Set<SpeciesType> territorial = EnumSet.of(
                SpeciesType.AXOLOTL, SpeciesType.DOLPHIN, SpeciesType.TURTLE, SpeciesType.FROG);
        for (SpeciesType species : BATCH) {
            assertEquals(territorial.contains(species),
                    SpeciesProfile.of(species).formsPersistentTerritory(false), species.toString());
        }
    }

    @Test
    void reproductionModesPreserveVanillaBreedersAndNonBreeders() {
        Set<SpeciesType> vanillaBreeders = EnumSet.of(
                SpeciesType.AXOLOTL, SpeciesType.TURTLE, SpeciesType.FROG);
        for (SpeciesType species : BATCH) {
            ReproductionMode expected = vanillaBreeders.contains(species)
                    ? ReproductionMode.VANILLA_LOVE : ReproductionMode.NONE;
            assertEquals(expected, SpeciesProfile.of(species).reproductionMode(), species.toString());
        }
    }

    @Test
    void habitatRulesDistinguishOceanDeepWaterAndSwamp() {
        int codOcean = HabitatRules.fit(SpeciesProfile.of(SpeciesType.COD).habitatClass(),
                "ocean", "overworld", true, false, true);
        int codDry = HabitatRules.fit(SpeciesProfile.of(SpeciesType.COD).habitatClass(),
                "plains", "overworld", false, false, true);
        assertTrue(codOcean > codDry + 50);

        int glowDeep = HabitatRules.fit(SpeciesProfile.of(SpeciesType.GLOW_SQUID).habitatClass(),
                "deep_ocean", "overworld", true, false, false);
        int glowSurface = HabitatRules.fit(SpeciesProfile.of(SpeciesType.GLOW_SQUID).habitatClass(),
                "plains", "overworld", true, false, true);
        assertTrue(glowDeep > glowSurface);

        int frogSwamp = HabitatRules.fit(SpeciesProfile.of(SpeciesType.FROG).habitatClass(),
                "mangrove_swamp", "overworld", true, false, true);
        int frogDesert = HabitatRules.fit(SpeciesProfile.of(SpeciesType.FROG).habitatClass(),
                "desert", "overworld", false, false, true);
        assertTrue(frogSwamp > frogDesert + 50);
    }

    @Test
    void criticalVanillaControllersRemainObservationOnly() {
        assertEquals(AquaticSpeciesPolicy.OverlayMode.PUFFER_DEFENSE_ONLY,
                AquaticSpeciesPolicy.require(SpeciesType.PUFFERFISH).overlayMode());
        assertEquals(AquaticSpeciesPolicy.OverlayMode.VANILLA_OBSERVATION_ONLY,
                AquaticSpeciesPolicy.require(SpeciesType.TURTLE).overlayMode());
        assertEquals(AquaticSpeciesPolicy.OverlayMode.VANILLA_OBSERVATION_ONLY,
                AquaticSpeciesPolicy.require(SpeciesType.FROG).overlayMode());
        assertFalse(AquaticSpeciesPolicy.require(SpeciesType.PUFFERFISH).acceptsSocialAlarm());
    }
}
