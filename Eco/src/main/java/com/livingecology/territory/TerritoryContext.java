package com.livingecology.territory;

public record TerritoryContext(
        TerritoryRecord own,
        TerritoryZone ownZone,
        TerritoryRecord strongest,
        TerritoryRecord second,
        boolean noMansLand,
        boolean contested,
        int tension,
        int affinity) {

    public static TerritoryContext empty() {
        return new TerritoryContext(null, TerritoryZone.OUTSIDE, null, null, false, false, 0, 0);
    }
}
