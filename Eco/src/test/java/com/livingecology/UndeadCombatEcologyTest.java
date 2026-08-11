package com.livingecology;

import com.livingecology.ai.UndeadCombatBehavior;
import com.livingecology.ai.UndeadCombatSpeciesPolicy;
import com.livingecology.data.FootprintType;
import com.livingecology.data.HabitatClass;
import com.livingecology.data.MovementDomain;
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

class UndeadCombatEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.SKELETON, SpeciesType.STRAY,
            SpeciesType.WITHER_SKELETON, SpeciesType.ZOMBIFIED_PIGLIN);

    @Test
    void ownershipAuditCoversExactlyTheFourIssueSpecies() {
        assertEquals(BATCH, UndeadCombatSpeciesPolicy.species());
        for (SpeciesType species : BATCH) {
            UndeadCombatSpeciesPolicy policy = UndeadCombatSpeciesPolicy.require(species);
            assertTrue(policy.vanillaOwner() != null && policy.combatMode() != null
                    && policy.knowledgeGroup() != null, species + " missing combat ownership policy");
            assertEquals(MovementDomain.LAND, SpeciesProfile.of(species).movementDomain(), species.toString());
        }
    }

    @Test
    void weaponAndDaylightPoliciesPreserveVanillaDifferences() {
        assertEquals(UndeadCombatSpeciesPolicy.CombatMode.WEAPON_AWARE_SKELETON,
                UndeadCombatSpeciesPolicy.require(SpeciesType.SKELETON).combatMode());
        assertEquals(UndeadCombatSpeciesPolicy.CombatMode.WEAPON_AWARE_SKELETON,
                UndeadCombatSpeciesPolicy.require(SpeciesType.STRAY).combatMode());
        assertEquals(UndeadCombatSpeciesPolicy.CombatMode.MELEE,
                UndeadCombatSpeciesPolicy.require(SpeciesType.WITHER_SKELETON).combatMode());
        assertEquals(UndeadCombatSpeciesPolicy.CombatMode.NEUTRAL_MELEE,
                UndeadCombatSpeciesPolicy.require(SpeciesType.ZOMBIFIED_PIGLIN).combatMode());

        assertTrue(UndeadCombatBehavior.daylightPriority(SpeciesType.SKELETON, true, true, false));
        assertTrue(UndeadCombatBehavior.daylightPriority(SpeciesType.STRAY, true, true, false));
        assertFalse(UndeadCombatBehavior.daylightPriority(SpeciesType.SKELETON, false, true, false));
        assertFalse(UndeadCombatBehavior.daylightPriority(SpeciesType.SKELETON, true, false, false));
        assertFalse(UndeadCombatBehavior.daylightPriority(SpeciesType.SKELETON, true, true, true));
        assertFalse(UndeadCombatBehavior.daylightPriority(SpeciesType.WITHER_SKELETON, true, true, false));
        assertFalse(UndeadCombatBehavior.daylightPriority(SpeciesType.ZOMBIFIED_PIGLIN, true, true, false));
    }

    @Test
    void meleeRecoveryRequiresAnAppropriateMovementState() {
        assertTrue(UndeadCombatBehavior.shouldAttemptMeleeRecovery(
                false, true, false, false, 25.0D, true, false));
        assertTrue(UndeadCombatBehavior.shouldAttemptMeleeRecovery(
                false, true, false, false, 25.0D, false, true));
        assertFalse(UndeadCombatBehavior.shouldAttemptMeleeRecovery(
                true, true, false, false, 25.0D, true, false));
        assertFalse(UndeadCombatBehavior.shouldAttemptMeleeRecovery(
                false, true, false, false, 16.0D, true, false));
        assertFalse(UndeadCombatBehavior.shouldAttemptMeleeRecovery(
                false, false, false, false, 25.0D, true, false));
        assertFalse(UndeadCombatBehavior.shouldAttemptMeleeRecovery(
                false, true, true, false, 25.0D, true, false));
        assertFalse(UndeadCombatBehavior.shouldAttemptMeleeRecovery(
                false, true, false, true, 25.0D, true, false));
        assertFalse(UndeadCombatBehavior.shouldAttemptMeleeRecovery(
                false, true, false, false, 25.0D, false, false));
    }

    @Test
    void onlyNetherGroupSpeciesKeepPersistentTerritories() {
        assertFalse(SpeciesProfile.of(SpeciesType.SKELETON).formsPersistentTerritory(false));
        assertFalse(SpeciesProfile.of(SpeciesType.STRAY).formsPersistentTerritory(false));
        assertTrue(SpeciesProfile.of(SpeciesType.WITHER_SKELETON).formsPersistentTerritory(false));
        assertTrue(SpeciesProfile.of(SpeciesType.ZOMBIFIED_PIGLIN).formsPersistentTerritory(false));
        for (SpeciesType species : BATCH) {
            assertEquals(FootprintType.NONE, SpeciesProfile.of(species).footprintType(), species.toString());
        }
    }

    @Test
    void generalColdAndNetherHabitatsRemainDistinct() {
        assertEquals(HabitatClass.GENERAL, SpeciesProfile.of(SpeciesType.SKELETON).habitatClass());
        assertEquals(HabitatClass.COLD, SpeciesProfile.of(SpeciesType.STRAY).habitatClass());
        assertEquals(HabitatClass.NETHER, SpeciesProfile.of(SpeciesType.WITHER_SKELETON).habitatClass());
        assertEquals(HabitatClass.NETHER, SpeciesProfile.of(SpeciesType.ZOMBIFIED_PIGLIN).habitatClass());

        int strayFrozen = HabitatRules.fit(HabitatClass.COLD,
                "snowy_plains", "overworld", false, true, true);
        int strayDesert = HabitatRules.fit(HabitatClass.COLD,
                "desert", "overworld", false, false, true);
        int witherNether = HabitatRules.fit(HabitatClass.NETHER,
                "nether_wastes", "the_nether", false, false, false);
        int witherOverworld = HabitatRules.fit(HabitatClass.NETHER,
                "plains", "overworld", false, false, true);
        assertTrue(strayFrozen > strayDesert + 45);
        assertTrue(witherNether > witherOverworld + 45);
    }

    @Test
    void piglinAndWitherSkeletonRivalryIsContextNotAForcedTargetRule() {
        var relation = RelationService.natural(SpeciesType.PIGLIN, SpeciesType.WITHER_SKELETON);
        assertEquals(RelationKind.WARLIKE, relation.kind());
        assertTrue(relation.rivalry() >= 85);
        assertTrue(relation.affinity() <= 5);
    }
}
