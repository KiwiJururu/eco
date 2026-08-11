package com.livingecology.territory;

import com.livingecology.data.SpeciesType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Persistent territory database. It saves causes/state (center, seed, pressure, etc.),
 * never a block-by-block representation of territory.
 */
public final class TerritorySavedData extends SavedData {
    private static final String DATA_NAME = "livingecology_territories";
    private static final int INDEX_REGION_CHUNKS = 16;

    private final Map<Long, TerritoryRecord> territories = new HashMap<>();
    private final Map<String, RelationRecord> relations = new HashMap<>();
    private final Map<Long, Set<Long>> spatialIndex = new HashMap<>();
    private long nextId = 1L;
    private double simulationScale = 1.0D;

    public TerritorySavedData() { }

    public static TerritorySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TerritorySavedData::load, TerritorySavedData::new, DATA_NAME);
    }

    public static TerritorySavedData load(CompoundTag tag) {
        TerritorySavedData data = new TerritorySavedData();
        data.nextId = Math.max(1L, tag.getLong("nextId"));
        data.simulationScale = tag.contains("simulationScale", Tag.TAG_DOUBLE)
                ? Math.max(0.01D, tag.getDouble("simulationScale")) : 1.0D;

        ListTag list = tag.getList("territories", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            TerritoryRecord record = TerritoryRecord.load(list.getCompound(i));
            data.territories.put(record.id(), record);
            data.nextId = Math.max(data.nextId, record.id() + 1L);
        }

        ListTag relationList = tag.getList("relations", Tag.TAG_COMPOUND);
        for (int i = 0; i < relationList.size(); i++) {
            RelationRecord relation = RelationRecord.load(relationList.getCompound(i));
            data.relations.put(pairKey(relation.a(), relation.b()), relation);
        }
        data.rebuildIndex();
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("nextId", nextId);
        tag.putDouble("simulationScale", simulationScale);

        ListTag list = new ListTag();
        territories.values().stream().sorted(Comparator.comparingLong(TerritoryRecord::id))
                .forEach(r -> list.add(r.save()));
        tag.put("territories", list);

        ListTag relationList = new ListTag();
        relations.values().forEach(r -> relationList.add(r.save()));
        tag.put("relations", relationList);
        return tag;
    }

    public TerritoryRecord create(SpeciesType species, BlockPos center, BlockPos core, int radiusChunks,
                                  int pressure, int maturity, int population, long seed, long now) {
        TerritoryRecord record = new TerritoryRecord(nextId++, species, center, core, radiusChunks,
                pressure, maturity, population, seed, now);
        territories.put(record.id(), record);
        index(record);
        setDirty();
        return record;
    }

    public TerritoryRecord get(long id) { return territories.get(id); }
    public Collection<TerritoryRecord> all() { return Collections.unmodifiableCollection(territories.values()); }

    public List<TerritoryRecord> near(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        int regionX = Math.floorDiv(chunkX, INDEX_REGION_CHUNKS);
        int regionZ = Math.floorDiv(chunkZ, INDEX_REGION_CHUNKS);
        HashSet<Long> ids = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Set<Long> bucket = spatialIndex.get(regionKey(regionX + dx, regionZ + dz));
                if (bucket != null) ids.addAll(bucket);
            }
        }
        ArrayList<TerritoryRecord> result = new ArrayList<>(ids.size());
        for (long id : ids) {
            TerritoryRecord record = territories.get(id);
            if (record != null) result.add(record);
        }
        return result;
    }

    public void reindex(TerritoryRecord record, BlockPos oldCenter) {
        long oldKey = regionKeyFor(oldCenter);
        Set<Long> old = spatialIndex.get(oldKey);
        if (old != null) {
            old.remove(record.id());
            if (old.isEmpty()) spatialIndex.remove(oldKey);
        }
        index(record);
        setDirty();
    }

    private void rebuildIndex() {
        spatialIndex.clear();
        territories.values().forEach(this::index);
    }

    private void index(TerritoryRecord record) {
        spatialIndex.computeIfAbsent(regionKeyFor(record.center()), k -> new HashSet<>()).add(record.id());
    }

    private static long regionKeyFor(BlockPos pos) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return regionKey(Math.floorDiv(chunkX, INDEX_REGION_CHUNKS), Math.floorDiv(chunkZ, INDEX_REGION_CHUNKS));
    }

    private static long regionKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static String pairKey(long first, long second) {
        long a = Math.min(first, second);
        long b = Math.max(first, second);
        return a + ":" + b;
    }

    public RelationRecord getRelation(long first, long second) {
        if (first <= 0 || second <= 0 || first == second) return null;
        return relations.get(pairKey(first, second));
    }

    public RelationRecord relation(long first, long second) {
        if (first <= 0 || second <= 0 || first == second) return null;
        String key = pairKey(first, second);
        RelationRecord relation = relations.get(key);
        if (relation == null) {
            relation = new RelationRecord(first, second);
            relations.put(key, relation);
            setDirty();
        }
        return relation;
    }

    public double simulationScale() { return simulationScale; }

    /** Rebase before changing scale so the new scale is never applied retroactively. */
    public void setSimulationScale(double scale, long now) {
        for (TerritoryRecord record : territories.values()) record.setLastRawUpdate(now);
        for (RelationRecord relation : relations.values()) relation.setLastResolution(now);
        this.simulationScale = Math.max(0.01D, Math.min(100.0D, scale));
        setDirty();
    }
}
