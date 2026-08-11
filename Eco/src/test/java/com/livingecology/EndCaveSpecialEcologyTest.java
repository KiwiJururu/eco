package com.livingecology;

import com.livingecology.ai.EndCaveSpecialSpeciesPolicy;
import com.livingecology.data.FootprintType;
import com.livingecology.data.HabitatClass;
import com.livingecology.data.MovementDomain;
import com.livingecology.data.ReproductionMode;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.TerritoryStyle;
import com.livingecology.environment.HabitatRules;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndCaveSpecialEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.ENDERMAN, SpeciesType.ENDERMITE,
            SpeciesType.SILVERFISH, SpeciesType.SHULKER);

    @Test
    void ownershipAuditCoversExactlyTheFourIssueSpecies() {
        assertEquals(BATCH, EndCaveSpecialSpeciesPolicy.species());
        assertEquals(BATCH.size(), EndCaveSpecialSpeciesPolicy.all().size());
        assertEquals(BATCH.size(), EndCaveSpecialSpeciesPolicy.all().values().stream()
                .map(EndCaveSpecialSpeciesPolicy::vanillaOwner).distinct().count());
        assertEquals(BATCH.size(), EndCaveSpecialSpeciesPolicy.all().values().stream()
                .map(EndCaveSpecialSpeciesPolicy::observationMode).distinct().count());
    }

    @Test
    void endermanEndermiteWarIsSymmetricAndVanillaOwned() {
        var forward = RelationService.natural(SpeciesType.ENDERMAN, SpeciesType.ENDERMITE);
        var reverse = RelationService.natural(SpeciesType.ENDERMITE, SpeciesType.ENDERMAN);
        assertEquals(100, forward.rivalry());
        assertEquals(0, forward.affinity());
        assertEquals(RelationKind.WARLIKE, forward.kind());
        assertEquals(forward, reverse);
        assertEquals(
                EndCaveSpecialSpeciesPolicy.VanillaOwner
                        .ENDERMAN_STARE_ANGER_TELEPORT_BLOCK_AND_ENDERMITE_TARGETING,
                EndCaveSpecialSpeciesPolicy.require(SpeciesType.ENDERMAN).vanillaOwner());
    }

    @Test
    void persistentCommunityExistsOnlyWhereEcologicallyAppropriate() {
        SpeciesProfile enderman = SpeciesProfile.of(SpeciesType.ENDERMAN);
        SpeciesProfile endermite = SpeciesProfile.of(SpeciesType.ENDERMITE);
        SpeciesProfile silverfish = SpeciesProfile.of(SpeciesType.SILVERFISH);
        SpeciesProfile shulker = SpeciesProfile.of(SpeciesType.SHULKER);

        assertEquals(TerritoryStyle.MOBILE, enderman.territoryStyle());
        assertFalse(enderman.formsPersistentTerritory(false));
        assertEquals(TerritoryStyle.NONE, endermite.territoryStyle());
        assertFalse(endermite.formsPersistentTerritory(false));
        assertEquals(TerritoryStyle.NEST, silverfish.territoryStyle());
        assertTrue(silverfish.formsPersistentTerritory(false));
        assertEquals(TerritoryStyle.STRUCTURE, shulker.territoryStyle());
        assertTrue(shulker.formsPersistentTerritory(false));

        assertTrue(EndCaveSpecialSpeciesPolicy.require(SpeciesType.SILVERFISH)
                .sharesNestMemory());
        assertEquals(1L, BATCH.stream().filter(species ->
                EndCaveSpecialSpeciesPolicy.require(species).sharesNestMemory()).count());
    }

    @Test
    void movementDomainsMatchSpecialMovementOwnership() {
        for (SpeciesType species : BATCH) {
            assertEquals(SpeciesProfile.of(species).movementDomain(),
                    EndCaveSpecialSpeciesPolicy.require(species).expectedDomain(),
                    species.toString());
        }
        assertEquals(MovementDomain.STATIC,
                EndCaveSpecialSpeciesPolicy.require(SpeciesType.SHULKER).expectedDomain());
        assertEquals(MovementDomain.LAND,
                EndCaveSpecialSpeciesPolicy.require(SpeciesType.ENDERMAN).expectedDomain());
    }

    @Test
    void endAndCaveHabitatResponsesRemainDistinct() {
        for (SpeciesType species : Set.of(
                SpeciesType.ENDERMAN, SpeciesType.ENDERMITE, SpeciesType.SHULKER)) {
            assertEquals(HabitatClass.END, SpeciesProfile.of(species).habitatClass());
        }
        assertEquals(HabitatClass.CAVE, SpeciesProfile.of(SpeciesType.SILVERFISH).habitatClass());

        int endFit = HabitatRules.fit(HabitatClass.END,
                "the_end", "the_end", false, false, true);
        int overworldFit = HabitatRules.fit(HabitatClass.END,
                "plains", "overworld", false, false, true);
        int caveFit = HabitatRules.fit(HabitatClass.CAVE,
                "dripstone_caves", "overworld", false, false, false);
        int exposedFit = HabitatRules.fit(HabitatClass.CAVE,
                "plains", "overworld", false, false, true);
        assertTrue(endFit > overworldFit + 50);
        assertTrue(caveFit > exposedFit + 50);
    }

    @Test
    void batchAddsNoReproductionOrPhysicalFootprints() {
        for (SpeciesType species : BATCH) {
            SpeciesProfile profile = SpeciesProfile.of(species);
            assertEquals(ReproductionMode.NONE, profile.reproductionMode(), species.toString());
            assertEquals(FootprintType.NONE, profile.footprintType(), species.toString());
        }
    }
}
