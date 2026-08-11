package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.IndependentLandBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import com.livingecology.territory.TerritorySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Objective integration coverage for issue #10's predators and independent animals. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class IndependentLandEcologyGameTests {
    private IndependentLandEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void foxTrustSleepItemAndTargetOwnershipRemainVanilla(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Fox fox = spawn(helper, EntityType.FOX, 4, 2, 4);
        UUID trusted = UUID.randomUUID();
        ItemStack held = new ItemStack(Items.SWEET_BERRIES);
        fox.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, held.copy());
        CompoundTag setup = new CompoundTag();
        fox.addAdditionalSaveData(setup);
        ListTag trustedList = new ListTag();
        trustedList.add(NbtUtils.createUUID(trusted));
        setup.put("Trusted", trustedList);
        setup.putBoolean("Sleeping", true);
        fox.readAdditionalSaveData(setup);
        fox.getNavigation().stop();
        MobMindData.initialize(fox, level);

        IndependentLandBehavior.tick(fox, level);

        CompoundTag saved = new CompoundTag();
        fox.addAdditionalSaveData(saved);
        ListTag savedTrusted = saved.getList("Trusted", 11);
        helper.assertTrue(fox.isSleeping(), "Living Ecology woke a sleeping Fox");
        helper.assertTrue(savedTrusted.size() == 1
                        && trusted.equals(NbtUtils.loadUUID(savedTrusted.get(0))),
                "Living Ecology changed Fox trusted-player memory");
        helper.assertTrue(ItemStack.isSameItemSameTags(held, fox.getMainHandItem()),
                "Living Ecology changed Fox held-item state");
        helper.assertTrue(fox.getNavigation().isDone() && fox.getTarget() == null,
                "Living Ecology replaced sleeping Fox movement/target ownership");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void trustingOcelotRetreatsWithoutInventingAggression(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Ocelot ocelot = spawn(helper, EntityType.OCELOT, 5, 1, 5);
        Zombie threat = spawn(helper, EntityType.ZOMBIE, 8, 1, 5);
        CompoundTag setup = new CompoundTag();
        ocelot.addAdditionalSaveData(setup);
        setup.putBoolean("Trusting", true);
        ocelot.readAdditionalSaveData(setup);
        ocelot.setHealth(ocelot.getMaxHealth() * 0.30F);
        // EntityType#create runs before the first physics tick; explicitly settle the actor on
        // the template's known floor so GroundPathNavigation can evaluate the retreat order.
        ocelot.setOnGround(true);
        ocelot.getNavigation().stop();
        MobMindData.initialize(ocelot, level);
        MobMindData.initialize(threat, level);
        MobMindData.rememberThreat(ocelot, threat, 60, level);
        MobMindData.addState(ocelot, StateType.FEAR, 2);

        IndependentLandBehavior.tick(ocelot, level);

        CompoundTag saved = new CompoundTag();
        ocelot.addAdditionalSaveData(saved);
        helper.assertTrue(saved.getBoolean("Trusting"),
                "Living Ecology changed Ocelot trust state");
        helper.assertTrue(ocelot.getTarget() == null,
                "Ocelot retreat invented a combat target outside vanilla repertoire");
        helper.assertTrue(!ocelot.getNavigation().isDone(),
                "Threatened low-health Ocelot did not receive a safe retreat path");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void tamedCatKeepsOwnerSitBedAndCollarState(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Cat cat = spawn(helper, EntityType.CAT, 4, 2, 4);
        Player owner = helper.makeMockPlayer();
        cat.tame(owner);
        cat.setOrderedToSit(true);
        cat.setLying(true);
        cat.setRelaxStateOne(true);
        cat.setCollarColor(DyeColor.BLUE);
        cat.getNavigation().stop();
        MobMindData.initialize(cat, level);

        IndependentLandBehavior.tick(cat, level);

        helper.assertTrue(cat.isTame() && owner.getUUID().equals(cat.getOwnerUUID()),
                "Living Ecology changed Cat tame/owner state");
        helper.assertTrue(cat.isOrderedToSit() && cat.isLying() && cat.isRelaxStateOne(),
                "Living Ecology changed Cat sit/bed relaxation state");
        helper.assertTrue(cat.getCollarColor() == DyeColor.BLUE,
                "Living Ecology changed Cat collar state");
        helper.assertTrue(cat.getNavigation().isDone() && MobMindData.territoryId(cat) == 0L,
                "Player-owned Cat received wild navigation/territory");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void polarBearCubProtectionAndNeutralAngerRemainVanilla(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PolarBear adult = spawn(helper, EntityType.POLAR_BEAR, 4, 2, 4);
        PolarBear cub = spawn(helper, EntityType.POLAR_BEAR, 6, 2, 4);
        Zombie intruder = spawn(helper, EntityType.ZOMBIE, 8, 2, 4);
        cub.setBaby(true);
        adult.setPersistentAngerTarget(intruder.getUUID());
        adult.setRemainingPersistentAngerTime(400);
        adult.setTarget(intruder);
        adult.setStanding(true);
        adult.getNavigation().stop();
        MobMindData.initialize(adult, level);
        MobMindData.initialize(cub, level);
        MobMindData.initialize(intruder, level);
        helper.assertTrue(adult.getTarget() == intruder,
                "GameTest failed to establish vanilla Polar Bear protection target");
        UUID angerTarget = adult.getPersistentAngerTarget();
        int angerTime = adult.getRemainingPersistentAngerTime();

        IndependentLandBehavior.tick(adult, level);

        helper.assertTrue(cub.isBaby(), "Polar Bear cub state was changed");
        helper.assertTrue(adult.getTarget() == intruder
                        && java.util.Objects.equals(angerTarget, adult.getPersistentAngerTarget())
                        && adult.getRemainingPersistentAngerTime() == angerTime,
                "Living Ecology replaced Polar Bear protective neutral anger");
        helper.assertTrue(adult.isStanding() && adult.getNavigation().isDone(),
                "Living Ecology replaced Polar Bear standing/protection movement");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void pandaGenesAndAtomicActionStatesRemainVanilla(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Panda panda = spawn(helper, EntityType.PANDA, 4, 2, 4);
        panda.setMainGene(Panda.Gene.WORRIED);
        panda.setHiddenGene(Panda.Gene.PLAYFUL);
        panda.setAttributes();
        panda.sit(true);
        panda.eat(true);
        panda.getNavigation().stop();
        MobMindData.initialize(panda, level);

        IndependentLandBehavior.tick(panda, level);

        helper.assertTrue(panda.getMainGene() == Panda.Gene.WORRIED
                        && panda.getHiddenGene() == Panda.Gene.PLAYFUL,
                "Living Ecology changed Panda genes");
        helper.assertTrue(panda.isSitting() && panda.isEating(),
                "Living Ecology interrupted Panda sitting/eating action");
        helper.assertTrue(panda.getNavigation().isDone() && panda.getTarget() == null,
                "Living Ecology issued movement/aggression during Panda atomic action");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void predatorOverlapRecordsAcquiredRivalryWithoutForcedTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Fox fox = spawn(helper, EntityType.FOX, 4, 2, 4);
        Ocelot ocelot = spawn(helper, EntityType.OCELOT, 7, 2, 4);
        MobMindData.initialize(fox, level);
        MobMindData.initialize(ocelot, level);
        TerritoryRecord foxTerritory = TerritoryManager.ensureTerritory(fox, level);
        TerritoryRecord ocelotTerritory = TerritoryManager.ensureTerritory(ocelot, level);
        helper.assertTrue(foxTerritory != null && ocelotTerritory != null
                        && foxTerritory.id() != ocelotTerritory.id(),
                "Independent predators failed to establish distinct overlapping territories");

        TerritorySavedData data = TerritorySavedData.get(level);
        int before = RelationService.effectiveRivalry(data, foxTerritory, ocelotTerritory);
        TerritoryManager.recordAggression(fox, ocelot, level, 3);
        int after = RelationService.effectiveRivalry(data, foxTerritory, ocelotTerritory);
        boolean overlap = foxTerritory.center().distSqr(ocelotTerritory.center())
                < Math.pow((foxTerritory.radiusChunks() + ocelotTerritory.radiusChunks()) * 16.0D, 2.0D);

        helper.assertTrue(RelationService.natural(SpeciesType.FOX, SpeciesType.OCELOT).kind()
                        == RelationKind.BORDERED && overlap,
                "Independent predators lost bordered overlapping-territory semantics");
        helper.assertTrue(after == before + 3,
                "Acquired predator rivalry did not record aggression: " + before + " -> " + after);
        helper.assertTrue(fox.getTarget() == null && ocelot.getTarget() == null,
                "Territorial rivalry forced combat outside vanilla target repertoires");
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
