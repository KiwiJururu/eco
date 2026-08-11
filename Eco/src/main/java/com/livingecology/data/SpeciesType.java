package com.livingecology.data;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.Optional;

public enum SpeciesType {
    COW("Cow", "Vaca"),
    WOLF("Wolf", "Lobo"),
    SPIDER("Spider", "Aranha"),
    ZOMBIE("Zombie", "Zumbi");

    private final String id;
    private final String displayName;

    SpeciesType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }

    public static Optional<SpeciesType> from(Entity entity) {
        if (!(entity instanceof Mob)) return Optional.empty();
        if (entity.getType() == EntityType.COW) return Optional.of(COW);
        if (entity.getType() == EntityType.WOLF) return Optional.of(WOLF);
        if (entity.getType() == EntityType.SPIDER) return Optional.of(SPIDER);
        if (entity.getType() == EntityType.ZOMBIE) return Optional.of(ZOMBIE);
        return Optional.empty();
    }

    public boolean matches(Entity entity) {
        return from(entity).orElse(null) == this;
    }
}
