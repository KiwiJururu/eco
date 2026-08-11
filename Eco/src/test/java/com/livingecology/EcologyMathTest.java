package com.livingecology;

import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.environment.EcologyMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EcologyMathTest {
    @Test
    void capacityGrowsWithRadiusAndHabitat() {
        SpeciesProfile cow = SpeciesProfile.of(SpeciesType.COW);
        int small = EcologyMath.carryingCapacity(cow, 2, 55);
        int large = EcologyMath.carryingCapacity(cow, 5, 55);
        int rich = EcologyMath.carryingCapacity(cow, 5, 90);
        assertTrue(large > small);
        assertTrue(rich >= large);
    }

    @Test
    void reproductionRequiresEnvironmentalHeadroom() {
        assertTrue(EcologyMath.reproductionAllowed(4, 10, 70, 70, 70));
        assertFalse(EcologyMath.reproductionAllowed(10, 10, 70, 70, 70));
        assertFalse(EcologyMath.reproductionAllowed(4, 10, 30, 70, 70));
        assertFalse(EcologyMath.reproductionAllowed(4, 10, 70, 20, 70));
    }

    @Test
    void reproductionCanDriveTerritoryExpansionButNeverPastSpeciesCap() {
        SpeciesProfile cow = SpeciesProfile.of(SpeciesType.COW);
        int radius = 2;
        for (int i = 0; i < 20; i++) {
            int capacity = EcologyMath.carryingCapacity(cow, radius, 90);
            radius = EcologyMath.desiredRadius(cow, radius, capacity, 90);
        }
        assertEquals(cow.maxTerritoryRadius(), radius);
    }

    @Test
    void abandonedPressureNeverRecoversAboveTenFromCatchupTarget() {
        assertTrue(EcologyMath.pressureTarget(50, 100, 100, true) <= 10);
    }
}
