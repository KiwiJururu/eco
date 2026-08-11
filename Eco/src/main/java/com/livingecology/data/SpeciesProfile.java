package com.livingecology.data;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Data-driven implementation profile for every vanilla Java 1.20.1 creature covered by
 * Documento Geral v0.2. Values are intentionally gameplay parameters, not vanilla attributes.
 */
public final class SpeciesProfile {
    public record IntRange(int min, int max) {
        public IntRange {
            if (min < 0 || max > 100 || min > max) throw new IllegalArgumentException("Faixa inválida: " + min + "-" + max);
        }
        public int midpoint() { return (min + max) / 2; }
    }

    private static final EnumMap<SpeciesType, SpeciesProfile> PROFILES = new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.AXOLOTL, ranges(25, 45, 30, 50, 55, 70, 45, 65, 45, 65, 60, 75, 30, 45), List.of(Personality.NORMAL, Personality.PLAYFUL, Personality.CURIOUS, Personality.CAUTIOUS), false, 2, 31, 3, BehaviorFamily.AQUATIC, MovementDomain.AMPHIBIOUS, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.WATER, ActivityPattern.VARIABLE);
        put(SpeciesType.BAT, ranges(45, 65, 45, 60, 85, 100, 60, 80, 65, 85, 65, 80, 15, 30), List.of(Personality.NORMAL, Personality.WORRIED, Personality.SOCIABLE, Personality.CAUTIOUS), false, 1, 34, 5, BehaviorFamily.FLYING_PASSIVE, MovementDomain.AIR, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.CAVE, ActivityPattern.NOCTURNAL);
        put(SpeciesType.BEE, ranges(80, 95, 55, 75, 65, 80, 70, 90, 90, 100, 75, 90, 20, 35), List.of(Personality.NORMAL, Personality.DILIGENT, Personality.WORRIED, Personality.PROTECTIVE, Personality.AGGRESSIVE), false, 2, 40, 7, BehaviorFamily.COLONY, MovementDomain.AIR, TerritoryStyle.NEST, FootprintType.FLOWERS, ReproductionMode.VANILLA_LOVE, HabitatClass.FOREST, ActivityPattern.DIURNAL);
        put(SpeciesType.CAMEL, ranges(25, 45, 60, 80, 55, 70, 40, 60, 65, 80, 65, 80, 75, 90), List.of(Personality.NORMAL, Personality.LAZY, Personality.CAUTIOUS, Personality.BOLD, Personality.SOCIABLE), false, 2, 34, 3, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.DESERT, ActivityPattern.DIURNAL);
        put(SpeciesType.CAT, ranges(55, 75, 70, 90, 75, 90, 55, 75, 45, 70, 75, 90, 30, 45), List.of(Personality.NORMAL, Personality.LAZY, Personality.PLAYFUL, Personality.CURIOUS, Personality.CAUTIOUS, Personality.INDEPENDENT), false, 1, 31, 5, BehaviorFamily.PREDATOR, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.VILLAGE, ActivityPattern.VARIABLE);
        put(SpeciesType.CHICKEN, ranges(20, 40, 25, 45, 50, 65, 55, 75, 65, 85, 50, 65, 10, 25), List.of(Personality.NORMAL, Personality.WORRIED, Personality.SOCIABLE, Personality.CURIOUS, Personality.PLAYFUL), false, 2, 34, 3, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.GENERAL, ActivityPattern.DIURNAL);
        put(SpeciesType.COD, ranges(0, 15, 10, 25, 45, 60, 45, 65, 75, 95, 45, 60, 10, 20), List.of(Personality.NORMAL, Personality.SOCIABLE, Personality.WORRIED), false, 999, 16, 0, BehaviorFamily.AQUATIC, MovementDomain.WATER, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.OCEAN, ActivityPattern.VARIABLE);
        put(SpeciesType.COW, ranges(30, 50, 45, 65, 55, 70, 45, 65, 80, 95, 55, 70, 55, 70), List.of(Personality.NORMAL, Personality.LAZY, Personality.WORRIED, Personality.SOCIABLE, Personality.CAUTIOUS, Personality.BOLD), false, 2, 42, 5, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.GROUP, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.GENERAL, ActivityPattern.DIURNAL);
        put(SpeciesType.DOLPHIN, ranges(20, 40, 75, 95, 80, 95, 65, 85, 85, 100, 80, 95, 55, 70), List.of(Personality.NORMAL, Personality.PLAYFUL, Personality.CURIOUS, Personality.SOCIABLE, Personality.PROTECTIVE, Personality.BOLD), false, 2, 34, 3, BehaviorFamily.AQUATIC, MovementDomain.WATER, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.OCEAN, ActivityPattern.VARIABLE);
        put(SpeciesType.DONKEY, ranges(35, 55, 70, 90, 55, 70, 50, 70, 55, 75, 70, 85, 65, 80), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.STUBBORN, Personality.SOCIABLE, Personality.LAZY), false, 2, 33, 4, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.GENERAL, ActivityPattern.DIURNAL);
        put(SpeciesType.FOX, ranges(45, 65, 65, 85, 80, 95, 70, 90, 30, 55, 80, 95, 30, 45), List.of(Personality.NORMAL, Personality.CURIOUS, Personality.CAUTIOUS, Personality.INDEPENDENT, Personality.PLAYFUL, Personality.BOLD), false, 1, 28, 5, BehaviorFamily.PREDATOR, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.FOREST, ActivityPattern.NOCTURNAL);
        put(SpeciesType.FROG, ranges(20, 40, 20, 40, 60, 75, 45, 65, 20, 40, 60, 75, 20, 35), List.of(Personality.NORMAL, Personality.LAZY, Personality.CURIOUS, Personality.CAUTIOUS), false, 2, 26, 3, BehaviorFamily.PASSIVE_WANDERER, MovementDomain.AMPHIBIOUS, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.SWAMP, ActivityPattern.VARIABLE);
        put(SpeciesType.GOAT, ranges(40, 60, 60, 80, 65, 80, 60, 80, 55, 75, 80, 95, 60, 80), List.of(Personality.NORMAL, Personality.BOLD, Personality.STUBBORN, Personality.CAUTIOUS, Personality.AGGRESSIVE, Personality.SOCIABLE), true, 2, 33, 4, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.MOUNTAIN, ActivityPattern.DIURNAL);
        put(SpeciesType.HORSE, ranges(35, 55, 60, 80, 60, 75, 55, 80, 70, 90, 65, 85, 55, 80), List.of(Personality.NORMAL, Personality.WORRIED, Personality.BOLD, Personality.SOCIABLE, Personality.CAUTIOUS, Personality.PLAYFUL), false, 2, 34, 4, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.GENERAL, ActivityPattern.DIURNAL);
        put(SpeciesType.LLAMA, ranges(40, 60, 55, 75, 60, 75, 55, 75, 70, 90, 65, 80, 55, 70), List.of(Personality.NORMAL, Personality.PROTECTIVE, Personality.SOCIABLE, Personality.CAUTIOUS, Personality.STUBBORN), false, 2, 34, 4, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.MOUNTAIN, ActivityPattern.DIURNAL);
        put(SpeciesType.MULE, ranges(35, 55, 75, 90, 55, 70, 50, 70, 55, 75, 75, 90, 70, 85), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.STUBBORN, Personality.BOLD, Personality.SOCIABLE), false, 2, 33, 4, BehaviorFamily.PASSIVE_WANDERER, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.GENERAL, ActivityPattern.DIURNAL);
        put(SpeciesType.OCELOT, ranges(55, 75, 70, 90, 85, 100, 75, 95, 15, 35, 85, 100, 35, 50), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.INDEPENDENT, Personality.CURIOUS, Personality.BOLD), false, 1, 25, 5, BehaviorFamily.PREDATOR, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.FOREST, ActivityPattern.NOCTURNAL);
        put(SpeciesType.PANDA, ranges(35, 55, 50, 70, 50, 65, 35, 60, 40, 65, 55, 70, 55, 80), List.of(Personality.NORMAL, Personality.LAZY, Personality.WORRIED, Personality.PLAYFUL, Personality.AGGRESSIVE, Personality.WEAK, Personality.CURIOUS, Personality.PROTECTIVE), false, 2, 30, 4, BehaviorFamily.PASSIVE_WANDERER, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.FOREST, ActivityPattern.VARIABLE);
        put(SpeciesType.PARROT, ranges(35, 55, 70, 90, 80, 95, 70, 90, 80, 95, 75, 90, 15, 30), List.of(Personality.NORMAL, Personality.PLAYFUL, Personality.CURIOUS, Personality.SOCIABLE, Personality.WORRIED, Personality.CAUTIOUS), false, 2, 34, 4, BehaviorFamily.FLYING_PASSIVE, MovementDomain.AIR, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.FOREST, ActivityPattern.DIURNAL);
        put(SpeciesType.PIG, ranges(20, 40, 35, 55, 50, 65, 45, 65, 65, 85, 50, 65, 40, 55), List.of(Personality.NORMAL, Personality.CURIOUS, Personality.SOCIABLE, Personality.LAZY, Personality.WORRIED), false, 2, 34, 3, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.GENERAL, ActivityPattern.DIURNAL);
        put(SpeciesType.POLAR_BEAR, ranges(60, 80, 60, 80, 75, 90, 65, 85, 15, 35, 85, 100, 90, 100), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.BOLD, Personality.PROTECTIVE, Personality.AGGRESSIVE), true, 1, 25, 5, BehaviorFamily.PREDATOR, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.COLD, ActivityPattern.VARIABLE);
        put(SpeciesType.PUFFERFISH, ranges(5, 20, 5, 20, 50, 65, 55, 75, 10, 30, 70, 85, 20, 35), List.of(Personality.NORMAL, Personality.WORRIED, Personality.CAUTIOUS), false, 999, 16, 0, BehaviorFamily.AQUATIC, MovementDomain.WATER, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.OCEAN, ActivityPattern.VARIABLE);
        put(SpeciesType.RABBIT, ranges(35, 55, 40, 60, 75, 90, 80, 95, 35, 60, 85, 100, 10, 25), List.of(Personality.NORMAL, Personality.WORRIED, Personality.CAUTIOUS, Personality.SOCIABLE, Personality.CURIOUS), false, 2, 29, 4, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.BURROW, ReproductionMode.VANILLA_LOVE, HabitatClass.GENERAL, ActivityPattern.VARIABLE);
        put(SpeciesType.SALMON, ranges(5, 20, 20, 35, 50, 65, 50, 70, 70, 90, 55, 70, 20, 35), List.of(Personality.NORMAL, Personality.SOCIABLE, Personality.WORRIED), false, 999, 16, 0, BehaviorFamily.AQUATIC, MovementDomain.WATER, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.WATER, ActivityPattern.VARIABLE);
        put(SpeciesType.SHEEP, ranges(25, 45, 35, 55, 55, 70, 60, 80, 90, 100, 60, 75, 35, 50), List.of(Personality.NORMAL, Personality.WORRIED, Personality.SOCIABLE, Personality.CAUTIOUS, Personality.LAZY), false, 2, 34, 3, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.GENERAL, ActivityPattern.DIURNAL);
        put(SpeciesType.SQUID, ranges(5, 20, 20, 40, 55, 70, 50, 70, 35, 55, 65, 80, 20, 35), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.WORRIED), false, 999, 16, 0, BehaviorFamily.AQUATIC, MovementDomain.WATER, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.WATER, ActivityPattern.VARIABLE);
        put(SpeciesType.TADPOLE, ranges(0, 15, 5, 20, 40, 55, 50, 70, 65, 85, 50, 65, 5, 15), List.of(Personality.NORMAL, Personality.SOCIABLE, Personality.WORRIED), false, 999, 16, 0, BehaviorFamily.AQUATIC, MovementDomain.WATER, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.WATER, ActivityPattern.VARIABLE);
        put(SpeciesType.TROPICAL_FISH, ranges(5, 20, 10, 25, 50, 65, 50, 70, 80, 95, 50, 65, 10, 20), List.of(Personality.NORMAL, Personality.SOCIABLE, Personality.WORRIED), false, 999, 16, 0, BehaviorFamily.AQUATIC, MovementDomain.WATER, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.OCEAN, ActivityPattern.VARIABLE);
        put(SpeciesType.TURTLE, ranges(70, 90, 80, 95, 55, 70, 50, 70, 30, 50, 75, 90, 55, 75), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.STUBBORN, Personality.PROTECTIVE), false, 1, 28, 6, BehaviorFamily.PASSIVE_WANDERER, MovementDomain.AMPHIBIOUS, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.WATER, ActivityPattern.VARIABLE);
        put(SpeciesType.WOLF, ranges(60, 80, 75, 95, 80, 95, 70, 90, 85, 100, 85, 100, 55, 75), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.BOLD, Personality.PROTECTIVE, Personality.SOCIABLE, Personality.AGGRESSIVE), true, 2, 42, 7, BehaviorFamily.PREDATOR, MovementDomain.LAND, TerritoryStyle.GROUP, FootprintType.TRAIL, ReproductionMode.NONE, HabitatClass.FOREST, ActivityPattern.VARIABLE);
        put(SpeciesType.ALLAY, ranges(30, 50, 75, 90, 70, 85, 55, 75, 85, 100, 65, 80, 15, 30), List.of(Personality.NORMAL, Personality.PLAYFUL, Personality.CURIOUS, Personality.SOCIABLE, Personality.CAUTIOUS), false, 999, 16, 0, BehaviorFamily.FLYING_PASSIVE, MovementDomain.AIR, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.AERIAL, ActivityPattern.VARIABLE);
        put(SpeciesType.GLOW_SQUID, ranges(10, 25, 20, 40, 60, 75, 45, 65, 40, 60, 55, 70, 20, 35), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.CURIOUS), false, 999, 16, 0, BehaviorFamily.AQUATIC, MovementDomain.WATER, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.DEEP_WATER, ActivityPattern.VARIABLE);
        put(SpeciesType.MOOSHROOM, ranges(45, 65, 50, 70, 50, 65, 40, 60, 80, 95, 55, 70, 55, 70), List.of(Personality.NORMAL, Personality.LAZY, Personality.SOCIABLE, Personality.CAUTIOUS), false, 1, 34, 5, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.MUSHROOMS, ReproductionMode.VANILLA_LOVE, HabitatClass.MUSHROOM, ActivityPattern.DIURNAL);
        put(SpeciesType.SNIFFER, ranges(35, 55, 70, 90, 80, 95, 35, 55, 55, 75, 70, 85, 80, 95), List.of(Personality.NORMAL, Personality.CURIOUS, Personality.LAZY, Personality.SOCIABLE, Personality.CAUTIOUS), false, 2, 33, 4, BehaviorFamily.PASSIVE_WANDERER, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.GENERAL, ActivityPattern.DIURNAL);
        put(SpeciesType.STRIDER, ranges(25, 45, 55, 75, 50, 65, 40, 60, 65, 85, 65, 80, 50, 65), List.of(Personality.NORMAL, Personality.SOCIABLE, Personality.CAUTIOUS, Personality.LAZY), false, 2, 34, 3, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAVA, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.LAVA, ActivityPattern.VARIABLE);
        put(SpeciesType.BLAZE, ranges(80, 95, 45, 65, 75, 90, 75, 90, 45, 65, 70, 85, 55, 75), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.CAUTIOUS, Personality.PROTECTIVE, Personality.STUBBORN), true, 1, 36, 8, BehaviorFamily.HOSTILE_RANGED, MovementDomain.AIR, TerritoryStyle.STRUCTURE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.NETHER, ActivityPattern.VARIABLE);
        put(SpeciesType.CAVE_SPIDER, ranges(75, 90, 40, 60, 75, 90, 75, 95, 55, 75, 80, 95, 35, 50), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.CAUTIOUS, Personality.PROTECTIVE), true, 2, 38, 7, BehaviorFamily.ARTHROPOD, MovementDomain.LAND, TerritoryStyle.NEST, FootprintType.COBWEB, ReproductionMode.NONE, HabitatClass.CAVE, ActivityPattern.NOCTURNAL);
        put(SpeciesType.CREEPER, ranges(20, 40, 55, 75, 70, 85, 65, 85, 5, 20, 75, 90, 45, 60), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.STUBBORN, Personality.CURIOUS, Personality.AGGRESSIVE), true, 999, 22, 3, BehaviorFamily.EXPLOSIVE, MovementDomain.LAND, TerritoryStyle.MOBILE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.GENERAL, ActivityPattern.NOCTURNAL);
        put(SpeciesType.DROWNED, ranges(45, 65, 30, 50, 65, 80, 60, 80, 55, 75, 60, 75, 60, 75), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN, Personality.CAUTIOUS), true, 4, 44, 6, BehaviorFamily.UNDEAD_HORDE, MovementDomain.AMPHIBIOUS, TerritoryStyle.HORDE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.WATER, ActivityPattern.NOCTURNAL);
        put(SpeciesType.ENDERMAN, ranges(20, 40, 80, 95, 85, 100, 70, 90, 25, 45, 85, 100, 70, 85), List.of(Personality.NORMAL, Personality.CURIOUS, Personality.CAUTIOUS, Personality.INDEPENDENT, Personality.AGGRESSIVE), true, 999, 22, 3, BehaviorFamily.HOSTILE_MELEE, MovementDomain.LAND, TerritoryStyle.MOBILE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.END, ActivityPattern.VARIABLE);
        put(SpeciesType.ENDERMITE, ranges(10, 25, 5, 20, 55, 70, 60, 80, 20, 40, 45, 60, 15, 25), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.WORRIED), false, 2, 29, 4, BehaviorFamily.ARTHROPOD, MovementDomain.LAND, TerritoryStyle.NEST, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.END, ActivityPattern.VARIABLE);
        put(SpeciesType.GHAST, ranges(45, 65, 45, 65, 80, 95, 65, 85, 10, 30, 70, 85, 45, 60), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.STUBBORN, Personality.AGGRESSIVE), true, 999, 16, 0, BehaviorFamily.HOSTILE_RANGED, MovementDomain.AIR, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.NETHER, ActivityPattern.VARIABLE);
        put(SpeciesType.HOGLIN, ranges(65, 85, 45, 65, 60, 75, 60, 80, 75, 90, 75, 90, 85, 100), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.PROTECTIVE, Personality.BOLD, Personality.SOCIABLE), true, 2, 42, 8, BehaviorFamily.HOSTILE_MELEE, MovementDomain.LAND, TerritoryStyle.GROUP, FootprintType.NONE, ReproductionMode.VANILLA_LOVE, HabitatClass.NETHER, ActivityPattern.VARIABLE);
        put(SpeciesType.HUSK, ranges(20, 40, 20, 40, 55, 70, 55, 75, 60, 80, 55, 70, 70, 85), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN), true, 4, 44, 5, BehaviorFamily.UNDEAD_HORDE, MovementDomain.LAND, TerritoryStyle.HORDE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.DESERT, ActivityPattern.NOCTURNAL);
        put(SpeciesType.MAGMA_CUBE, ranges(25, 45, 5, 20, 50, 65, 55, 70, 30, 50, 45, 60, 75, 95), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN), true, 999, 16, 0, BehaviorFamily.HOSTILE_MELEE, MovementDomain.LAND, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.NETHER, ActivityPattern.VARIABLE);
        put(SpeciesType.PHANTOM, ranges(15, 35, 50, 70, 85, 100, 80, 95, 65, 85, 75, 90, 35, 50), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.CAUTIOUS, Personality.SOCIABLE), true, 999, 16, 0, BehaviorFamily.FLYING_HOSTILE, MovementDomain.AIR, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.AERIAL, ActivityPattern.NOCTURNAL);
        put(SpeciesType.RAVAGER, ranges(35, 55, 55, 75, 65, 80, 65, 80, 60, 80, 75, 90, 95, 100), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN, Personality.PROTECTIVE, Personality.BOLD), true, 2, 48, 6, BehaviorFamily.SOCIETY_HOSTILE, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.VILLAGE, ActivityPattern.VARIABLE);
        put(SpeciesType.SHULKER, ranges(95, 100, 65, 85, 75, 90, 70, 90, 45, 65, 75, 90, 85, 100), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.PROTECTIVE, Personality.STUBBORN), true, 1, 36, 8, BehaviorFamily.HOSTILE_RANGED, MovementDomain.STATIC, TerritoryStyle.STRUCTURE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.END, ActivityPattern.VARIABLE);
        put(SpeciesType.SILVERFISH, ranges(75, 90, 10, 25, 60, 75, 70, 85, 85, 100, 60, 75, 10, 20), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.SOCIABLE, Personality.WORRIED), false, 2, 40, 7, BehaviorFamily.ARTHROPOD, MovementDomain.LAND, TerritoryStyle.NEST, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.CAVE, ActivityPattern.VARIABLE);
        put(SpeciesType.SKELETON, ranges(20, 40, 45, 65, 80, 95, 70, 90, 55, 75, 75, 90, 45, 60), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.AGGRESSIVE, Personality.STUBBORN), true, 999, 22, 3, BehaviorFamily.UNDEAD_COMBAT, MovementDomain.LAND, TerritoryStyle.MOBILE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.GENERAL, ActivityPattern.NOCTURNAL);
        put(SpeciesType.SLIME, ranges(10, 30, 0, 15, 45, 60, 50, 65, 35, 55, 35, 50, 55, 85), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN), true, 999, 16, 0, BehaviorFamily.HOSTILE_MELEE, MovementDomain.LAND, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.SWAMP, ActivityPattern.VARIABLE);
        put(SpeciesType.SPIDER, ranges(45, 65, 35, 55, 75, 90, 70, 90, 20, 40, 80, 95, 40, 55), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.AGGRESSIVE, Personality.INDEPENDENT), true, 2, 29, 6, BehaviorFamily.ARTHROPOD, MovementDomain.LAND, TerritoryStyle.NEST, FootprintType.COBWEB, ReproductionMode.NONE, HabitatClass.CAVE, ActivityPattern.NOCTURNAL);
        put(SpeciesType.STRAY, ranges(25, 45, 50, 70, 80, 95, 75, 90, 55, 75, 80, 95, 50, 65), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.AGGRESSIVE, Personality.STUBBORN), true, 999, 22, 3, BehaviorFamily.UNDEAD_COMBAT, MovementDomain.LAND, TerritoryStyle.MOBILE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.COLD, ActivityPattern.NOCTURNAL);
        put(SpeciesType.VEX, ranges(20, 40, 40, 60, 80, 95, 75, 90, 70, 90, 75, 90, 15, 30), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.SOCIABLE, Personality.STUBBORN), false, 999, 16, 0, BehaviorFamily.FLYING_HOSTILE, MovementDomain.AIR, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.AERIAL, ActivityPattern.VARIABLE);
        put(SpeciesType.WARDEN, ranges(90, 100, 75, 90, 100, 100, 95, 100, 0, 10, 95, 100, 100, 100), List.of(Personality.NORMAL, Personality.STUBBORN, Personality.AGGRESSIVE, Personality.CAUTIOUS), false, 999, 22, 7, BehaviorFamily.HOSTILE_MELEE, MovementDomain.LAND, TerritoryStyle.MOBILE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.CAVE, ActivityPattern.VARIABLE);
        put(SpeciesType.WITHER_SKELETON, ranges(80, 95, 50, 70, 75, 90, 75, 90, 65, 85, 80, 95, 75, 90), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.PROTECTIVE, Personality.STUBBORN), true, 1, 36, 8, BehaviorFamily.UNDEAD_COMBAT, MovementDomain.LAND, TerritoryStyle.STRUCTURE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.NETHER, ActivityPattern.VARIABLE);
        put(SpeciesType.ZOGLIN, ranges(30, 50, 10, 25, 60, 75, 75, 90, 0, 15, 55, 70, 90, 100), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN, Personality.BOLD), true, 2, 28, 5, BehaviorFamily.HOSTILE_MELEE, MovementDomain.LAND, TerritoryStyle.GROUP, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.NETHER, ActivityPattern.VARIABLE);
        put(SpeciesType.ZOMBIE, ranges(10, 30, 15, 35, 50, 70, 55, 75, 75, 95, 45, 60, 65, 80), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN, Personality.SOCIABLE), true, 4, 44, 5, BehaviorFamily.UNDEAD_HORDE, MovementDomain.LAND, TerritoryStyle.HORDE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.GENERAL, ActivityPattern.NOCTURNAL);
        put(SpeciesType.ZOMBIE_VILLAGER, ranges(20, 40, 25, 45, 55, 70, 55, 75, 70, 90, 50, 65, 60, 75), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN, Personality.SOCIABLE), true, 4, 44, 5, BehaviorFamily.UNDEAD_HORDE, MovementDomain.LAND, TerritoryStyle.HORDE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.VILLAGE, ActivityPattern.NOCTURNAL);
        put(SpeciesType.ZOMBIFIED_PIGLIN, ranges(20, 40, 25, 45, 60, 75, 60, 80, 90, 100, 55, 70, 70, 85), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.SOCIABLE, Personality.STUBBORN), true, 4, 44, 5, BehaviorFamily.UNDEAD_COMBAT, MovementDomain.LAND, TerritoryStyle.HORDE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.NETHER, ActivityPattern.VARIABLE);
        put(SpeciesType.EVOKER, ranges(70, 90, 80, 95, 80, 95, 75, 90, 80, 95, 90, 100, 45, 60), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.PROTECTIVE, Personality.AGGRESSIVE, Personality.STUBBORN), true, 2, 48, 8, BehaviorFamily.SOCIETY_HOSTILE, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.STRUCTURE, ActivityPattern.VARIABLE);
        put(SpeciesType.PIGLIN, ranges(65, 85, 80, 95, 75, 90, 70, 90, 90, 100, 85, 100, 60, 80), List.of(Personality.NORMAL, Personality.CURIOUS, Personality.CAUTIOUS, Personality.AGGRESSIVE, Personality.SOCIABLE, Personality.BOLD), true, 2, 48, 8, BehaviorFamily.SOCIETY_HOSTILE, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.NETHER, ActivityPattern.VARIABLE);
        put(SpeciesType.PIGLIN_BRUTE, ranges(90, 100, 80, 95, 80, 95, 80, 95, 85, 100, 90, 100, 85, 100), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.PROTECTIVE, Personality.STUBBORN, Personality.BOLD), true, 2, 48, 9, BehaviorFamily.SOCIETY_HOSTILE, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.NETHER, ActivityPattern.VARIABLE);
        put(SpeciesType.PILLAGER, ranges(60, 80, 70, 90, 80, 95, 75, 90, 85, 100, 85, 100, 50, 65), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.AGGRESSIVE, Personality.SOCIABLE, Personality.PROTECTIVE), true, 2, 48, 8, BehaviorFamily.SOCIETY_HOSTILE, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.VILLAGE, ActivityPattern.VARIABLE);
        put(SpeciesType.VILLAGER, ranges(75, 95, 85, 100, 65, 80, 60, 80, 95, 100, 80, 95, 25, 40), List.of(Personality.NORMAL, Personality.LAZY, Personality.WORRIED, Personality.SOCIABLE, Personality.CURIOUS, Personality.CAUTIOUS, Personality.PROTECTIVE), false, 2, 48, 9, BehaviorFamily.SOCIETY_PEACEFUL, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.VILLAGE, ActivityPattern.DIURNAL);
        put(SpeciesType.VINDICATOR, ranges(65, 85, 65, 85, 75, 90, 75, 90, 85, 100, 85, 100, 75, 90), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.PROTECTIVE, Personality.BOLD, Personality.STUBBORN), true, 2, 48, 8, BehaviorFamily.SOCIETY_HOSTILE, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.STRUCTURE, ActivityPattern.VARIABLE);
        put(SpeciesType.WANDERING_TRADER, ranges(10, 30, 85, 100, 75, 90, 80, 95, 60, 80, 85, 100, 30, 45), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.CURIOUS, Personality.SOCIABLE, Personality.WORRIED), false, 2, 48, 5, BehaviorFamily.SOCIETY_PEACEFUL, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.VILLAGE, ActivityPattern.VARIABLE);
        put(SpeciesType.WITCH, ranges(35, 55, 80, 95, 80, 95, 75, 90, 45, 70, 90, 100, 55, 70), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.CURIOUS, Personality.AGGRESSIVE, Personality.INDEPENDENT), true, 2, 45, 6, BehaviorFamily.SOCIETY_HOSTILE, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.SWAMP, ActivityPattern.VARIABLE);
        put(SpeciesType.ELDER_GUARDIAN, ranges(100, 100, 85, 100, 90, 100, 90, 100, 90, 100, 95, 100, 100, 100), List.of(Personality.NORMAL, Personality.PROTECTIVE, Personality.STUBBORN, Personality.AGGRESSIVE), false, 1, 36, 8, BehaviorFamily.GUARDIAN, MovementDomain.WATER, TerritoryStyle.STRUCTURE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.DEEP_WATER, ActivityPattern.VARIABLE);
        put(SpeciesType.GUARDIAN, ranges(95, 100, 65, 85, 85, 100, 85, 100, 80, 95, 85, 100, 75, 90), List.of(Personality.NORMAL, Personality.PROTECTIVE, Personality.CAUTIOUS, Personality.STUBBORN), true, 1, 36, 8, BehaviorFamily.GUARDIAN, MovementDomain.WATER, TerritoryStyle.STRUCTURE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.DEEP_WATER, ActivityPattern.VARIABLE);
        put(SpeciesType.IRON_GOLEM, ranges(90, 100, 75, 95, 75, 90, 70, 90, 90, 100, 85, 100, 100, 100), List.of(Personality.NORMAL, Personality.PROTECTIVE, Personality.CAUTIOUS, Personality.STUBBORN), false, 2, 48, 9, BehaviorFamily.GUARDIAN, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.VILLAGE, ActivityPattern.VARIABLE);
        put(SpeciesType.SNOW_GOLEM, ranges(70, 90, 35, 55, 60, 75, 60, 80, 60, 80, 55, 70, 20, 35), List.of(Personality.NORMAL, Personality.PROTECTIVE, Personality.CAUTIOUS), false, 2, 48, 8, BehaviorFamily.GUARDIAN, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.COLD, ActivityPattern.VARIABLE);
        put(SpeciesType.TRADER_LLAMA, ranges(20, 40, 65, 85, 65, 80, 70, 90, 85, 100, 75, 90, 55, 70), List.of(Personality.NORMAL, Personality.PROTECTIVE, Personality.SOCIABLE, Personality.CAUTIOUS), false, 999, 16, 0, BehaviorFamily.PASSIVE_HERD, MovementDomain.LAND, TerritoryStyle.NONE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.VILLAGE, ActivityPattern.DIURNAL);
        put(SpeciesType.ENDER_DRAGON, ranges(100, 100, 90, 100, 95, 100, 95, 100, 0, 10, 100, 100, 100, 100), List.of(Personality.NORMAL), false, 999, 22, 7, BehaviorFamily.BOSS, MovementDomain.AIR, TerritoryStyle.MOBILE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.END, ActivityPattern.VARIABLE);
        put(SpeciesType.WITHER, ranges(10, 30, 85, 100, 95, 100, 95, 100, 0, 10, 95, 100, 100, 100), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN), false, 999, 22, 3, BehaviorFamily.BOSS, MovementDomain.AIR, TerritoryStyle.MOBILE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.GENERAL, ActivityPattern.VARIABLE);
        put(SpeciesType.GIANT, ranges(10, 30, 15, 35, 55, 70, 55, 75, 70, 90, 45, 60, 100, 100), List.of(Personality.NORMAL, Personality.AGGRESSIVE, Personality.STUBBORN), true, 4, 44, 5, BehaviorFamily.UNDEAD_HORDE, MovementDomain.LAND, TerritoryStyle.HORDE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.GENERAL, ActivityPattern.NOCTURNAL);
        put(SpeciesType.ILLUSIONER, ranges(45, 65, 90, 100, 90, 100, 85, 100, 75, 95, 95, 100, 50, 65), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.CURIOUS, Personality.AGGRESSIVE), true, 2, 48, 7, BehaviorFamily.SOCIETY_HOSTILE, MovementDomain.LAND, TerritoryStyle.COMMUNITY, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.STRUCTURE, ActivityPattern.VARIABLE);
        put(SpeciesType.SKELETON_HORSE, ranges(20, 40, 55, 75, 65, 80, 55, 75, 55, 75, 65, 80, 60, 80), List.of(Personality.NORMAL, Personality.CAUTIOUS, Personality.BOLD, Personality.SOCIABLE), false, 2, 33, 3, BehaviorFamily.SPECIAL_MOUNT, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.GENERAL, ActivityPattern.VARIABLE);
        put(SpeciesType.ZOMBIE_HORSE, ranges(20, 40, 40, 60, 55, 70, 50, 70, 45, 65, 50, 65, 70, 85), List.of(Personality.NORMAL, Personality.STUBBORN, Personality.AGGRESSIVE, Personality.SOCIABLE), true, 2, 31, 3, BehaviorFamily.SPECIAL_MOUNT, MovementDomain.LAND, TerritoryStyle.HOME_RANGE, FootprintType.NONE, ReproductionMode.NONE, HabitatClass.GENERAL, ActivityPattern.VARIABLE);
        if (PROFILES.size() != SpeciesType.values().length) {
            throw new IllegalStateException("Living Ecology: perfil faltando para alguma espécie: "
                    + PROFILES.size() + "/" + SpeciesType.values().length);
        }
    }

    private final SpeciesType species;
    private final EnumMap<AttributeType, IntRange> attributes;
    private final List<Personality> personalities;
    private final boolean bossEligible;
    private final int territoryFormationMinimum;
    private final int territorySearchRadius;
    private final int maxTerritoryRadius;
    private final BehaviorFamily behaviorFamily;
    private final MovementDomain movementDomain;
    private final TerritoryStyle territoryStyle;
    private final FootprintType footprintType;
    private final ReproductionMode reproductionMode;
    private final HabitatClass habitatClass;
    private final ActivityPattern activityPattern;

    private SpeciesProfile(SpeciesType species,
                           Map<AttributeType, IntRange> attributes,
                           List<Personality> personalities,
                           boolean bossEligible,
                           int territoryFormationMinimum,
                           int territorySearchRadius,
                           int maxTerritoryRadius,
                           BehaviorFamily behaviorFamily,
                           MovementDomain movementDomain,
                           TerritoryStyle territoryStyle,
                           FootprintType footprintType,
                           ReproductionMode reproductionMode,
                           HabitatClass habitatClass,
                           ActivityPattern activityPattern) {
        this.species = species;
        this.attributes = new EnumMap<>(attributes);
        this.personalities = List.copyOf(personalities);
        this.bossEligible = bossEligible;
        this.territoryFormationMinimum = territoryFormationMinimum;
        this.territorySearchRadius = territorySearchRadius;
        this.maxTerritoryRadius = maxTerritoryRadius;
        this.behaviorFamily = behaviorFamily;
        this.movementDomain = movementDomain;
        this.territoryStyle = territoryStyle;
        this.footprintType = footprintType;
        this.reproductionMode = reproductionMode;
        this.habitatClass = habitatClass;
        this.activityPattern = activityPattern;
    }

    public static SpeciesProfile of(SpeciesType species) {
        SpeciesProfile profile = PROFILES.get(species);
        if (profile == null) throw new IllegalArgumentException("Sem perfil para " + species);
        return profile;
    }

    public static Map<SpeciesType, SpeciesProfile> all() { return Map.copyOf(PROFILES); }

    public SpeciesType species() { return species; }
    public IntRange range(AttributeType type) { return attributes.get(type); }
    public int midpoint(AttributeType type) { return range(type).midpoint(); }
    public List<Personality> personalities() { return personalities; }
    public boolean bossEligible() { return bossEligible; }
    public int territoryFormationMinimum() { return territoryFormationMinimum; }
    public int territorySearchRadius() { return territorySearchRadius; }
    public int maxTerritoryRadius() { return maxTerritoryRadius; }
    public BehaviorFamily behaviorFamily() { return behaviorFamily; }
    public MovementDomain movementDomain() { return movementDomain; }
    public TerritoryStyle territoryStyle() { return territoryStyle; }
    public FootprintType footprintType() { return footprintType; }
    public ReproductionMode reproductionMode() { return reproductionMode; }
    public HabitatClass habitatClass() { return habitatClass; }
    public ActivityPattern activityPattern() { return activityPattern; }

    public boolean formsPersistentTerritory(boolean boss) {
        if (territoryStyle == TerritoryStyle.NONE) return false;
        if (territoryStyle == TerritoryStyle.MOBILE) return boss;
        return true;
    }

    private static void put(SpeciesType species,
                            EnumMap<AttributeType, IntRange> attributes,
                            List<Personality> personalities,
                            boolean bossEligible,
                            int territoryFormationMinimum,
                            int territorySearchRadius,
                            int maxTerritoryRadius,
                            BehaviorFamily behaviorFamily,
                            MovementDomain movementDomain,
                            TerritoryStyle territoryStyle,
                            FootprintType footprintType,
                            ReproductionMode reproductionMode,
                            HabitatClass habitatClass,
                            ActivityPattern activityPattern) {
        PROFILES.put(species, new SpeciesProfile(species, attributes, personalities, bossEligible,
                territoryFormationMinimum, territorySearchRadius, maxTerritoryRadius,
                behaviorFamily, movementDomain, territoryStyle, footprintType, reproductionMode, habitatClass, activityPattern));
    }

    private static EnumMap<AttributeType, IntRange> ranges(
            int t0, int t1, int m0, int m1, int p0, int p1, int a0, int a1,
            int s0, int s1, int i0, int i1, int c0, int c1) {
        EnumMap<AttributeType, IntRange> map = new EnumMap<>(AttributeType.class);
        map.put(AttributeType.TERRITORY, new IntRange(t0, t1));
        map.put(AttributeType.MEMORY, new IntRange(m0, m1));
        map.put(AttributeType.PERCEPTION, new IntRange(p0, p1));
        map.put(AttributeType.ALERT, new IntRange(a0, a1));
        map.put(AttributeType.SOCIABILITY, new IntRange(s0, s1));
        map.put(AttributeType.INSTINCT, new IntRange(i0, i1));
        map.put(AttributeType.CONSTITUTION, new IntRange(c0, c1));
        return map;
    }
}
