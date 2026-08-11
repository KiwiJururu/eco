package com.livingecology;

import com.livingecology.ai.FlyingColonyBehavior;
import com.livingecology.ai.FlyingColonySpeciesPolicy;
import com.livingecology.data.ActivityPattern;
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

class FlyingColonyEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.BAT, SpeciesType.PARROT, SpeciesType.ALLAY, SpeciesType.BEE);

    @Test
    void ownershipAuditCoversExactlyTheFourIssueSpecies() {
        assertEquals(BATCH, FlyingColonySpeciesPolicy.species());
        for (SpeciesType species : BATCH) {
            FlyingColonySpeciesPolicy policy = FlyingColonySpeciesPolicy.require(species);
            assertTrue(policy.vanillaOwner() != null && policy.overlayMode() != null,
                    species + " missing explicit vanilla ownership policy");
        }
    }

    @Test
    void allFourSpeciesUseAirDomainWithoutSharingOneMovementController() {
        for (SpeciesType species : BATCH) {
            assertEquals(MovementDomain.AIR, SpeciesProfile.of(species).movementDomain(), species.toString());
        }
        assertEquals(FlyingColonySpeciesPolicy.OverlayMode.BAT_STATE_OBSERVATION,
                FlyingColonySpeciesPolicy.require(SpeciesType.BAT).overlayMode());
        assertEquals(FlyingColonySpeciesPolicy.OverlayMode.ALLAY_BRAIN_OBSERVATION,
                FlyingColonySpeciesPolicy.require(SpeciesType.ALLAY).overlayMode());
        assertEquals(FlyingColonySpeciesPolicy.OverlayMode.BEE_HIVE_COLONY_MEMORY,
                FlyingColonySpeciesPolicy.require(SpeciesType.BEE).overlayMode());
    }

    @Test
    void persistentTerritoryMatchesHomeRangeNestAndNonTerritorialProfiles() {
        Set<SpeciesType> territorial = EnumSet.of(SpeciesType.BAT, SpeciesType.PARROT, SpeciesType.BEE);
        for (SpeciesType species : BATCH) {
            assertEquals(territorial.contains(species),
                    SpeciesProfile.of(species).formsPersistentTerritory(false), species.toString());
        }
    }

    @Test
    void onlyBeeUsesVanillaLoveReproduction() {
        for (SpeciesType species : BATCH) {
            ReproductionMode expected = species == SpeciesType.BEE
                    ? ReproductionMode.VANILLA_LOVE : ReproductionMode.NONE;
            assertEquals(expected, SpeciesProfile.of(species).reproductionMode(), species.toString());
        }
    }

    @Test
    void caveForestAndAerialHabitatsStayDistinct() {
        assertEquals(HabitatClass.CAVE, SpeciesProfile.of(SpeciesType.BAT).habitatClass());
        assertEquals(HabitatClass.FOREST, SpeciesProfile.of(SpeciesType.PARROT).habitatClass());
        assertEquals(HabitatClass.FOREST, SpeciesProfile.of(SpeciesType.BEE).habitatClass());
        assertEquals(HabitatClass.AERIAL, SpeciesProfile.of(SpeciesType.ALLAY).habitatClass());

        int batCave = HabitatRules.fit(HabitatClass.CAVE,
                "dripstone_caves", "overworld", false, false, false);
        int batOpen = HabitatRules.fit(HabitatClass.CAVE,
                "plains", "overworld", false, false, true);
        int beeForest = HabitatRules.fit(HabitatClass.FOREST,
                "flower_forest", "overworld", false, false, true);
        int beeDesert = HabitatRules.fit(HabitatClass.FOREST,
                "desert", "overworld", false, false, true);
        assertTrue(batCave > batOpen + 40);
        assertTrue(beeForest > beeDesert + 40);
    }

    @Test
    void optionalIdleOverlayRespectsDayAndNightProfiles() {
        assertEquals(ActivityPattern.NOCTURNAL, SpeciesProfile.of(SpeciesType.BAT).activityPattern());
        assertEquals(ActivityPattern.DIURNAL, SpeciesProfile.of(SpeciesType.PARROT).activityPattern());
        assertEquals(ActivityPattern.DIURNAL, SpeciesProfile.of(SpeciesType.BEE).activityPattern());
        assertEquals(ActivityPattern.VARIABLE, SpeciesProfile.of(SpeciesType.ALLAY).activityPattern());

        assertFalse(FlyingColonyBehavior.allowsIdleOverlay(ActivityPattern.NOCTURNAL, 6000L));
        assertTrue(FlyingColonyBehavior.allowsIdleOverlay(ActivityPattern.NOCTURNAL, 18000L));
        assertTrue(FlyingColonyBehavior.allowsIdleOverlay(ActivityPattern.DIURNAL, 6000L));
        assertFalse(FlyingColonyBehavior.allowsIdleOverlay(ActivityPattern.DIURNAL, 18000L));
        assertTrue(FlyingColonyBehavior.allowsIdleOverlay(ActivityPattern.VARIABLE, 6000L));
        assertTrue(FlyingColonyBehavior.allowsIdleOverlay(ActivityPattern.VARIABLE, 18000L));
    }
}
