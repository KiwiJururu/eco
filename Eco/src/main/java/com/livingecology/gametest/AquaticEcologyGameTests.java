package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.AquaticBehavior;
import com.livingecology.ai.SocialAlarmBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import com.livingecology.environment.ReproductionManager;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Objective integration coverage for issue #7's aquatic/amphibious behavior. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class AquaticEcologyGameTests {
    private AquaticEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void salmonSharesOnlyPerceivedLocalSchoolAlarm(GameTestHelper helper) {
        fillPool(helper);
        ServerLevel level = helper.getLevel();
        Salmon observer = spawn(helper, EntityType.SALMON, 4, 2, 4);
        Salmon receiver = spawn(helper, EntityType.SALMON, 5, 2, 4);
        Zombie threat = spawn(helper, EntityType.ZOMBIE, 8, 2, 4);
        MobMindData.initialize(observer, level);
        MobMindData.initialize(receiver, level);
        MobMindData.initialize(threat, level);

        MobMindData.rememberThreat(observer, threat, 60, level);
        helper.assertTrue(MobMindData.resolveThreat(receiver, level).isEmpty(),
                "Receiver knew threat before a local school signal");

        SocialAlarmBehavior.tick(receiver, level, SpeciesProfile.of(SpeciesType.SALMON));

        helper.assertTrue(MobMindData.resolveThreat(receiver, level)
                        .filter(entity -> entity.getUUID().equals(threat.getUUID())).isPresent(),
                "Nearby visible Salmon did not relay its perceived threat");
        helper.assertTrue(MobMindData.getState(receiver, StateType.FEAR) >= 1,
                "Threat relay did not produce a local fear response");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void puffedPufferfishRetainsVanillaDefenseOwnership(GameTestHelper helper) {
        fillPool(helper);
        ServerLevel level = helper.getLevel();
        Pufferfish puffer = spawn(helper, EntityType.PUFFERFISH, 4, 2, 4);
        Zombie threat = spawn(helper, EntityType.ZOMBIE, 7, 2, 4);
        MobMindData.initialize(puffer, level);
        MobMindData.initialize(threat, level);
        puffer.setPuffState(2);
        puffer.getNavigation().stop();
        MobMindData.rememberThreat(puffer, threat, 60, level);
        MobMindData.setState(puffer, StateType.FEAR, 4);

        AquaticBehavior.tick(puffer, level);

        helper.assertTrue(puffer.getPuffState() == 2,
                "Living Ecology replaced Pufferfish vanilla inflation state");
        helper.assertTrue(puffer.getNavigation().isDone(),
                "Living Ecology stole navigation while Pufferfish defense owned the response");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void playingDeadAxolotlKeepsBrainOwnedState(GameTestHelper helper) {
        fillPool(helper);
        ServerLevel level = helper.getLevel();
        Axolotl axolotl = spawn(helper, EntityType.AXOLOTL, 4, 2, 4);
        Zombie threat = spawn(helper, EntityType.ZOMBIE, 7, 2, 4);
        MobMindData.initialize(axolotl, level);
        MobMindData.initialize(threat, level);
        axolotl.setPlayingDead(true);
        axolotl.getNavigation().stop();
        MobMindData.rememberThreat(axolotl, threat, 60, level);
        MobMindData.setState(axolotl, StateType.FEAR, 4);

        AquaticBehavior.tick(axolotl, level);

        helper.assertTrue(axolotl.isPlayingDead(),
                "Living Ecology cleared Axolotl's vanilla play-dead state");
        helper.assertTrue(axolotl.getNavigation().isDone(),
                "Living Ecology issued movement while Axolotl's Brain owned play-dead");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void idleSquidUsesVectorCohesionWithoutPathNavigation(GameTestHelper helper) {
        fillPool(helper);
        ServerLevel level = helper.getLevel();
        Squid first = spawn(helper, EntityType.SQUID, 2, 2, 4);
        Squid second = spawn(helper, EntityType.SQUID, 10, 2, 4);
        MobMindData.initialize(first, level);
        MobMindData.initialize(second, level);
        MobMindData.get(first).putByte("attr_sociability", (byte) 80);
        // Entity fluid flags are populated by the first world tick. Reset the vanilla random vector
        // after that tick so this specifically exercises Living Ecology's idle cohesion overlay.
        helper.runAfterDelay(1, () -> {
            first.setMovementVector(0.0F, 0.0F, 0.0F);
            second.setMovementVector(0.0F, 0.0F, 0.0F);

            AquaticBehavior.tick(first, level);

            helper.assertTrue(first.hasMovementVector(),
                    "Idle Squid did not receive bounded same-species cohesion vector");
            helper.assertTrue(first.getNavigation().isDone(),
                    "Squid cohesion incorrectly used path navigation instead of its vanilla vector controller");
            helper.succeed();
        });
    }

    @GameTest(template = "test_arena")
    public static void aquaticTerritoryExistsOnlyForProfiledHomeRange(GameTestHelper helper) {
        fillPool(helper);
        ServerLevel level = helper.getLevel();
        Axolotl first = spawn(helper, EntityType.AXOLOTL, 4, 2, 4);
        Axolotl second = spawn(helper, EntityType.AXOLOTL, 5, 2, 4);
        Cod cod = spawn(helper, EntityType.COD, 7, 2, 4);
        MobMindData.initialize(first, level);
        MobMindData.initialize(second, level);
        MobMindData.initialize(cod, level);

        TerritoryRecord axolotlTerritory = TerritoryManager.ensureTerritory(first, level);
        TerritoryRecord codTerritory = TerritoryManager.ensureTerritory(cod, level);

        helper.assertTrue(axolotlTerritory != null,
                "Axolotl pair failed to establish its profiled home range");
        helper.assertTrue(codTerritory == null && MobMindData.territoryId(cod) == 0L,
                "Non-territorial Cod received persistent territory");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void axolotlReproductionUsesVanillaLoveAndEcologicalCapacity(GameTestHelper helper) {
        fillPool(helper);
        ServerLevel level = helper.getLevel();
        Axolotl first = spawn(helper, EntityType.AXOLOTL, 4, 2, 5);
        Axolotl second = spawn(helper, EntityType.AXOLOTL, 5, 2, 5);
        for (Axolotl axolotl : new Axolotl[]{first, second}) {
            MobMindData.initialize(axolotl, level);
            MobMindData.get(axolotl).putLong("nextReproduction", 0L);
        }

        TerritoryRecord territory = TerritoryManager.ensureTerritory(first, level);
        helper.assertTrue(territory != null, "Axolotl home range missing before reproduction");
        AABB pool = new AABB(helper.absolutePos(new BlockPos(1, 1, 1)),
                helper.absolutePos(new BlockPos(11, 3, 11)));

        int pairs = ReproductionManager.triggerEligiblePairs(level, pool);

        helper.assertTrue(pairs == 1, "Expected one eligible Axolotl pair, got " + pairs);
        helper.assertTrue(first.isInLove() && second.isInLove(),
                "Ecological reproduction bypassed Axolotl's vanilla love state");
        helper.succeed();
    }

    private static void fillPool(GameTestHelper helper) {
        for (int x = 1; x <= 11; x++) {
            for (int z = 1; z <= 11; z++) {
                for (int y = 1; y <= 3; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                }
            }
        }
    }

    private static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type,
                                            int x, int y, int z) {
        T mob = type.create(helper.getLevel());
        if (mob == null) throw new IllegalStateException(
                "Could not create " + BuiltInRegistries.ENTITY_TYPE.getKey(type));
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        mob.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }
}
