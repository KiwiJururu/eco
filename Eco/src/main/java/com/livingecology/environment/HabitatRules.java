package com.livingecology.environment;

import com.livingecology.data.HabitatClass;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;

import java.util.Locale;

/** Cheap habitat scoring. It never searches neighbouring chunks or structures. */
public final class HabitatRules {
    private HabitatRules() {}

    public static int fit(ServerLevel level, BlockPos pos, SpeciesType species) {
        String biome = level.getBiome(pos).unwrapKey()
                .map(k -> k.location().getPath()).orElse("unknown").toLowerCase(Locale.ROOT);
        String dimension = level.dimension().location().getPath().toLowerCase(Locale.ROOT);
        boolean water = level.getFluidState(pos).is(FluidTags.WATER);
        boolean lava = level.getFluidState(pos).is(FluidTags.LAVA);
        return fit(SpeciesProfile.of(species).habitatClass(), biome, dimension, water, lava, level.canSeeSky(pos));
    }

    /** Pure overload used by automated tests and deterministic generation. */
    public static int fit(HabitatClass habitat, String biomePath, String dimensionPath,
                          boolean inWater, boolean inLava, boolean canSeeSky) {
        String b = biomePath == null ? "" : biomePath.toLowerCase(Locale.ROOT);
        String d = dimensionPath == null ? "" : dimensionPath.toLowerCase(Locale.ROOT);
        int score = 55;

        boolean nether = d.contains("nether");
        boolean end = d.contains("end");
        boolean ocean = b.contains("ocean");
        boolean river = b.contains("river");
        boolean watery = ocean || river || b.contains("beach") || b.contains("swamp");
        boolean forest = b.contains("forest") || b.contains("taiga") || b.contains("jungle")
                || b.contains("bamboo") || b.contains("grove");
        boolean desert = b.contains("desert") || b.contains("badlands");
        boolean cold = b.contains("snow") || b.contains("frozen") || b.contains("ice") || b.contains("grove");
        boolean mountain = b.contains("peak") || b.contains("slope") || b.contains("mountain")
                || b.contains("windswept") || b.contains("grove");
        boolean swamp = b.contains("swamp") || b.contains("mangrove");
        boolean mushroom = b.contains("mushroom");
        boolean cave = b.contains("cave") || b.contains("deep_dark") || !canSeeSky;

        score = switch (habitat) {
            case GENERAL -> nether || end ? 30 : 60;
            case WATER -> (inWater ? 85 : watery ? 68 : 30) + (nether ? -30 : 0);
            case OCEAN -> inWater && ocean ? 100 : ocean ? 80 : inWater ? 55 : 20;
            case DEEP_WATER -> inWater && (ocean || !canSeeSky) ? 95 : inWater ? 60 : 20;
            case CAVE -> cave ? 90 : 35;
            case FOREST -> forest ? 90 : nether || end ? 20 : 45;
            case DESERT -> desert ? 95 : 30;
            case COLD -> cold ? 95 : 30;
            case MOUNTAIN -> mountain ? 92 : 40;
            case SWAMP -> swamp ? 95 : watery ? 55 : 35;
            case MUSHROOM -> mushroom ? 100 : 20;
            case LAVA -> inLava ? 100 : nether ? 60 : 10;
            case NETHER -> nether ? 95 : 20;
            case END -> end ? 95 : 35;
            case VILLAGE -> nether || end ? 20 : 65; // structures are intentionally not scanned here
            case STRUCTURE -> 55;                    // structure-specific vanilla AI remains authoritative
            case AERIAL -> nether ? 45 : 70;
        };
        return Math.max(0, Math.min(100, score));
    }
}
