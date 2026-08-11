package com.livingecology.environment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public final class EnvironmentSavedData extends SavedData {
    private static final String DATA_NAME = "livingecology_environment";
    private final Map<Long, EcoRegionState> regions = new HashMap<>();

    public static EnvironmentSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(EnvironmentSavedData::load, EnvironmentSavedData::new, DATA_NAME);
    }

    public static EnvironmentSavedData load(CompoundTag tag) {
        EnvironmentSavedData data = new EnvironmentSavedData();
        ListTag list = tag.getList("regions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            EcoRegionState state = EcoRegionState.load(list.getCompound(i));
            data.regions.put(state.key(), state);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (EcoRegionState state : regions.values()) list.add(state.save());
        tag.put("regions", list);
        return tag;
    }

    public EcoRegionState peek(long key) { return regions.get(key); }

    public void rebase(long now) {
        for (EcoRegionState state : regions.values()) state.rebase(now);
        setDirty();
    }

    public EcoRegionState getOrCreate(long key, long now) {
        EcoRegionState existing = regions.get(key);
        if (existing != null) return existing;
        EcoRegionState created = new EcoRegionState(key, now);
        regions.put(key, created);
        setDirty();
        return created;
    }
}
