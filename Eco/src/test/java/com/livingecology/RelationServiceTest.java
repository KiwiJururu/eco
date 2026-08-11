package com.livingecology;

import com.livingecology.data.SpeciesType;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RelationServiceTest {
    @Test
    void prototypeAnchorRelationshipsRemainStable() {
        var spiderZombie = RelationService.natural(SpeciesType.SPIDER, SpeciesType.ZOMBIE);
        assertEquals(RelationKind.SYMBIOTIC, spiderZombie.kind());
        assertTrue(spiderZombie.affinity() >= 65);

        var wolfZombie = RelationService.natural(SpeciesType.WOLF, SpeciesType.ZOMBIE);
        assertEquals(RelationKind.WARLIKE, wolfZombie.kind());
        assertTrue(wolfZombie.rivalry() >= 85);

        var wolfSpider = RelationService.natural(SpeciesType.WOLF, SpeciesType.SPIDER);
        assertEquals(RelationKind.BORDERED, wolfSpider.kind());
        assertTrue(wolfSpider.rivalry() > wolfSpider.affinity());
    }

    @Test
    void communityRelationsAreCoherent() {
        assertTrue(RelationService.natural(SpeciesType.VILLAGER, SpeciesType.IRON_GOLEM).affinity() >= 90);
        assertTrue(RelationService.natural(SpeciesType.PILLAGER, SpeciesType.VILLAGER).rivalry() >= 90);
        assertTrue(RelationService.natural(SpeciesType.PIGLIN, SpeciesType.PIGLIN_BRUTE).affinity() >= 80);
        assertTrue(RelationService.natural(SpeciesType.PIGLIN, SpeciesType.WITHER_SKELETON).rivalry() >= 80);
        assertTrue(RelationService.natural(SpeciesType.ENDERMAN, SpeciesType.ENDERMITE).rivalry() >= 95);
    }

    @Test
    void relationMatrixAlwaysStaysWithinBounds() {
        for (SpeciesType a : SpeciesType.values()) {
            for (SpeciesType b : SpeciesType.values()) {
                var relation = RelationService.natural(a, b);
                assertTrue(relation.rivalry() >= 0 && relation.rivalry() <= 100, a + " -> " + b);
                assertTrue(relation.affinity() >= 0 && relation.affinity() <= 100, a + " -> " + b);
                assertNotNull(relation.kind());
            }
        }
    }
}
