package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.IllagerSocietyBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import com.livingecology.territory.TerritoryContext;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import com.livingecology.territory.TerritorySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Objective integration coverage for issue #16's Illager society and raid ownership. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class IllagerSocietyEcologyGameTests {
    private IllagerSocietyEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void raidLifecycleFlagsRemainVanillaOwnedAcrossRoles(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<Raider> raiders = List.of(
                spawn(helper, EntityType.PILLAGER, 2, 1, 2),
                spawn(helper, EntityType.VINDICATOR, 4, 1, 2),
                spawn(helper, EntityType.EVOKER, 6, 1, 2),
                spawn(helper, EntityType.WITCH, 8, 1, 2),
                spawn(helper, EntityType.RAVAGER, 10, 1, 2),
                spawn(helper, EntityType.ILLUSIONER, 12, 1, 2));
        for (Raider raider : raiders) {
            MobMindData.initialize(raider, level);
            raider.setWave(3);
            raider.setCanJoinRaid(false);
            raider.setCelebrating(true);
            IllagerSocietyBehavior.tick(raider, level);
            helper.assertTrue(raider.getWave() == 3 && !raider.canJoinRaid() && raider.isCelebrating(),
                    "Living Ecology changed raid lifecycle state for " + raider.getType());
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void pillagerCrossbowTargetAndActivePathRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Pillager pillager = spawn(helper, EntityType.PILLAGER, 4, 1, 4);
        Villager target = spawn(helper, EntityType.VILLAGER, 9, 1, 4);
        MobMindData.initialize(pillager, level);
        pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        pillager.setChargingCrossbow(true);
        pillager.setTarget(target);
        PathNavigation navigation = pillager.getNavigation();
        Path path = navigation.createPath(target.blockPosition(), 0);
        boolean ordered = path != null && navigation.moveTo(path, 0.6D);

        IllagerSocietyBehavior.tick(pillager, level);

        helper.assertTrue(pillager.isChargingCrossbow()
                        && pillager.getMainHandItem().is(Items.CROSSBOW)
                        && pillager.getTarget() == target,
                "Living Ecology changed Pillager crossbow or legal combat target");
        assertNavigationStillActive(helper, navigation, ordered, "Pillager");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void vindicatorJohnnyAxeTargetAndPathRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vindicator vindicator = spawn(helper, EntityType.VINDICATOR, 4, 1, 4);
        Villager target = spawn(helper, EntityType.VILLAGER, 9, 1, 4);
        MobMindData.initialize(vindicator, level);
        vindicator.setCustomName(Component.literal("Johnny"));
        vindicator.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
        vindicator.setTarget(target);
        PathNavigation navigation = vindicator.getNavigation();
        Path path = navigation.createPath(target.blockPosition(), 0);
        boolean ordered = path != null && navigation.moveTo(path, 0.7D);

        IllagerSocietyBehavior.tick(vindicator, level);

        helper.assertTrue(Component.literal("Johnny").equals(vindicator.getCustomName())
                        && vindicator.getMainHandItem().is(Items.IRON_AXE)
                        && vindicator.getTarget() == target,
                "Living Ecology changed Vindicator Johnny/axe/combat state");
        assertNavigationStillActive(helper, navigation, ordered, "Vindicator");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void evokerVexOwnershipTargetAndPathRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Evoker evoker = spawn(helper, EntityType.EVOKER, 4, 1, 4);
        Vex vex = spawn(helper, EntityType.VEX, 5, 3, 4);
        Villager target = spawn(helper, EntityType.VILLAGER, 9, 1, 4);
        MobMindData.initialize(evoker, level);
        vex.setOwner(evoker);
        evoker.setTarget(target);
        PathNavigation navigation = evoker.getNavigation();
        Path path = navigation.createPath(target.blockPosition(), 0);
        boolean ordered = path != null && navigation.moveTo(path, 0.6D);

        IllagerSocietyBehavior.tick(evoker, level);

        helper.assertTrue(vex.getOwner() == evoker && evoker.getTarget() == target,
                "Living Ecology changed Evoker/Vex ownership or legal Evoker target");
        assertNavigationStillActive(helper, navigation, ordered, "Evoker");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void witchPotionUseTargetAndPathRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Witch witch = spawn(helper, EntityType.WITCH, 4, 1, 4);
        Villager target = spawn(helper, EntityType.VILLAGER, 9, 1, 4);
        MobMindData.initialize(witch, level);
        witch.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.POTION));
        witch.startUsingItem(InteractionHand.MAIN_HAND);
        witch.setTarget(target);
        PathNavigation navigation = witch.getNavigation();
        Path path = navigation.createPath(target.blockPosition(), 0);
        boolean ordered = path != null && navigation.moveTo(path, 0.6D);

        IllagerSocietyBehavior.tick(witch, level);

        helper.assertTrue(witch.isUsingItem() && witch.getMainHandItem().is(Items.POTION)
                        && witch.getTarget() == target,
                "Living Ecology changed Witch potion-use or legal combat state");
        assertNavigationStillActive(helper, navigation, ordered, "Witch");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void ravagerRiderTargetAndPathRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Ravager ravager = spawn(helper, EntityType.RAVAGER, 4, 1, 4);
        Pillager rider = spawn(helper, EntityType.PILLAGER, 4, 2, 4);
        Villager target = spawn(helper, EntityType.VILLAGER, 9, 1, 4);
        MobMindData.initialize(ravager, level);
        rider.startRiding(ravager, true);
        ravager.setTarget(target);
        PathNavigation navigation = ravager.getNavigation();
        Path path = navigation.createPath(target.blockPosition(), 0);
        boolean ordered = path != null && navigation.moveTo(path, 0.7D);

        IllagerSocietyBehavior.tick(ravager, level);

        helper.assertTrue(rider.getVehicle() == ravager && ravager.hasPassenger(rider)
                        && ravager.getTarget() == target,
                "Living Ecology changed Ravager rider or legal combat target");
        assertNavigationStillActive(helper, navigation, ordered, "Ravager");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void illusionerBowTargetAndPathRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Illusioner illusioner = spawn(helper, EntityType.ILLUSIONER, 4, 1, 4);
        Villager target = spawn(helper, EntityType.VILLAGER, 9, 1, 4);
        MobMindData.initialize(illusioner, level);
        illusioner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        illusioner.setTarget(target);
        PathNavigation navigation = illusioner.getNavigation();
        Path path = navigation.createPath(target.blockPosition(), 0);
        boolean ordered = path != null && navigation.moveTo(path, 0.6D);

        IllagerSocietyBehavior.tick(illusioner, level);

        helper.assertTrue(illusioner.getMainHandItem().is(Items.BOW)
                        && illusioner.getTarget() == target,
                "Living Ecology changed Illusioner weapon or legal combat target");
        assertNavigationStillActive(helper, navigation, ordered, "Illusioner");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void crossRoleMemoryIsBoundedAndNeverAssignsTargetsOrPaths(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Pillager source = spawn(helper, EntityType.PILLAGER, 4, 1, 4);
        IronGolem attacker = spawn(helper, EntityType.IRON_GOLEM, 9, 1, 4);
        MobMindData.initialize(source, level);
        MobMindData.initialize(attacker, level);
        source.setLastHurtByMob(attacker);
        List<Evoker> receivers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Evoker receiver = spawn(helper, EntityType.EVOKER, 5, 1, 5);
            MobMindData.initialize(receiver, level);
            receiver.getNavigation().stop();
            receivers.add(receiver);
        }

        IllagerSocietyBehavior.tick(source, level);

        long informed = receivers.stream().filter(receiver -> MobMindData.resolveThreat(receiver, level)
                .filter(entity -> entity == attacker).isPresent()).count();
        helper.assertTrue(informed == 8,
                "Expected exactly eight bounded Illager memory receivers, got " + informed);
        helper.assertTrue(receivers.stream().allMatch(receiver -> receiver.getTarget() == null
                        && receiver.getNavigation().isDone()),
                "Illager society memory assigned a target or path");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void realHurtEventBuildsAcquiredRivalryWithoutForcingCrossRoleTarget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Pillager victim = spawn(helper, EntityType.PILLAGER, 4, 1, 4);
        Evoker helperEvoker = spawn(helper, EntityType.EVOKER, 6, 1, 4);
        IronGolem attacker = spawn(helper, EntityType.IRON_GOLEM, 8, 1, 4);
        for (Mob mob : new Mob[]{victim, helperEvoker, attacker}) MobMindData.initialize(mob, level);

        TerritorySavedData data = TerritorySavedData.get(level);
        BlockPos base = helper.absolutePos(new BlockPos(2, 1, 2));
        TerritoryRecord illagerTerritory = data.create(SpeciesType.PILLAGER, base, base,
                2, 50, 50, 2, 111L, level.getGameTime());
        BlockPos distant = base.offset(96, 0, 0);
        TerritoryRecord villageTerritory = data.create(SpeciesType.IRON_GOLEM, distant, distant,
                2, 50, 50, 2, 222L, level.getGameTime());
        MobMindData.setTerritoryId(victim, illagerTerritory.id());
        MobMindData.setTerritoryId(attacker, villageTerritory.id());
        int initialTension = RelationService.tension(data, illagerTerritory, villageTerritory, false);

        victim.invulnerableTime = 0;
        victim.hurt(level.damageSources().mobAttack(attacker), 2.0F);
        TerritoryManager.recordAggression(victim, attacker, level, 1);
        TerritoryManager.recordAggression(victim, attacker, level, 1);

        var relation = data.getRelation(illagerTerritory.id(), villageTerritory.id());
        int finalTension = RelationService.tension(data, illagerTerritory, villageTerritory, false);
        helper.assertTrue(relation != null && relation.rivalryDelta() >= 3
                        && RelationService.effectiveRivalry(data, illagerTerritory, villageTerritory) > 95
                        && finalTension > initialTension,
                "Repeated Illager-village conflict did not build acquired rivalry/tension");
        helper.assertTrue(MobMindData.resolveThreat(helperEvoker, level)
                        .filter(entity -> entity == attacker).isPresent(),
                "Cross-role Illager helper did not receive local conflict memory");
        helper.assertTrue(helperEvoker.getTarget() == null,
                "Cooperative hurt event forced a target into another Illager role");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void villageTerritorialOverlapIsContestedWithoutPathTakeover(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TerritorySavedData data = TerritorySavedData.get(level);
        BlockPos center = helper.absolutePos(new BlockPos(5, 1, 5));
        TerritoryRecord illager = data.create(SpeciesType.PILLAGER, center.offset(-6, 0, 0),
                center.offset(-6, 0, 0), 3, 70, 60, 4, 333L, level.getGameTime());
        TerritoryRecord village = data.create(SpeciesType.VILLAGER, center.offset(6, 0, 0),
                center.offset(6, 0, 0), 3, 70, 60, 4, 444L, level.getGameTime());

        TerritoryContext context = TerritoryManager.contextAt(level, center, illager.id());

        helper.assertTrue(context.contested() && context.tension() >= 60,
                "Illager/village territorial overlap was not recognized as contested");
        helper.assertTrue(RelationService.natural(illager.species(), village.species()).kind()
                        == RelationKind.WARLIKE,
                "Illager/village territorial overlap lost its WARLIKE relationship");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void auditedSocietyHurtAlarmsShareMemoryWithoutForcingVanillaTargets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Piglin piglin = spawn(helper, EntityType.PIGLIN, 3, 1, 3);
        PiglinBrute brute = spawn(helper, EntityType.PIGLIN_BRUTE, 5, 1, 3);
        Zombie piglinAttacker = spawn(helper, EntityType.ZOMBIE, 8, 1, 3);
        Villager villager = spawn(helper, EntityType.VILLAGER, 3, 1, 8);
        IronGolem golem = spawn(helper, EntityType.IRON_GOLEM, 5, 1, 8);
        Zombie villageAttacker = spawn(helper, EntityType.ZOMBIE, 8, 1, 8);
        for (Mob mob : new Mob[]{piglin, brute, piglinAttacker, villager, golem, villageAttacker}) {
            MobMindData.initialize(mob, level);
        }

        piglin.invulnerableTime = 0;
        villager.invulnerableTime = 0;
        piglin.hurt(level.damageSources().mobAttack(piglinAttacker), 2.0F);
        villager.hurt(level.damageSources().mobAttack(villageAttacker), 2.0F);

        helper.assertTrue(MobMindData.resolveThreat(brute, level)
                        .filter(entity -> entity == piglinAttacker).isPresent()
                        && brute.getTarget() == null,
                "Piglin cooperative hurt alarm failed memory-only target ownership");
        helper.assertTrue(MobMindData.resolveThreat(golem, level)
                        .filter(entity -> entity == villageAttacker).isPresent()
                        && golem.getTarget() == null,
                "Village cooperative hurt alarm failed memory-only target ownership");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void creativeTargetAndMemoryAreSanitizedWithoutRaidNavigation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Pillager pillager = spawn(helper, EntityType.PILLAGER, 4, 1, 4);
        MobMindData.initialize(pillager, level);
        ServerPlayer creative = FakePlayerFactory.getMinecraft(level);
        creative.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        creative.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 1, 4))));
        pillager.setTarget(creative);
        MobMindData.rememberThreat(pillager, creative, 60, level);
        pillager.getNavigation().stop();

        IllagerSocietyBehavior.tick(pillager, level);

        helper.assertTrue(pillager.getTarget() == null
                        && MobMindData.resolveThreat(pillager, level).isEmpty(),
                "Illager retained a Creative player target or threat memory");
        helper.assertTrue(pillager.getNavigation().isDone(),
                "Creative sanitization issued raid navigation");
        creative.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        helper.succeed();
    }

    private static void assertNavigationStillActive(GameTestHelper helper, PathNavigation navigation,
                                                    boolean ordered, String owner) {
        helper.assertTrue(ordered && navigation.getPath() != null && !navigation.isDone(),
                "Living Ecology stopped or cleared active " + owner + " navigation");
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
