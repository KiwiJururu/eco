package com.livingecology.environment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class EcoRegionState {
    private final long key;
    private int resourceDelta;
    private int coverageDelta;
    private int stabilityDelta;
    private int vegetationDamagePoints;
    private int terrainDamagePoints;
    private long lastUpdate;
    private long lastFireImpact;

    public EcoRegionState(long key, long now) {
        this.key = key;
        this.lastUpdate = now;
    }

    public long key() { return key; }
    public int resourceDelta() { return resourceDelta; }
    public int coverageDelta() { return coverageDelta; }
    public int stabilityDelta() { return stabilityDelta; }
    public long lastFireImpact() { return lastFireImpact; }
    public void rebase(long now) { this.lastUpdate = now; }

    public void addVegetationDamage(int points) {
        vegetationDamagePoints = Math.max(0, vegetationDamagePoints + points);
        int coverageLoss = vegetationDamagePoints / 32;
        if (coverageLoss > 0) {
            coverageDelta = Mth.clamp(coverageDelta - coverageLoss, -50, 50);
            int resourceLoss = Math.max(0, coverageLoss / 2);
            resourceDelta = Mth.clamp(resourceDelta - resourceLoss, -50, 50);
            vegetationDamagePoints %= 32;
        }
    }

    public void addTerrainDamage(int points) {
        terrainDamagePoints = Math.max(0, terrainDamagePoints + points);
        int stabilityLoss = terrainDamagePoints / 512;
        if (stabilityLoss > 0) {
            stabilityDelta = Mth.clamp(stabilityDelta - stabilityLoss, -50, 50);
            terrainDamagePoints %= 512;
        }
    }

    public void disturb(int resource, int coverage, int stability) {
        resourceDelta = Mth.clamp(resourceDelta + resource, -50, 50);
        coverageDelta = Mth.clamp(coverageDelta + coverage, -50, 50);
        stabilityDelta = Mth.clamp(stabilityDelta + stability, -50, 50);
    }

    public boolean recordFire(long now) {
        if (now - lastFireImpact < 200L) return false;
        lastFireImpact = now;
        disturb(-2, -2, -4);
        return true;
    }

    public boolean recover(long now, double scale) {
        long raw = Math.max(0L, now - lastUpdate);
        long scaled = (long) Math.floor(raw * Math.max(0.01D, scale));
        long steps = scaled / 2400L;
        if (steps <= 0L) return false;
        int amount = (int) Math.min(20L, steps);
        resourceDelta = towardZero(resourceDelta, amount);
        coverageDelta = towardZero(coverageDelta, Math.max(1, amount / 2));
        stabilityDelta = towardZero(stabilityDelta, amount);
        // Consume approximately the raw ticks represented by these ecological steps.
        long consumedRaw = Math.max(1L, (long) Math.floor((steps * 2400.0D) / Math.max(0.01D, scale)));
        lastUpdate = Math.min(now, lastUpdate + consumedRaw);
        return true;
    }

    private static int towardZero(int value, int amount) {
        if (value > 0) return Math.max(0, value - amount);
        if (value < 0) return Math.min(0, value + amount);
        return 0;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("key", key);
        tag.putInt("resourceDelta", resourceDelta);
        tag.putInt("coverageDelta", coverageDelta);
        tag.putInt("stabilityDelta", stabilityDelta);
        tag.putInt("vegetationDamagePoints", vegetationDamagePoints);
        tag.putInt("terrainDamagePoints", terrainDamagePoints);
        tag.putLong("lastUpdate", lastUpdate);
        tag.putLong("lastFireImpact", lastFireImpact);
        return tag;
    }

    public static EcoRegionState load(CompoundTag tag) {
        EcoRegionState state = new EcoRegionState(tag.getLong("key"), tag.getLong("lastUpdate"));
        state.resourceDelta = Mth.clamp(tag.getInt("resourceDelta"), -50, 50);
        state.coverageDelta = Mth.clamp(tag.getInt("coverageDelta"), -50, 50);
        state.stabilityDelta = Mth.clamp(tag.getInt("stabilityDelta"), -50, 50);
        state.vegetationDamagePoints = Math.max(0, tag.getInt("vegetationDamagePoints"));
        state.terrainDamagePoints = Math.max(0, tag.getInt("terrainDamagePoints"));
        state.lastFireImpact = tag.getLong("lastFireImpact");
        return state;
    }
}
