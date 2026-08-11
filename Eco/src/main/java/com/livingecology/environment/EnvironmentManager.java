package com.livingecology.environment;

import com.livingecology.data.SpeciesType;
import com.livingecology.territory.TerritorySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class EnvironmentManager {
    private EnvironmentManager() {}

    private static final int ECO_REGION_CHUNKS = 16;

    public static long regionKey(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        int rx = Math.floorDiv(chunkX, ECO_REGION_CHUNKS);
        int rz = Math.floorDiv(chunkZ, ECO_REGION_CHUNKS);
        return ((long) rx << 32) ^ (rz & 0xffffffffL);
    }

    public static EnvironmentSnapshot snapshot(ServerLevel level, BlockPos pos, SpeciesType species) {
        EnvironmentSavedData data = EnvironmentSavedData.get(level);
        long key = regionKey(pos);
        EcoRegionState state = data.peek(key);
        int resources = 50;
        int coverage = 50;
        int stability = 50;
        if (state != null) {
            double scale = TerritorySavedData.get(level).simulationScale();
            if (state.recover(level.getGameTime(), scale)) data.setDirty();
            resources = Mth.clamp(50 + state.resourceDelta(), 0, 100);
            coverage = Mth.clamp(50 + state.coverageDelta(), 0, 100);
            stability = Mth.clamp(50 + state.stabilityDelta(), 0, 100);
        }
        int habitability = calculateHabitability(species, resources, coverage, stability);
        return new EnvironmentSnapshot(resources, coverage, stability, habitability);
    }

    private static int calculateHabitability(SpeciesType species, int resources, int coverage, int stability) {
        double value = switch (species) {
            case COW -> resources * 0.40D + coverage * 0.30D + stability * 0.30D;
            case WOLF -> resources * 0.38D + coverage * 0.32D + stability * 0.30D;
            case SPIDER -> resources * 0.15D + coverage * 0.45D + stability * 0.40D;
            case ZOMBIE -> 35.0D + (100 - stability) * 0.35D + coverage * 0.15D;
        };
        return Mth.clamp((int) Math.round(value), 0, 100);
    }

    public static void onNaturalBlockBroken(ServerLevel level, BlockPos pos, BlockState state) {
        int vegetation = 0;
        int terrain = 0;
        if (state.is(BlockTags.LOGS)) vegetation = 8;
        else if (state.is(BlockTags.LEAVES)) vegetation = 1;

        // Common mining is intentionally almost free. Only very large-scale terrain removal accumulates an effect.
        if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.TUFF) || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)) terrain = 1;
        if (vegetation <= 0 && terrain <= 0) return;

        EnvironmentSavedData data = EnvironmentSavedData.get(level);
        EcoRegionState region = data.getOrCreate(regionKey(pos), level.getGameTime());
        if (vegetation > 0) region.addVegetationDamage(vegetation);
        if (terrain > 0) region.addTerrainDamage(terrain);
        data.setDirty();
    }

    public static void onExplosion(ServerLevel level, BlockPos pos, int affectedBlocks) {
        if (affectedBlocks <= 0) return;
        EnvironmentSavedData data = EnvironmentSavedData.get(level);
        EcoRegionState region = data.getOrCreate(regionKey(pos), level.getGameTime());
        int stabilityLoss = Math.min(12, 1 + affectedBlocks / 24);
        region.disturb(0, -Math.min(4, affectedBlocks / 64), -stabilityLoss);
        data.setDirty();
    }

    /**
     * Cheap local fire observer. Fire only evolves in loaded/ticking space anyway, so sampling near players
     * captures the situations the player can actually witness without scanning whole chunks.
     */
    public static void sampleFireNearPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            BlockPos origin = player.blockPosition();
            int fireCount = 0;
            // 13 x 7 x 13 = 1183 cheap block-state reads every 40 logical ticks per player.
            outer:
            for (int dx = -6; dx <= 6; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -6; dz <= 6; dz++) {
                        BlockState state = level.getBlockState(origin.offset(dx, dy, dz));
                        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                            fireCount++;
                            if (fireCount >= 4) break outer;
                        }
                    }
                }
            }
            if (fireCount >= 4) {
                EnvironmentSavedData data = EnvironmentSavedData.get(level);
                EcoRegionState region = data.getOrCreate(regionKey(origin), level.getGameTime());
                if (region.recordFire(level.getGameTime())) data.setDirty();
            }
        }
    }
}
