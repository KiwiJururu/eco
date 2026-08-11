package com.livingecology;

import com.livingecology.data.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpeciesCatalogueTest {
    @Test
    void catalogueContainsExactly79SupportedCreatures() {
        assertEquals(79, SpeciesType.values().length);
        assertEquals(SpeciesType.values().length, SpeciesProfile.all().size());
    }

    @Test
    void everyProfileIsInternallyValid() {
        for (SpeciesType species : SpeciesType.values()) {
            SpeciesProfile profile = SpeciesProfile.of(species);
            assertSame(species, profile.species());
            assertFalse(profile.personalities().isEmpty(), species + " missing personalities");
            assertNotNull(profile.behaviorFamily());
            assertNotNull(profile.movementDomain());
            assertNotNull(profile.territoryStyle());
            assertNotNull(profile.footprintType());
            assertNotNull(profile.reproductionMode());
            assertNotNull(profile.habitatClass());
            assertNotNull(profile.activityPattern());
            for (AttributeType attribute : AttributeType.values()) {
                var range = profile.range(attribute);
                assertNotNull(range, species + " missing " + attribute);
                assertTrue(range.min() >= 0 && range.max() <= 100 && range.min() <= range.max(),
                        species + " invalid " + attribute + " range");
            }
            if (profile.territoryStyle() == TerritoryStyle.NONE) {
                assertFalse(profile.formsPersistentTerritory(false));
            }
            if (profile.footprintType() != FootprintType.NONE) {
                assertTrue(profile.maxTerritoryRadius() > 0, species + " footprint without territory radius");
            }
        }
    }
}
