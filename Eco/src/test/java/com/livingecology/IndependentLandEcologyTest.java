package com.livingecology;

import com.livingecology.ai.IndependentLandBehavior;
import com.livingecology.ai.IndependentLandSpeciesPolicy;
import com.livingecology.data.ActivityPattern;
import com.livingecology.data.FootprintType;
import com.livingecology.data.HabitatClass;
import com.livingecology.data.MovementDomain;
import com.livingecology.data.ReproductionMode;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.environment.HabitatRules;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndependentLandEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.FOX, SpeciesType.OCELOT, SpeciesType.CAT,
            SpeciesType.POLAR_BEAR, SpeciesType.PANDA);

    @Test
    void ownershipAuditCoversExactlyTheFiveIssueSpecies() {
        assertEquals(BATCH, IndependentLandSpeciesPolicy.species());
        for (SpeciesType species : BATCH) {
            IndependentLandSpeciesPolicy policy = IndependentLandSpeciesPolicy.require(species);
            assertTrue(policy.vanillaOwner() != null && policy.overlayMode() != null,
                    species + " missing explicit vanilla ownership policy");
            assertEquals(MovementDomain.LAND, SpeciesProfile.of(species).movementDomain(), species.toString());
        }
    }

    @Test
    void allFiveUseHomeRangesWithoutPhysicalFootprints() {
        for (SpeciesType species : BATCH) {
            SpeciesProfile profile = SpeciesProfile.of(species);
            assertTrue(profile.formsPersistentTerritory(false), species.toString());
            assertEquals(FootprintType.NONE, profile.footprintType(), species.toString());
        }
    }

    @Test
    void automaticReproductionIsLimitedToWildFoxAndOcelot() {
        for (SpeciesType species : BATCH) {
            ReproductionMode expected = species == SpeciesType.FOX || species == SpeciesType.OCELOT
                    ? ReproductionMode.VANILLA_LOVE : ReproductionMode.NONE;
            assertEquals(expected, SpeciesProfile.of(species).reproductionMode(), species.toString());
        }
    }

    @Test
    void predatorOverlapIsBorderedRatherThanForcedAggression() {
        var relation = RelationService.natural(SpeciesType.FOX, SpeciesType.OCELOT);
        assertEquals(RelationKind.BORDERED, relation.kind());
        assertTrue(relation.rivalry() < 85, "Predator overlap became an automatic combat target rule");
        assertTrue(relation.affinity() < 30, "Independent predator territories became cooperative");
    }

    @Test
    void forestVillageAndColdHabitatsRemainDistinct() {
        assertEquals(HabitatClass.FOREST, SpeciesProfile.of(SpeciesType.FOX).habitatClass());
        assertEquals(HabitatClass.FOREST, SpeciesProfile.of(SpeciesType.PANDA).habitatClass());
        assertEquals(HabitatClass.VILLAGE, SpeciesProfile.of(SpeciesType.CAT).habitatClass());
        assertEquals(HabitatClass.COLD, SpeciesProfile.of(SpeciesType.POLAR_BEAR).habitatClass());

        int foxForest = HabitatRules.fit(HabitatClass.FOREST,
                "forest", "overworld", false, false, true);
        int foxDesert = HabitatRules.fit(HabitatClass.FOREST,
                "desert", "overworld", false, false, true);
        int bearFrozen = HabitatRules.fit(HabitatClass.COLD,
                "frozen_ocean", "overworld", false, true, true);
        int bearJungle = HabitatRules.fit(HabitatClass.COLD,
                "jungle", "overworld", false, false, true);
        assertTrue(foxForest > foxDesert + 35);
        assertTrue(bearFrozen > bearJungle + 45);
    }

    @Test
    void nocturnalAndVariableActivityGatesAreExplicit() {
        assertFalse(IndependentLandBehavior.allowsIdleOverlay(ActivityPattern.NOCTURNAL, 6000L));
        assertTrue(IndependentLandBehavior.allowsIdleOverlay(ActivityPattern.NOCTURNAL, 18000L));
        assertTrue(IndependentLandBehavior.allowsIdleOverlay(ActivityPattern.VARIABLE, 6000L));
        assertTrue(IndependentLandBehavior.allowsIdleOverlay(ActivityPattern.DIURNAL, 6000L));
        assertFalse(IndependentLandBehavior.allowsIdleOverlay(ActivityPattern.DIURNAL, 18000L));
    }
}
