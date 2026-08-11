package com.livingecology.territory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public final class RelationRecord {
    private final long a;
    private final long b;
    private int rivalryDelta;
    private int affinityDelta;
    private int warMomentum;
    private long lastAggression;
    private long lastCooperation;
    private long lastResolution;

    public RelationRecord(long first, long second) {
        this.a = Math.min(first, second);
        this.b = Math.max(first, second);
    }

    public long a() { return a; }
    public long b() { return b; }
    public int rivalryDelta() { return rivalryDelta; }
    public int affinityDelta() { return affinityDelta; }
    public int warMomentum() { return warMomentum; }
    public long lastAggression() { return lastAggression; }
    public long lastCooperation() { return lastCooperation; }
    public long lastResolution() { return lastResolution; }
    public void setLastResolution(long now) { lastResolution = now; }

    public void addRivalry(int amount, long now) {
        adjustRivalry(amount);
        lastAggression = now;
    }

    public void adjustRivalry(int amount) {
        rivalryDelta = Mth.clamp(rivalryDelta + amount, -100, 100);
    }

    public void addAffinity(int amount, long now) {
        adjustAffinity(amount);
        lastCooperation = now;
    }

    public void adjustAffinity(int amount) {
        affinityDelta = Mth.clamp(affinityDelta + amount, -100, 100);
    }

    public void addMomentum(int amount) {
        warMomentum = Mth.clamp(warMomentum + amount, -10, 10);
    }

    public void resetMomentum() { warMomentum = 0; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("a", a);
        tag.putLong("b", b);
        tag.putInt("rivalryDelta", rivalryDelta);
        tag.putInt("affinityDelta", affinityDelta);
        tag.putInt("warMomentum", warMomentum);
        tag.putLong("lastAggression", lastAggression);
        tag.putLong("lastCooperation", lastCooperation);
        tag.putLong("lastResolution", lastResolution);
        return tag;
    }

    public static RelationRecord load(CompoundTag tag) {
        RelationRecord relation = new RelationRecord(tag.getLong("a"), tag.getLong("b"));
        relation.rivalryDelta = tag.getInt("rivalryDelta");
        relation.affinityDelta = tag.getInt("affinityDelta");
        relation.warMomentum = tag.getInt("warMomentum");
        relation.lastAggression = tag.getLong("lastAggression");
        relation.lastCooperation = tag.getLong("lastCooperation");
        relation.lastResolution = tag.getLong("lastResolution");
        return relation;
    }
}
