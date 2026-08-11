package com.livingecology.territory;

import com.livingecology.data.FootprintType;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.UUID;

public final class TerritoryRecord {
    private final long id;
    private final SpeciesType species;
    private BlockPos center;
    private BlockPos core;
    private int radiusChunks;
    private int pressure;
    private int maturity;
    private int population;
    private final long seed;
    private TerritoryState state;
    private long lastRawUpdate;
    private double simulatedTickBuffer;
    private int footprintProgress;
    private int footprintCursor;
    private UUID leader;

    public TerritoryRecord(long id, SpeciesType species, BlockPos center, BlockPos core,
                           int radiusChunks, int pressure, int maturity, int population,
                           long seed, long now) {
        this.id = id;
        this.species = species;
        this.center = center.immutable();
        this.core = core.immutable();
        this.radiusChunks = Mth.clamp(radiusChunks, 1, 12);
        this.pressure = Mth.clamp(pressure, 0, 100);
        this.maturity = Mth.clamp(maturity, 0, 100);
        this.population = Math.max(0, population);
        this.seed = seed;
        this.state = TerritoryState.ACTIVE;
        this.lastRawUpdate = now;
    }

    public long id() { return id; }
    public SpeciesType species() { return species; }
    public BlockPos center() { return center; }
    public BlockPos core() { return core; }
    public int radiusChunks() { return radiusChunks; }
    public int pressure() { return pressure; }
    public int maturity() { return maturity; }
    public int population() { return population; }
    public long seed() { return seed; }
    public TerritoryState state() { return state; }
    public long lastRawUpdate() { return lastRawUpdate; }
    public double simulatedTickBuffer() { return simulatedTickBuffer; }
    public int footprintProgress() { return footprintProgress; }
    public int footprintCursor() { return footprintCursor; }
    public UUID leader() { return leader; }

    public void setCenter(BlockPos center) { this.center = center.immutable(); }
    public void setCore(BlockPos core) { this.core = core.immutable(); }
    public void setRadiusChunks(int radiusChunks) { this.radiusChunks = Mth.clamp(radiusChunks, 1, 12); }
    public void setPressure(int pressure) { this.pressure = Mth.clamp(pressure, 0, 100); }
    public void addPressure(int delta) { setPressure(pressure + delta); }
    public void setMaturity(int maturity) { this.maturity = Mth.clamp(maturity, 0, 100); }
    public void setPopulation(int population) { this.population = Math.max(0, population); }
    public void setState(TerritoryState state) { this.state = state; }
    public void setLastRawUpdate(long lastRawUpdate) { this.lastRawUpdate = lastRawUpdate; }
    public void setSimulatedTickBuffer(double simulatedTickBuffer) { this.simulatedTickBuffer = Math.max(0.0D, simulatedTickBuffer); }
    public void setFootprintProgress(int value) { this.footprintProgress = Math.max(0, value); }
    public void addFootprintProgress(int delta) { setFootprintProgress(footprintProgress + delta); }
    public int nextFootprintCursor() { return footprintCursor++; }
    public void setLeader(UUID leader) { this.leader = leader; }

    public int desiredFootprint() {
        FootprintType type = SpeciesProfile.of(species).footprintType();
        if (type == FootprintType.NONE || state == TerritoryState.ABANDONED || population <= 0) return 0;
        int base = switch (type) {
            case COBWEB -> 6;
            case FLOWERS -> 4;
            case TRAIL -> 3;
            case BURROW -> 3;
            case MUSHROOMS -> 4;
            case NONE -> 0;
        };
        int cap = switch (type) {
            case COBWEB -> 96;
            case FLOWERS -> 48;
            case TRAIL -> 36;
            case BURROW -> 28;
            case MUSHROOMS -> 44;
            case NONE -> 0;
        };
        return Mth.clamp(base + maturity / 5 + pressure / 7 + population, base, cap);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("id", id);
        tag.putString("species", species.name());
        tag.putLong("center", center.asLong());
        tag.putLong("core", core.asLong());
        tag.putInt("radiusChunks", radiusChunks);
        tag.putByte("pressure", (byte) pressure);
        tag.putByte("maturity", (byte) maturity);
        tag.putInt("population", population);
        tag.putLong("seed", seed);
        tag.putString("state", state.name());
        tag.putLong("lastRawUpdate", lastRawUpdate);
        tag.putDouble("simulatedTickBuffer", simulatedTickBuffer);
        tag.putInt("footprintProgress", footprintProgress);
        tag.putInt("footprintCursor", footprintCursor);
        if (leader != null) tag.putUUID("leader", leader);
        return tag;
    }

    public static TerritoryRecord load(CompoundTag tag) {
        SpeciesType species;
        try { species = SpeciesType.valueOf(tag.getString("species")); }
        catch (IllegalArgumentException ex) { species = SpeciesType.COW; }
        TerritoryRecord r = new TerritoryRecord(
                tag.getLong("id"), species, BlockPos.of(tag.getLong("center")), BlockPos.of(tag.getLong("core")),
                tag.getInt("radiusChunks"), tag.getByte("pressure") & 0xFF, tag.getByte("maturity") & 0xFF,
                tag.getInt("population"), tag.getLong("seed"), tag.getLong("lastRawUpdate"));
        try { r.state = TerritoryState.valueOf(tag.getString("state")); }
        catch (IllegalArgumentException ignored) { r.state = TerritoryState.ACTIVE; }
        r.simulatedTickBuffer = Math.max(0.0D, tag.getDouble("simulatedTickBuffer"));
        r.footprintProgress = Math.max(0, tag.getInt("footprintProgress"));
        r.footprintCursor = Math.max(0, tag.getInt("footprintCursor"));
        if (tag.hasUUID("leader")) r.leader = tag.getUUID("leader");
        return r;
    }
}
