package com.livingecology;

import com.livingecology.ai.BossSpecialSpeciesPolicy;
import com.livingecology.data.BehaviorFamily;
import com.livingecology.data.FootprintType;
import com.livingecology.data.ReproductionMode;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossSpecialEcologyTest {
    private static final Set<SpeciesType> BATCH = EnumSet.of(
            SpeciesType.WARDEN, SpeciesType.WITHER, SpeciesType.ENDER_DRAGON,
            SpeciesType.GIANT, SpeciesType.SKELETON_HORSE, SpeciesType.ZOMBIE_HORSE);

    @Test void ownershipAuditCoversExactlyTheSixIssueSpecies() {
        assertEquals(BATCH, BossSpecialSpeciesPolicy.species());
        assertEquals(6, BossSpecialSpeciesPolicy.all().values().stream()
                .map(BossSpecialSpeciesPolicy::vanillaOwner).distinct().count());
    }

    @Test void phaseDrivenBossesStayExplicitlyObservationOnly() {
        for (SpeciesType s : EnumSet.of(SpeciesType.WARDEN, SpeciesType.WITHER, SpeciesType.ENDER_DRAGON, SpeciesType.GIANT))
            assertEquals(BossSpecialSpeciesPolicy.OverlayMode.OBSERVATION_ONLY,
                    BossSpecialSpeciesPolicy.require(s).overlayMode());
    }

    @Test void horseVariantsStayMountObservationOnly() {
        for (SpeciesType s : EnumSet.of(SpeciesType.SKELETON_HORSE, SpeciesType.ZOMBIE_HORSE))
            assertEquals(BossSpecialSpeciesPolicy.OverlayMode.MOUNT_OBSERVATION,
                    BossSpecialSpeciesPolicy.require(s).overlayMode());
    }

    @Test void batchAddsNoEcologicalFootprintsOrReproduction() {
        for (SpeciesType s : BATCH) {
            SpeciesProfile p = SpeciesProfile.of(s);
            assertEquals(FootprintType.NONE, p.footprintType());
            assertEquals(ReproductionMode.NONE, p.reproductionMode());
        }
    }

    @Test void dragonAndWitherRemainBossFamily() {
        assertEquals(BehaviorFamily.BOSS, SpeciesProfile.of(SpeciesType.ENDER_DRAGON).behaviorFamily());
        assertEquals(BehaviorFamily.BOSS, SpeciesProfile.of(SpeciesType.WITHER).behaviorFamily());
    }
}
