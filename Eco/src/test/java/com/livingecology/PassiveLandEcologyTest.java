package com.livingecology;

import com.livingecology.ai.PassiveLandBehavior;
import com.livingecology.ai.PassiveLandSpeciesPolicy;
import com.livingecology.data.ActivityPattern;
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

class PassiveLandEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.SHEEP, SpeciesType.PIG, SpeciesType.CHICKEN, SpeciesType.RABBIT,
            SpeciesType.HORSE, SpeciesType.DONKEY, SpeciesType.MULE, SpeciesType.CAMEL,
            SpeciesType.GOAT, SpeciesType.LLAMA, SpeciesType.TRADER_LLAMA, SpeciesType.SNIFFER);

    @Test
    void ownershipAuditCoversExactlyTheTwelveIssueSpecies() {
        assertEquals(BATCH, PassiveLandSpeciesPolicy.species());
        for (SpeciesType species : BATCH) {
            PassiveLandSpeciesPolicy policy = PassiveLandSpeciesPolicy.require(species);
            assertTrue(policy.vanillaOwner() != null && policy.overlayMode() != null,
                    species + " missing explicit vanilla ownership policy");
            assertEquals(MovementDomain.LAND, SpeciesProfile.of(species).movementDomain(), species.toString());
        }
    }

    @Test
    void traderLlamaAloneIsNonTerritorial() {
        for (SpeciesType species : BATCH) {
            assertEquals(species != SpeciesType.TRADER_LLAMA,
                    SpeciesProfile.of(species).formsPersistentTerritory(false), species.toString());
        }
    }

    @Test
    void reproductionMatchesVanillaBreedersAndSterileExceptions() {
        Set<SpeciesType> nonBreeders = EnumSet.of(SpeciesType.MULE, SpeciesType.TRADER_LLAMA);
        for (SpeciesType species : BATCH) {
            ReproductionMode expected = nonBreeders.contains(species)
                    ? ReproductionMode.NONE : ReproductionMode.VANILLA_LOVE;
            assertEquals(expected, SpeciesProfile.of(species).reproductionMode(), species.toString());
        }
    }

    @Test
    void rabbitIsTheOnlyReadableSafeFootprintInThisBatch() {
        for (SpeciesType species : BATCH) {
            FootprintType expected = species == SpeciesType.RABBIT
                    ? FootprintType.BURROW : FootprintType.NONE;
            assertEquals(expected, SpeciesProfile.of(species).footprintType(), species.toString());
        }
    }

    @Test
    void desertMountainAndGeneralHabitatClassesRemainDistinct() {
        assertEquals(HabitatClass.DESERT, SpeciesProfile.of(SpeciesType.CAMEL).habitatClass());
        assertEquals(HabitatClass.MOUNTAIN, SpeciesProfile.of(SpeciesType.GOAT).habitatClass());
        assertEquals(HabitatClass.MOUNTAIN, SpeciesProfile.of(SpeciesType.LLAMA).habitatClass());
        assertEquals(HabitatClass.GENERAL, SpeciesProfile.of(SpeciesType.SHEEP).habitatClass());

        int camelDesert = HabitatRules.fit(HabitatClass.DESERT,
                "desert", "overworld", false, false, true);
        int camelForest = HabitatRules.fit(HabitatClass.DESERT,
                "forest", "overworld", false, false, true);
        int goatPeak = HabitatRules.fit(HabitatClass.MOUNTAIN,
                "jagged_peaks", "overworld", false, false, true);
        int goatSwamp = HabitatRules.fit(HabitatClass.MOUNTAIN,
                "swamp", "overworld", false, false, true);
        assertTrue(camelDesert > camelForest + 50);
        assertTrue(goatPeak > goatSwamp + 40);
    }

    @Test
    void socialAlarmAndActivityPoliciesAreExplicit() {
        assertFalse(PassiveLandSpeciesPolicy.require(SpeciesType.TRADER_LLAMA).acceptsSocialAlarm());
        assertTrue(PassiveLandSpeciesPolicy.require(SpeciesType.MULE).acceptsSocialAlarm());
        assertTrue(PassiveLandSpeciesPolicy.require(SpeciesType.SNIFFER).acceptsSocialAlarm());

        assertTrue(PassiveLandBehavior.allowsIdleOverlay(ActivityPattern.DIURNAL, 6000L));
        assertFalse(PassiveLandBehavior.allowsIdleOverlay(ActivityPattern.DIURNAL, 18000L));
        assertFalse(PassiveLandBehavior.allowsIdleOverlay(ActivityPattern.NOCTURNAL, 6000L));
        assertTrue(PassiveLandBehavior.allowsIdleOverlay(ActivityPattern.NOCTURNAL, 18000L));
        assertTrue(PassiveLandBehavior.allowsIdleOverlay(ActivityPattern.VARIABLE, 6000L));
    }
}
