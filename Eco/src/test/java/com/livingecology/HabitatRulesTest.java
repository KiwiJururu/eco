package com.livingecology;

import com.livingecology.data.HabitatClass;
import com.livingecology.environment.HabitatRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HabitatRulesTest {
    @Test
    void forestSpeciesPreferForestToDesert() {
        int forest = HabitatRules.fit(HabitatClass.FOREST, "forest", "overworld", false, false, true);
        int desert = HabitatRules.fit(HabitatClass.FOREST, "desert", "overworld", false, false, true);
        assertTrue(forest >= 85);
        assertTrue(forest > desert + 30);
    }

    @Test
    void waterAndOceanProfilesReactToWaterAndBiome() {
        assertTrue(HabitatRules.fit(HabitatClass.WATER, "river", "overworld", true, false, true)
                > HabitatRules.fit(HabitatClass.WATER, "plains", "overworld", false, false, true));
        assertEquals(100, HabitatRules.fit(HabitatClass.OCEAN, "deep_ocean", "overworld", true, false, true));
    }

    @Test
    void netherEndColdAndSwampRemainDistinct() {
        assertTrue(HabitatRules.fit(HabitatClass.NETHER, "nether_wastes", "the_nether", false, false, false) >= 90);
        assertTrue(HabitatRules.fit(HabitatClass.END, "the_end", "the_end", false, false, true) >= 90);
        assertTrue(HabitatRules.fit(HabitatClass.COLD, "snowy_plains", "overworld", false, false, true) >= 90);
        assertTrue(HabitatRules.fit(HabitatClass.SWAMP, "mangrove_swamp", "overworld", false, false, true) >= 90);
    }

    @Test
    void caveProfileRecognizesNoSkyAsShelter() {
        int cave = HabitatRules.fit(HabitatClass.CAVE, "plains", "overworld", false, false, false);
        int open = HabitatRules.fit(HabitatClass.CAVE, "plains", "overworld", false, false, true);
        assertTrue(cave > open);
    }
}
