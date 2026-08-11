package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.BehaviorUtil;
import com.livingecology.ai.SpecialHostileNetherBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.MovementDomain;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.EnvironmentSavedData;
import com.livingecology.environment.ReproductionManager;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Objective integration coverage for issue #12's special hostile and Nether batch. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class SpecialHostileNetherEcologyGameTests {
    private SpecialHostileNetherEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void creeperFuseTargetAndNavigationRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Creeper creeper = spawn(helper, EntityType.CREEPER, 4, 1, 4);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 8, 1, 4);
        MobMindData.initialize(creeper, level);
        MobMindData.initialize(target, level);
        creeper.setTarget(target);
        creeper.setSwellDir(1);
        creeper.ignite();
        creeper.getNavigation().stop();

        SpecialHostileNetherBehavior.tick(creeper, level);

        helper.assertTrue(creeper.isIgnited() && creeper.getSwellDir() == 1,
                "Living Ecology changed Creeper fuse/swell state");
        helper.assertTrue(creeper.getTarget() == target && creeper.getNavigation().isDone(),
                "Living Ecology replaced Creeper target or fuse approach navigation");
        helper.assertTrue(MobMindData.resolveThreat(creeper, level)
                        .filter(entity -> entity.getUUID().equals(target.getUUID())).isPresent(),
                "Creeper did not record its legal perceived target");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void slimeAndMagmaCubeSizeSquishAndJumpMotionRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Slime slime = spawn(helper, EntityType.SLIME, 4, 1, 4);
        MagmaCube magma = spawn(helper, EntityType.MAGMA_CUBE, 7, 1, 4);
        MobMindData.initialize(slime, level);
        MobMindData.initialize(magma, level);
        slime.setSize(4, true);
        magma.setSize(3, true);
        slime.targetSquish = 0.8F;
        magma.targetSquish = -0.4F;
        Vec3 slimeMotion = new Vec3(0.18D, 0.42D, -0.09D);
        Vec3 magmaMotion = new Vec3(-0.12D, 0.36D, 0.15D);
        slime.setDeltaMovement(slimeMotion);
        magma.setDeltaMovement(magmaMotion);
        slime.getNavigation().stop();
        magma.getNavigation().stop();

        SpecialHostileNetherBehavior.tick(slime, level);
        SpecialHostileNetherBehavior.tick(magma, level);

        helper.assertTrue(slime.getSize() == 4 && magma.getSize() == 3,
                "Living Ecology changed Slime/Magma Cube size state");
        helper.assertTrue(slime.targetSquish == 0.8F && magma.targetSquish == -0.4F,
                "Living Ecology changed Slime/Magma Cube squish cycle");
        helper.assertTrue(slime.getDeltaMovement().equals(slimeMotion)
                        && magma.getDeltaMovement().equals(magmaMotion),
                "Living Ecology replaced Slime/Magma Cube jump movement");
        helper.assertTrue(slime.getNavigation().isDone() && magma.getNavigation().isDone(),
                "Living Ecology issued generic patrol navigation to a jump-controlled mob");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void specialFlyerChargeAnchorOwnerAndMotionRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Blaze blaze = spawn(helper, EntityType.BLAZE, 3, 4, 3);
        Ghast ghast = spawn(helper, EntityType.GHAST, 8, 5, 8);
        Phantom phantom = spawn(helper, EntityType.PHANTOM, 5, 5, 8);
        Vex vex = spawn(helper, EntityType.VEX, 5, 4, 4);
        for (Mob mob : new Mob[]{blaze, ghast, phantom, vex}) {
            MobMindData.initialize(mob, level);
            mob.getNavigation().stop();
        }
        ghast.setCharging(true);
        phantom.setPhantomSize(4);
        BlockPos bound = helper.absolutePos(new BlockPos(6, 3, 6));
        vex.setOwner(blaze);
        vex.setBoundOrigin(bound);
        vex.setIsCharging(true);
        vex.setLimitedLife(200);
        Vec3 blazeMotion = new Vec3(0.08D, 0.14D, 0.02D);
        Vec3 ghastMotion = new Vec3(-0.07D, 0.03D, 0.11D);
        Vec3 phantomMotion = new Vec3(0.12D, -0.09D, 0.06D);
        Vec3 vexMotion = new Vec3(-0.10D, 0.05D, -0.04D);
        blaze.setDeltaMovement(blazeMotion);
        ghast.setDeltaMovement(ghastMotion);
        phantom.setDeltaMovement(phantomMotion);
        vex.setDeltaMovement(vexMotion);

        SpecialHostileNetherBehavior.tick(blaze, level);
        SpecialHostileNetherBehavior.tick(ghast, level);
        SpecialHostileNetherBehavior.tick(phantom, level);
        SpecialHostileNetherBehavior.tick(vex, level);

        helper.assertTrue(ghast.isCharging() && phantom.getPhantomSize() == 4,
                "Living Ecology changed Ghast charge or Phantom size/flight state");
        helper.assertTrue(vex.isCharging() && vex.getOwner() == blaze
                        && bound.equals(vex.getBoundOrigin()),
                "Living Ecology changed Vex owner, bound origin or charge state");
        helper.assertTrue(blaze.getDeltaMovement().equals(blazeMotion)
                        && ghast.getDeltaMovement().equals(ghastMotion)
                        && phantom.getDeltaMovement().equals(phantomMotion)
                        && vex.getDeltaMovement().equals(vexMotion),
                "Living Ecology replaced a special flyer's native motion controller");
        helper.assertTrue(blaze.getNavigation().isDone() && ghast.getNavigation().isDone()
                        && phantom.getNavigation().isDone() && vex.getNavigation().isDone(),
                "Living Ecology issued generic patrol navigation to a special flyer");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void hoglinAndZoglinBrainLifecycleAndTerritoryRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Hoglin hoglin = spawn(helper, EntityType.HOGLIN, 3, 1, 4);
        Hoglin hoglinMate = spawn(helper, EntityType.HOGLIN, 5, 1, 4);
        Zoglin zoglin = spawn(helper, EntityType.ZOGLIN, 3, 1, 8);
        Zoglin zoglinMate = spawn(helper, EntityType.ZOGLIN, 5, 1, 8);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 9, 1, 4);
        for (Mob mob : new Mob[]{hoglin, hoglinMate, zoglin, zoglinMate, target}) {
            MobMindData.initialize(mob, level);
            mob.getNavigation().stop();
        }
        hoglin.setImmuneToZombification(true);
        hoglin.setTarget(target);
        zoglin.setBaby(true);
        CompoundTag before = new CompoundTag();
        hoglin.addAdditionalSaveData(before);

        SpecialHostileNetherBehavior.tick(hoglin, level);
        SpecialHostileNetherBehavior.tick(zoglin, level);
        CompoundTag after = new CompoundTag();
        hoglin.addAdditionalSaveData(after);
        TerritoryRecord hoglinTerritory = TerritoryManager.ensureTerritory(hoglin, level);
        TerritoryRecord zoglinTerritory = TerritoryManager.ensureTerritory(zoglin, level);

        helper.assertTrue(hoglin.getTarget() == target
                        && before.getBoolean("IsImmuneToZombification")
                        == after.getBoolean("IsImmuneToZombification"),
                "Living Ecology changed Hoglin Brain target or zombification lifecycle");
        helper.assertTrue(zoglin.isBaby() && zoglin.getTarget() == null,
                "Living Ecology changed Zoglin baby state or invented a target");
        helper.assertTrue(hoglinTerritory != null && zoglinTerritory != null,
                "Profiled Hoglin/Zoglin groups failed to establish persistent ecology");
        helper.assertTrue(hoglin.getNavigation().isDone() && zoglin.getNavigation().isDone(),
                "Living Ecology replaced Hoglin/Zoglin Brain movement");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void riddenColdStriderKeepsSaddleRiderBoostAndMovementOwnership(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Strider strider = spawn(helper, EntityType.STRIDER, 4, 1, 4);
        spawn(helper, EntityType.STRIDER, 6, 1, 4);
        MobMindData.initialize(strider, level);
        strider.equipSaddle(null);
        strider.setSuffocating(true);
        helper.makeMockPlayer().startRiding(strider, true);
        boolean boosted = strider.boost();
        Vec3 motion = new Vec3(0.14D, 0.0D, -0.06D);
        strider.setDeltaMovement(motion);
        strider.getNavigation().stop();

        SpecialHostileNetherBehavior.tick(strider, level);

        helper.assertTrue(strider.isSaddled() && strider.isVehicle() && boosted,
                "Living Ecology changed Strider saddle, rider or boost state");
        helper.assertTrue(strider.isSuffocating() && strider.getDeltaMovement().equals(motion),
                "Living Ecology changed Strider cold/lava movement state");
        helper.assertTrue(strider.getNavigation().isDone(),
                "Living Ecology issued generic herd navigation to a ridden Strider");
        helper.assertTrue(!ReproductionManager.isEligibleBase(strider),
                "Ridden Strider remained eligible for ecological reproduction");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void creativeTargetAndMemoryAreSanitizedWithoutFlightTakeover(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Blaze blaze = spawn(helper, EntityType.BLAZE, 4, 3, 4);
        MobMindData.initialize(blaze, level);
        ServerPlayer creative = FakePlayerFactory.getMinecraft(level);
        creative.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        creative.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 2, 4))));
        blaze.setTarget(creative);
        MobMindData.rememberThreat(blaze, creative, 60, level);
        Vec3 motion = new Vec3(0.05D, 0.12D, -0.03D);
        blaze.setDeltaMovement(motion);

        SpecialHostileNetherBehavior.tick(blaze, level);

        helper.assertTrue(blaze.getTarget() == null && MobMindData.resolveThreat(blaze, level).isEmpty(),
                "Special hostile retained a Creative player target or memory");
        helper.assertTrue(blaze.getDeltaMovement().equals(motion),
                "Creative sanitization replaced Blaze flight movement");
        creative.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void landAirAndLavaDomainGatesUseActualWorldBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos lava = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos air = helper.absolutePos(new BlockPos(6, 3, 6));
        BlockPos land = helper.absolutePos(new BlockPos(8, 1, 8));
        helper.setBlock(new BlockPos(4, 1, 4), Blocks.LAVA);

        helper.assertTrue(BehaviorUtil.isValidForDomain(level, land, MovementDomain.LAND),
                "Known floor position failed LAND domain validation");
        helper.assertTrue(BehaviorUtil.isValidForDomain(level, air, MovementDomain.AIR),
                "Open template volume failed AIR domain validation");
        helper.assertTrue(BehaviorUtil.isValidForDomain(level, lava, MovementDomain.LAVA),
                "Lava block failed LAVA domain validation");
        helper.assertTrue(!BehaviorUtil.isValidForDomain(level, lava, MovementDomain.LAND)
                        && !BehaviorUtil.isValidForDomain(level, air, MovementDomain.LAVA),
                "Movement domains accepted an incompatible environmental target");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void hoglinAndStriderReproductionUsesVanillaLoveAndRepellentGate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.LAVA);
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.LAVA);
        Strider firstStrider = spawn(helper, EntityType.STRIDER, 3, 1, 3);
        Strider secondStrider = spawn(helper, EntityType.STRIDER, 5, 1, 3);
        Hoglin firstHoglin = spawn(helper, EntityType.HOGLIN, 3, 1, 8);
        Hoglin secondHoglin = spawn(helper, EntityType.HOGLIN, 5, 1, 8);
        Hoglin pacified = spawn(helper, EntityType.HOGLIN, 8, 1, 8);
        firstHoglin.setImmuneToZombification(true);
        secondHoglin.setImmuneToZombification(true);
        pacified.setImmuneToZombification(true);
        // HoglinAi derives PACIFIED from the nearest-repellent sensor; seed the resulting Brain
        // contract directly because this synchronous fixture does not run a sensor tick first.
        pacified.getBrain().setMemory(MemoryModuleType.PACIFIED, true);
        for (Mob mob : new Mob[]{firstStrider, secondStrider, firstHoglin, secondHoglin, pacified}) {
            MobMindData.initialize(mob, level);
            MobMindData.get(mob).putLong("nextReproduction", 0L);
        }
        TerritoryRecord striderTerritory = TerritoryManager.ensureTerritory(firstStrider, level);
        TerritoryRecord hoglinTerritory = TerritoryManager.ensureTerritory(firstHoglin, level);
        helper.assertTrue(striderTerritory != null && hoglinTerritory != null,
                "Hoglin/Strider reproduction territories were not established");
        striderTerritory.setPopulation(2);
        hoglinTerritory.setPopulation(2);
        EnvironmentSavedData.get(level)
                .getOrCreate(EnvironmentManager.regionKey(striderTerritory.center()), level.getGameTime())
                .disturb(50, 50, 50);
        EnvironmentSavedData.get(level)
                .getOrCreate(EnvironmentManager.regionKey(hoglinTerritory.center()), level.getGameTime())
                .disturb(50, 50, 50);
        AABB bounds = new AABB(helper.absolutePos(new BlockPos(1, 0, 1)),
                helper.absolutePos(new BlockPos(11, 5, 11)));

        int pairs = ReproductionManager.triggerEligiblePairs(level, bounds);

        helper.assertTrue(pairs == 2, "Expected one Hoglin and one Strider pair, got " + pairs);
        helper.assertTrue(firstStrider.isInLove() && secondStrider.isInLove()
                        && firstHoglin.isInLove() && secondHoglin.isInLove(),
                "Ecological reproduction bypassed vanilla Hoglin/Strider love state");
        helper.assertTrue(!pacified.canFallInLove() && !pacified.isInLove()
                        && !ReproductionManager.isEligibleBase(pacified),
                "Repellent-pacified Hoglin bypassed its vanilla canFallInLove restriction");
        helper.succeed();
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
