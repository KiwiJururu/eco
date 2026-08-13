package com.livingecology;

import com.livingecology.ai.PiglinSocietySpeciesPolicy;
import com.livingecology.data.BehaviorFamily;
import com.livingecology.data.FootprintType;
import com.livingecology.data.HabitatClass;
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

class PiglinSocietyEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.PIGLIN, SpeciesType.PIGLIN_BRUTE, SpeciesType.ZOMBIFIED_PIGLIN);

    @Test
    void ownershipAuditCoversExactlyTheThreeIssueSpecies() {
        assertEquals(BATCH, PiglinSocietySpeciesPolicy.species());
        assertEquals(BATCH.size(), PiglinSocietySpeciesPolicy.all().size());
        assertEquals(BATCH.size(), PiglinSocietySpeciesPolicy.all().values().stream()
                .map(PiglinSocietySpeciesPolicy::vanillaOwner).distinct().count());
        assertEquals(BATCH.size(), PiglinSocietySpeciesPolicy.all().values().stream()
                .map(PiglinSocietySpeciesPolicy::overlayMode).distinct().count());
    }

    @Test
    void livingPiglinSocietyAndZombifiedKnowledgeRemainSeparated() {
        var living = PiglinSocietySpeciesPolicy.KnowledgeGroup.LIVING_PIGLINS;
        var zombified = PiglinSocietySpeciesPolicy.KnowledgeGroup.ZOMBIFIED_PIGLINS;
        assertTrue(PiglinSocietySpeciesPolicy.require(SpeciesType.PIGLIN).livingSociety());
        assertTrue(PiglinSocietySpeciesPolicy.require(SpeciesType.PIGLIN_BRUTE).livingSociety());
        assertFalse(PiglinSocietySpeciesPolicy.require(SpeciesType.ZOMBIFIED_PIGLIN).livingSociety());
        assertEquals(living,
                PiglinSocietySpeciesPolicy.require(SpeciesType.PIGLIN).knowledgeGroup());
        assertEquals(living,
                PiglinSocietySpeciesPolicy.require(SpeciesType.PIGLIN_BRUTE).knowledgeGroup());
        assertEquals(zombified,
                PiglinSocietySpeciesPolicy.require(SpeciesType.ZOMBIFIED_PIGLIN).knowledgeGroup());
        assertEquals(PiglinSocietySpeciesPolicy.OverlayMode.ZOMBIFIED_PIGLIN_SEPARATE_UNDEAD_CONTROLLER,
                PiglinSocietySpeciesPolicy.require(SpeciesType.ZOMBIFIED_PIGLIN).overlayMode());
    }

    @Test
    void piglinAndBruteAffinityIsSymbioticAndWitherSkeletonRivalryIsContextual() {
        var cooperation = RelationService.natural(SpeciesType.PIGLIN, SpeciesType.PIGLIN_BRUTE);
        var reverse = RelationService.natural(SpeciesType.PIGLIN_BRUTE, SpeciesType.PIGLIN);
        assertEquals(5, cooperation.rivalry());
        assertEquals(90, cooperation.affinity());
        assertEquals(RelationKind.SYMBIOTIC, cooperation.kind());
        assertEquals(cooperation, reverse);

        var rivalry = RelationService.natural(SpeciesType.PIGLIN, SpeciesType.WITHER_SKELETON);
        assertEquals(85, rivalry.rivalry());
        assertEquals(0, rivalry.affinity());
        assertEquals(RelationKind.WARLIKE, rivalry.kind());
        assertEquals(rivalry,
                RelationService.natural(SpeciesType.WITHER_SKELETON, SpeciesType.PIGLIN));
    }

    @Test
    void livingPiglinsUsePersistentNetherCommunityProfilesWithoutGenericMovementOwnership() {
        for (SpeciesType species : EnumSet.of(SpeciesType.PIGLIN, SpeciesType.PIGLIN_BRUTE)) {
            SpeciesProfile profile = SpeciesProfile.of(species);
            PiglinSocietySpeciesPolicy policy = PiglinSocietySpeciesPolicy.require(species);
            assertEquals(MovementDomain.LAND, profile.movementDomain(), species.toString());
            assertEquals(MovementDomain.LAND, policy.expectedDomain(), species.toString());
            assertEquals(HabitatClass.NETHER, profile.habitatClass(), species.toString());
            assertEquals(TerritoryStyle.COMMUNITY, profile.territoryStyle(), species.toString());
            assertEquals(BehaviorFamily.SOCIETY_HOSTILE, profile.behaviorFamily(), species.toString());
            assertTrue(profile.formsPersistentTerritory(false), species.toString());
            assertTrue(policy.persistentEcology(), species.toString());
        }
    }

    @Test
    void zombifiedPiglinKeepsDistinctUndeadPersistentEcology() {
        SpeciesProfile profile = SpeciesProfile.of(SpeciesType.ZOMBIFIED_PIGLIN);
        assertEquals(BehaviorFamily.UNDEAD_COMBAT, profile.behaviorFamily());
        assertEquals(TerritoryStyle.HORDE, profile.territoryStyle());
        assertEquals(HabitatClass.NETHER, profile.habitatClass());
        assertTrue(profile.formsPersistentTerritory(false));
        assertTrue(PiglinSocietySpeciesPolicy.require(SpeciesType.ZOMBIFIED_PIGLIN)
                .persistentEcology());
    }

    @Test
    void PiglinBatchUsesNoEcologicalFootprintsOrReproduction() {
        for (SpeciesType species : BATCH) {
            SpeciesProfile profile = SpeciesProfile.of(species);
            assertEquals(FootprintType.NONE, profile.footprintType(), species.toString());
            assertEquals(ReproductionMode.NONE, profile.reproductionMode(), species.toString());
        }
    }
}
