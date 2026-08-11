package com.livingecology.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Vanilla 1.20.1 creature catalogue used by Living Ecology.
 * Matching is registry-id based so special/internal vanilla entities (Giant, Illusioner,
 * Zombie Horse) work without a giant instanceof chain.
 */
public enum SpeciesType {
    AXOLOTL("axolotl", "Axolotl"),
    BAT("bat", "Bat"),
    BEE("bee", "Bee"),
    CAMEL("camel", "Camel"),
    CAT("cat", "Cat"),
    CHICKEN("chicken", "Chicken"),
    COD("cod", "Cod"),
    COW("cow", "Cow"),
    DOLPHIN("dolphin", "Dolphin"),
    DONKEY("donkey", "Donkey"),
    FOX("fox", "Fox"),
    FROG("frog", "Frog"),
    GOAT("goat", "Goat"),
    HORSE("horse", "Horse"),
    LLAMA("llama", "Llama"),
    MULE("mule", "Mule"),
    OCELOT("ocelot", "Ocelot"),
    PANDA("panda", "Panda"),
    PARROT("parrot", "Parrot"),
    PIG("pig", "Pig"),
    POLAR_BEAR("polar_bear", "Polar Bear"),
    PUFFERFISH("pufferfish", "Pufferfish"),
    RABBIT("rabbit", "Rabbit"),
    SALMON("salmon", "Salmon"),
    SHEEP("sheep", "Sheep"),
    SQUID("squid", "Squid"),
    TADPOLE("tadpole", "Tadpole"),
    TROPICAL_FISH("tropical_fish", "Tropical Fish"),
    TURTLE("turtle", "Turtle"),
    WOLF("wolf", "Wolf"),
    ALLAY("allay", "Allay"),
    GLOW_SQUID("glow_squid", "Glow Squid"),
    MOOSHROOM("mooshroom", "Mooshroom"),
    SNIFFER("sniffer", "Sniffer"),
    STRIDER("strider", "Strider"),
    BLAZE("blaze", "Blaze"),
    CAVE_SPIDER("cave_spider", "Cave Spider"),
    CREEPER("creeper", "Creeper"),
    DROWNED("drowned", "Drowned"),
    ENDERMAN("enderman", "Enderman"),
    ENDERMITE("endermite", "Endermite"),
    GHAST("ghast", "Ghast"),
    HOGLIN("hoglin", "Hoglin"),
    HUSK("husk", "Husk"),
    MAGMA_CUBE("magma_cube", "Magma Cube"),
    PHANTOM("phantom", "Phantom"),
    RAVAGER("ravager", "Ravager"),
    SHULKER("shulker", "Shulker"),
    SILVERFISH("silverfish", "Silverfish"),
    SKELETON("skeleton", "Skeleton"),
    SLIME("slime", "Slime"),
    SPIDER("spider", "Spider"),
    STRAY("stray", "Stray"),
    VEX("vex", "Vex"),
    WARDEN("warden", "Warden"),
    WITHER_SKELETON("wither_skeleton", "Wither Skeleton"),
    ZOGLIN("zoglin", "Zoglin"),
    ZOMBIE("zombie", "Zombie"),
    ZOMBIE_VILLAGER("zombie_villager", "Zombie Villager"),
    ZOMBIFIED_PIGLIN("zombified_piglin", "Zombified Piglin"),
    EVOKER("evoker", "Evoker"),
    PIGLIN("piglin", "Piglin"),
    PIGLIN_BRUTE("piglin_brute", "Piglin Brute"),
    PILLAGER("pillager", "Pillager"),
    VILLAGER("villager", "Villager"),
    VINDICATOR("vindicator", "Vindicator"),
    WANDERING_TRADER("wandering_trader", "Wandering Trader"),
    WITCH("witch", "Witch"),
    ELDER_GUARDIAN("elder_guardian", "Elder Guardian"),
    GUARDIAN("guardian", "Guardian"),
    IRON_GOLEM("iron_golem", "Iron Golem"),
    SNOW_GOLEM("snow_golem", "Snow Golem"),
    TRADER_LLAMA("trader_llama", "Trader Llama"),
    ENDER_DRAGON("ender_dragon", "Ender Dragon"),
    WITHER("wither", "Wither"),
    GIANT("giant", "Giant"),
    ILLUSIONER("illusioner", "Illusioner"),
    SKELETON_HORSE("skeleton_horse", "Skeleton Horse"),
    ZOMBIE_HORSE("zombie_horse", "Zombie Horse");

    private static final Map<String, SpeciesType> BY_MINECRAFT_ID = new HashMap<>();
    static {
        for (SpeciesType value : values()) BY_MINECRAFT_ID.put(value.minecraftId, value);
    }

    private final String minecraftId;
    private final String displayName;

    SpeciesType(String minecraftId, String displayName) {
        this.minecraftId = minecraftId;
        this.displayName = displayName;
    }

    public String minecraftId() { return minecraftId; }
    public String id() { return name(); }
    public String displayName() { return displayName; }

    public static Optional<SpeciesType> from(Entity entity) {
        if (!(entity instanceof Mob)) return Optional.empty();
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (key == null || !"minecraft".equals(key.getNamespace())) return Optional.empty();
        return Optional.ofNullable(BY_MINECRAFT_ID.get(key.getPath()));
    }

    public boolean matches(Entity entity) {
        return from(entity).orElse(null) == this;
    }
}
