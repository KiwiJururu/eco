package com.livingecology;

import com.livingecology.ai.GuardianSocietySpeciesPolicy;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardianSocietyEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(SpeciesType.GUARDIAN, SpeciesType.ELDER_GUARDIAN);

    @Test
    void ownershipAuditCoversExactlyGuardianAndElder() {
        assertEquals(BATCH, GuardianSocietySpeciesPolicy.species());
        assertEquals(2, GuardianSocietySpeciesPolicy.all().values().stream()
                .map(GuardianSocietySpeciesPolicy::vanillaOwner).distinct().count());
        assertEquals(2, GuardianSocietySpeciesPolicy.all().values().stream()
                .map(GuardianSocietySpeciesPolicy::monumentRole).distinct().count());
    }

    @Test
    void guardianAndElderHaveMaximumSocietyAffinity() {
        var relation = RelationService.natural(SpeciesType.GUARDIAN, SpeciesType.ELDER_GUARDIAN);
        assertEquals(0, relation.rivalry());
        assertEquals(95, relation.affinity());
        assertEquals(RelationKind.SYMBIOTIC, relation.kind());
        assertEquals(relation, RelationService.natural(SpeciesType.ELDER_GUARDIAN, SpeciesType.GUARDIAN));
    }

    @Test
    void monumentProfilesAreWaterOnlyPersistentStructures() {
        for (SpeciesType species : BATCH) {
            SpeciesProfile profile = SpeciesProfile.of(species);
            assertEquals(BehaviorFamily.GUARDIAN, profile.behaviorFamily());
            assertEquals(MovementDomain.WATER, profile.movementDomain());
            assertEquals(MovementDomain.WATER, GuardianSocietySpeciesPolicy.require(species).expectedDomain());
            assertEquals(TerritoryStyle.STRUCTURE, profile.territoryStyle());
            assertEquals(HabitatClass.DEEP_WATER, profile.habitatClass());
            assertTrue(profile.formsPersistentTerritory(false));
            assertEquals(FootprintType.NONE, profile.footprintType());
            assertEquals(ReproductionMode.NONE, profile.reproductionMode());
        }
    }

    @Test
    void elderInfluenceIsBoundedBehaviorNotRawStats() {
        var guardian = GuardianSocietySpeciesPolicy.require(SpeciesType.GUARDIAN);
        var elder = GuardianSocietySpeciesPolicy.require(SpeciesType.ELDER_GUARDIAN);
        assertTrue(elder.memoryRange() > guardian.memoryRange());
        assertTrue(elder.maxMemoryReceivers() > guardian.maxMemoryReceivers());
        assertTrue(elder.maxMemoryReceivers() <= 12);
    }
}
