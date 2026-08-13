package com.livingecology;

import com.livingecology.ai.VillageGuardianSpeciesPolicy;
import com.livingecology.data.FootprintType;
import com.livingecology.data.MovementDomain;
import com.livingecology.data.ReproductionMode;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.TerritoryStyle;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillageGuardianEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.VILLAGER, SpeciesType.WANDERING_TRADER,
            SpeciesType.IRON_GOLEM, SpeciesType.SNOW_GOLEM, SpeciesType.TRADER_LLAMA);

    @Test
    void ownershipAuditCoversExactlyTheFiveIssueSpecies() {
        assertEquals(BATCH, VillageGuardianSpeciesPolicy.species());
        assertEquals(BATCH.size(), VillageGuardianSpeciesPolicy.all().size());
        assertEquals(BATCH.size(), VillageGuardianSpeciesPolicy.all().values().stream()
                .map(VillageGuardianSpeciesPolicy::vanillaOwner).distinct().count());
        assertEquals(BATCH.size(), VillageGuardianSpeciesPolicy.all().values().stream()
                .map(VillageGuardianSpeciesPolicy::overlayMode).distinct().count());
    }

    @Test
    void villagerAndIronGolemHaveSymmetricMaximumAffinity() {
        var forward = RelationService.natural(SpeciesType.VILLAGER, SpeciesType.IRON_GOLEM);
        var reverse = RelationService.natural(SpeciesType.IRON_GOLEM, SpeciesType.VILLAGER);
        assertEquals(0, forward.rivalry());
        assertEquals(100, forward.affinity());
        assertEquals(RelationKind.SYMBIOTIC, forward.kind());
        assertEquals(forward, reverse);
    }

    @Test
    void persistentCommunityExcludesTransientTraderCaravan() {
        Set<SpeciesType> persistent = EnumSet.of(
                SpeciesType.VILLAGER, SpeciesType.IRON_GOLEM, SpeciesType.SNOW_GOLEM);
        for (SpeciesType species : BATCH) {
            VillageGuardianSpeciesPolicy policy = VillageGuardianSpeciesPolicy.require(species);
            assertEquals(persistent.contains(species), policy.persistentCommunity(), species.toString());
            assertEquals(persistent.contains(species),
                    SpeciesProfile.of(species).formsPersistentTerritory(false), species.toString());
        }
        assertEquals(TerritoryStyle.NONE,
                SpeciesProfile.of(SpeciesType.WANDERING_TRADER).territoryStyle());
        assertEquals(TerritoryStyle.NONE,
                SpeciesProfile.of(SpeciesType.TRADER_LLAMA).territoryStyle());
    }

    @Test
    void villageAndTraderKnowledgeStayInSeparateLocalGroups() {
        var village = VillageGuardianSpeciesPolicy.KnowledgeGroup.VILLAGE;
        var caravan = VillageGuardianSpeciesPolicy.KnowledgeGroup.TRADER_CARAVAN;
        assertEquals(village, VillageGuardianSpeciesPolicy.require(SpeciesType.VILLAGER).knowledgeGroup());
        assertEquals(village, VillageGuardianSpeciesPolicy.require(SpeciesType.IRON_GOLEM).knowledgeGroup());
        assertEquals(village, VillageGuardianSpeciesPolicy.require(SpeciesType.SNOW_GOLEM).knowledgeGroup());
        assertEquals(caravan,
                VillageGuardianSpeciesPolicy.require(SpeciesType.WANDERING_TRADER).knowledgeGroup());
        assertEquals(caravan,
                VillageGuardianSpeciesPolicy.require(SpeciesType.TRADER_LLAMA).knowledgeGroup());
    }

    @Test
    void allSpeciesUseLandDomainWithoutGenericNavigationOwnership() {
        for (SpeciesType species : BATCH) {
            assertEquals(MovementDomain.LAND, SpeciesProfile.of(species).movementDomain(), species.toString());
            assertEquals(MovementDomain.LAND,
                    VillageGuardianSpeciesPolicy.require(species).expectedDomain(), species.toString());
        }
        assertEquals(VillageGuardianSpeciesPolicy.OverlayMode.VILLAGER_BRAIN_OBSERVATION,
                VillageGuardianSpeciesPolicy.require(SpeciesType.VILLAGER).overlayMode());
        assertEquals(VillageGuardianSpeciesPolicy.OverlayMode.IRON_GOLEM_DEFENSE_OBSERVATION,
                VillageGuardianSpeciesPolicy.require(SpeciesType.IRON_GOLEM).overlayMode());
    }

    @Test
    void playerStructureSafetyHasNoEcologicalFootprintsOrReproduction() {
        for (SpeciesType species : BATCH) {
            SpeciesProfile profile = SpeciesProfile.of(species);
            assertEquals(FootprintType.NONE, profile.footprintType(), species.toString());
            assertEquals(ReproductionMode.NONE, profile.reproductionMode(), species.toString());
        }
        assertFalse(SpeciesProfile.of(SpeciesType.WANDERING_TRADER)
                .formsPersistentTerritory(false));
        assertTrue(SpeciesProfile.of(SpeciesType.VILLAGER)
                .formsPersistentTerritory(false));
    }
}
