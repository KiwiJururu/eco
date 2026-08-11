package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.GenericBehavior;
import com.livingecology.ai.WolfBehavior;
import com.livingecology.ai.ZombieBehavior;
import com.livingecology.data.AttributeType;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.EnvironmentSavedData;
import com.livingecology.environment.ReproductionManager;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.EnumSet;

/**
 * Required integration tests for the Living Ecology core. These are deliberately focused on
 * objective invariants; visual/naturalness tuning remains a human QA task.
 */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class LivingEcologyGameTests {
    private LivingEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void catalogueCreatesAll79Mobs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int created = 0;
        for (SpeciesType species : SpeciesType.values()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("minecraft", species.minecraftId()));
            Entity entity = type.create(level);
            helper.assertTrue(entity instanceof Mob, "Could not create supported mob: " + species.name());
            Mob mob = (Mob) entity;
            MobMindData.initialize(mob, level);
            helper.assertTrue(MobMindData.supports(mob), "Mob not recognized after creation: " + species.name());
            for (AttributeType attribute : AttributeType.values()) {
                int value = MobMindData.getAttribute(mob, attribute);
                helper.assertTrue(value >= 0 && value <= 100,
                        species.name() + " produced out-of-range " + attribute + "=" + value);
            }
            int adaptation = MobMindData.calculateAdaptation(mob, level);
            helper.assertTrue(adaptation >= 0 && adaptation <= 100,
                    species.name() + " adaptation outside 0..100: " + adaptation);
            mob.discard();
            created++;
        }
        helper.assertTrue(created == 79, "Expected 79 supported creatures, created " + created);
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void creativePlayerNeverRemainsAHostileTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Zombie zombie = spawn(helper, EntityType.ZOMBIE, 4, 1, 4);
        MobMindData.initialize(zombie, level);

        ServerPlayer creative = FakePlayerFactory.getMinecraft(level);
        creative.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        creative.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(6, 1, 6))));

        zombie.setTarget(creative);
        MobMindData.rememberThreat(zombie, creative, 30, level);
        MobMindData.clearInvalidPlayerThreat(zombie, level);

        helper.assertTrue(zombie.getTarget() == null, "Zombie retained Creative player as target");
        helper.assertTrue(MobMindData.resolveThreat(zombie, level).isEmpty(),
                "Zombie retained Creative player in threat memory");
        creative.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void zombieHordePrioritizesWolfWar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Wolf wolf = spawn(helper, EntityType.WOLF, 7, 1, 6);
        Zombie a = spawn(helper, EntityType.ZOMBIE, 3, 1, 5);
        Zombie b = spawn(helper, EntityType.ZOMBIE, 3, 1, 6);
        Zombie c = spawn(helper, EntityType.ZOMBIE, 3, 1, 7);
        MobMindData.initialize(wolf, level);
        MobMindData.initialize(a, level);
        MobMindData.initialize(b, level);
        MobMindData.initialize(c, level);

        ZombieBehavior.tick(a, level);
        ZombieBehavior.tick(b, level);
        ZombieBehavior.tick(c, level);

        int targetingWolf = 0;
        for (Zombie zombie : new Zombie[]{a, b, c}) if (zombie.getTarget() == wolf) targetingWolf++;
        helper.assertTrue(targetingWolf >= 2,
                "Horde did not propagate Wolf target; targetingWolf=" + targetingWolf);
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void wolfPatrolCreatesNavigationOrder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Wolf wolf = spawn(helper, EntityType.WOLF, 4, 1, 4);
        MobMindData.initialize(wolf, level);

        BlockPos center = helper.absolutePos(new BlockPos(4, 1, 4));
        TerritoryRecord territory = new TerritoryRecord(900001L, SpeciesType.WOLF, center, center,
                2, 70, 50, 1, level.getSeed() ^ 0x574f4c46L, level.getGameTime());

        boolean ordered = WolfBehavior.orderPatrol(wolf, level, territory);
        helper.assertTrue(ordered, "Wolf patrol planner could not issue a reachable navigation order");
        helper.assertTrue(!wolf.getNavigation().isDone(),
                "Wolf patrol planner reported success but navigation remained idle");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void spiderZombieSymbiosisPreventsFriendlyTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Spider spider = spawn(helper, EntityType.SPIDER, 4, 1, 4);
        Zombie zombie = spawn(helper, EntityType.ZOMBIE, 6, 1, 4);
        MobMindData.initialize(spider, level);
        MobMindData.initialize(zombie, level);

        zombie.setTarget(spider);
        var relation = RelationService.natural(SpeciesType.ZOMBIE, SpeciesType.SPIDER);
        helper.assertTrue(relation.kind() == RelationKind.SYMBIOTIC && relation.affinity() >= 65,
                "Spider/Zombie natural relation lost symbiotic classification");
        helper.assertTrue(zombie.getTarget() == null, "Zombie retained symbiotic Spider as combat target");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void cowReproductionUsesCapacityAndTerritory(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Cow a = spawn(helper, EntityType.COW, 4, 1, 5);
        Cow b = spawn(helper, EntityType.COW, 5, 1, 5);
        Cow c = spawn(helper, EntityType.COW, 6, 1, 5);
        for (Cow cow : new Cow[]{a, b, c}) {
            MobMindData.initialize(cow, level);
            MobMindData.get(cow).putLong("nextReproduction", 0L);
        }

        TerritoryRecord territory = TerritoryManager.ensureTerritory(a, level);
        helper.assertTrue(territory != null, "Cow herd failed to form a territory before reproduction");
        EnvironmentSavedData.get(level).getOrCreate(EnvironmentManager.regionKey(territory.center()), level.getGameTime())
                .disturb(35, 35, 35);
        territory.setPopulation(3);

        AABB area = new AABB(helper.absolutePos(new BlockPos(1, 0, 1)), helper.absolutePos(new BlockPos(11, 4, 11)));
        int pairs = ReproductionManager.triggerEligiblePairs(level, area);
        helper.assertTrue(pairs >= 1, "Ecological reproduction did not start an eligible Cow pair");
        helper.assertTrue(a.isInLove() || b.isInLove() || c.isInLove(), "No Cow entered vanilla love mode");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void birthInheritsTerritoryAndCanGrowPopulation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Cow a = spawn(helper, EntityType.COW, 4, 1, 4);
        Cow b = spawn(helper, EntityType.COW, 5, 1, 4);
        MobMindData.initialize(a, level);
        MobMindData.initialize(b, level);
        TerritoryRecord territory = TerritoryManager.ensureTerritory(a, level);
        helper.assertTrue(territory != null, "Cow territory missing for birth test");

        Cow child = EntityType.COW.create(level);
        helper.assertTrue(child != null, "Could not create Cow child");
        MobMindData.initialize(child, level);
        int before = territory.population();
        TerritoryManager.recordBirth(a, child, level);

        helper.assertTrue(MobMindData.territoryId(child) == territory.id(), "Child did not inherit parent territory");
        helper.assertTrue(territory.population() == before + 1, "Birth did not increment territory population");
        helper.assertTrue(territory.radiusChunks() <= SpeciesProfile.of(SpeciesType.COW).maxTerritoryRadius(),
                "Birth expanded territory beyond species cap");
        child.discard();
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void everyBehaviorFamilyCanTickRepresentative(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        EnumSet<com.livingecology.data.BehaviorFamily> seen = EnumSet.noneOf(com.livingecology.data.BehaviorFamily.class);
        int x = 2;
        int z = 2;
        for (SpeciesType species : SpeciesType.values()) {
            SpeciesProfile profile = SpeciesProfile.of(species);
            if (seen.contains(profile.behaviorFamily())) continue;
            seen.add(profile.behaviorFamily());

            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("minecraft", species.minecraftId()));
            Entity entity = type.create(level);
            helper.assertTrue(entity instanceof Mob, "Representative could not be created: " + species);
            Mob mob = (Mob) entity;
            BlockPos pos = helper.absolutePos(new BlockPos(x, 1, z));
            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            level.addFreshEntity(mob);
            MobMindData.initialize(mob, level);
            GenericBehavior.tick(mob, level, profile);
            helper.assertTrue(mob.isAlive(), "Representative died/invalidated while ticking: " + species);
            mob.discard();
            x += 2;
            if (x >= 11) { x = 2; z += 2; }
        }
        helper.assertTrue(seen.size() == com.livingecology.data.BehaviorFamily.values().length,
                "Not all behavior families received a representative tick: " + seen.size());
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void footprintProfilesProduceBoundedTargets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SpeciesType[] species = {SpeciesType.SPIDER, SpeciesType.CAVE_SPIDER, SpeciesType.BEE,
                SpeciesType.WOLF, SpeciesType.RABBIT, SpeciesType.MOOSHROOM};
        for (SpeciesType type : species) {
            SpeciesProfile profile = SpeciesProfile.of(type);
            TerritoryRecord record = new TerritoryRecord(1000L + type.ordinal(), type, helper.absolutePos(new BlockPos(6, 1, 6)),
                    helper.absolutePos(new BlockPos(6, 1, 6)), Math.max(2, profile.maxTerritoryRadius()),
                    90, 90, 12, level.getSeed() ^ type.ordinal(), level.getGameTime());
            int desired = record.desiredFootprint();
            helper.assertTrue(desired > 0 && desired <= 96,
                    type + " footprint target invalid: " + desired);
        }
        helper.succeed();
    }

    private static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type, int x, int y, int z) {
        T mob = type.create(helper.getLevel());
        if (mob == null) throw new IllegalStateException("Could not create " + BuiltInRegistries.ENTITY_TYPE.getKey(type));
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }
}
