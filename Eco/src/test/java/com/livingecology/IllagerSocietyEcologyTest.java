package com.livingecology;

import com.livingecology.ai.IllagerSocietySpeciesPolicy;
import com.livingecology.data.BehaviorFamily;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class IllagerSocietyEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.PILLAGER, SpeciesType.VINDICATOR, SpeciesType.EVOKER,
            SpeciesType.WITCH, SpeciesType.RAVAGER, SpeciesType.ILLUSIONER);

    @Test
    void ownershipAuditCoversExactlyTheSixIssueSpecies() {
        assertEquals(BATCH, IllagerSocietySpeciesPolicy.species());
        assertEquals(BATCH.size(), IllagerSocietySpeciesPolicy.all().size());
        assertEquals(BATCH.size(), IllagerSocietySpeciesPolicy.all().values().stream()
                .map(IllagerSocietySpeciesPolicy::vanillaOwner).distinct().count());
        assertEquals(BATCH.size(), IllagerSocietySpeciesPolicy.all().values().stream()
                .map(IllagerSocietySpeciesPolicy::societyRole).distinct().count());
    }

    @Test
    void everyCrossRoleIllagerRelationIsStronglySymbiotic() {
        for (SpeciesType from : BATCH) {
            for (SpeciesType to : BATCH) {
                if (from == to) continue;
                var relation = RelationService.natural(from, to);
                assertEquals(5, relation.rivalry(), from + " -> " + to);
                assertEquals(85, relation.affinity(), from + " -> " + to);
                assertEquals(RelationKind.SYMBIOTIC, relation.kind(), from + " -> " + to);
            }
        }
    }

    @Test
    void everyIllagerHasMaximumConflictWithVillagersAndIronGolems() {
        for (SpeciesType illager : BATCH) {
            for (SpeciesType defender : EnumSet.of(SpeciesType.VILLAGER, SpeciesType.IRON_GOLEM)) {
                var relation = RelationService.natural(illager, defender);
                var reverse = RelationService.natural(defender, illager);
                assertEquals(95, relation.rivalry(), illager + " -> " + defender);
                assertEquals(0, relation.affinity(), illager + " -> " + defender);
                assertEquals(RelationKind.WARLIKE, relation.kind(), illager + " -> " + defender);
                assertEquals(relation, reverse, "relationship must stay symmetric for " + illager);
            }
        }
    }

    @Test
    void IllagersUsePersistentLandCommunitiesWithoutEcologicalFootprints() {
        for (SpeciesType species : BATCH) {
            SpeciesProfile profile = SpeciesProfile.of(species);
            var policy = IllagerSocietySpeciesPolicy.require(species);
            assertEquals(BehaviorFamily.SOCIETY_HOSTILE, profile.behaviorFamily(), species.toString());
            assertEquals(MovementDomain.LAND, profile.movementDomain(), species.toString());
            assertEquals(MovementDomain.LAND, policy.expectedDomain(), species.toString());
            assertEquals(TerritoryStyle.COMMUNITY, profile.territoryStyle(), species.toString());
            assertTrue(profile.formsPersistentTerritory(false), species.toString());
            assertTrue(policy.persistentEcology(), species.toString());
            assertEquals(FootprintType.NONE, profile.footprintType(), species.toString());
            assertEquals(ReproductionMode.NONE, profile.reproductionMode(), species.toString());
        }
    }
}
