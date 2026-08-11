package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.VillageGuardianBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Objective integration coverage for issue #14's village, trader and golem family. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class VillageGuardianEcologyGameTests {
    private VillageGuardianEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void villagerBrainProfessionTradingAndPathRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Villager villager = spawn(helper, EntityType.VILLAGER, 4, 1, 4);
        MobMindData.initialize(villager, level);
        villager.setVillagerData(villager.getVillagerData()
                .setProfession(VillagerProfession.FARMER).setLevel(3));
        villager.setVillagerXp(75);
        BlockPos home = helper.absolutePos(new BlockPos(3, 1, 3));
        GlobalPos globalHome = GlobalPos.of(level.dimension(), home);
        villager.getBrain().setMemory(MemoryModuleType.HOME, globalHome);
        villager.setOnGround(true);
        PathNavigation navigation = villager.getNavigation();
        BlockPos destination = helper.absolutePos(new BlockPos(8, 1, 4));
        Path path = navigation.createPath(destination, 0);
        boolean ordered = path != null && navigation.moveTo(path, 0.5D);
        ServerPlayer customer = FakePlayerFactory.getMinecraft(level);
        customer.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        villager.setTradingPlayer(customer);
        MerchantOffers offers = villager.getOffers();

        VillageGuardianBehavior.tick(villager, level);

        helper.assertTrue(villager.getVillagerData().getProfession() == VillagerProfession.FARMER
                        && villager.getVillagerData().getLevel() == 3
                        && villager.getVillagerXp() == 75,
                "Living Ecology changed Villager profession, level or XP");
        helper.assertTrue(villager.getBrain().getMemory(MemoryModuleType.HOME)
                        .filter(globalHome::equals).isPresent(),
                "Living Ecology changed Villager HOME Brain memory");
        helper.assertTrue(villager.getTradingPlayer() == customer && villager.getOffers() == offers,
                "Living Ecology changed Villager trading state or offers");
        helper.assertTrue(ordered && path != null && navigation.getPath() == path,
                "Living Ecology replaced an active Villager Brain navigation path");
        villager.setTradingPlayer(null);
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void wanderingTraderAndLlamaLifecycleRemainTransient(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WanderingTrader trader = spawn(helper, EntityType.WANDERING_TRADER, 4, 1, 4);
        TraderLlama llama = spawn(helper, EntityType.TRADER_LLAMA, 6, 1, 4);
        MobMindData.initialize(trader, level);
        MobMindData.initialize(llama, level);
        trader.setDespawnDelay(700);
        trader.setWanderTarget(helper.absolutePos(new BlockPos(9, 1, 4)));
        llama.setDespawnDelay(700);
        llama.setLeashedTo(trader, true);
        ServerPlayer customer = FakePlayerFactory.getMinecraft(level);
        customer.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        trader.setTradingPlayer(customer);
        MerchantOffers offers = trader.getOffers();
        trader.getNavigation().stop();
        llama.getNavigation().stop();
        CompoundTag llamaBefore = new CompoundTag();
        llama.addAdditionalSaveData(llamaBefore);

        VillageGuardianBehavior.tick(trader, level);
        VillageGuardianBehavior.tick(llama, level);
        CompoundTag llamaAfter = new CompoundTag();
        llama.addAdditionalSaveData(llamaAfter);

        helper.assertTrue(trader.getDespawnDelay() == 700
                        && trader.getTradingPlayer() == customer && trader.getOffers() == offers,
                "Living Ecology changed Wandering Trader despawn or trade state");
        helper.assertTrue(llamaBefore.getInt("DespawnDelay") == llamaAfter.getInt("DespawnDelay")
                        && llama.getLeashHolder() == trader,
                "Living Ecology changed Trader Llama despawn or trader leash state");
        helper.assertTrue(TerritoryManager.ensureTerritory(trader, level) == null
                        && TerritoryManager.ensureTerritory(llama, level) == null
                        && MobMindData.territoryId(trader) == 0L
                        && MobMindData.territoryId(llama) == 0L,
                "Transient trader caravan received persistent territory");
        helper.assertTrue(trader.getNavigation().isDone() && llama.getNavigation().isDone(),
                "Living Ecology issued navigation over trader travel/leash goals");
        trader.setTradingPlayer(null);
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void residentAndGuardianCommunitiesRemainProfiled(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Villager villager = spawn(helper, EntityType.VILLAGER, 3, 1, 3);
        Villager villagerMate = spawn(helper, EntityType.VILLAGER, 5, 1, 3);
        IronGolem iron = spawn(helper, EntityType.IRON_GOLEM, 3, 1, 6);
        IronGolem ironMate = spawn(helper, EntityType.IRON_GOLEM, 5, 1, 6);
        SnowGolem snow = spawn(helper, EntityType.SNOW_GOLEM, 3, 1, 9);
        SnowGolem snowMate = spawn(helper, EntityType.SNOW_GOLEM, 5, 1, 9);
        for (Mob mob : new Mob[]{villager, villagerMate, iron, ironMate, snow, snowMate}) {
            MobMindData.initialize(mob, level);
        }

        TerritoryRecord villagerCommunity = TerritoryManager.ensureTerritory(villager, level);
        TerritoryRecord ironCommunity = TerritoryManager.ensureTerritory(iron, level);
        TerritoryRecord snowCommunity = TerritoryManager.ensureTerritory(snow, level);
        // GameTest worlds persist between invocations. Explicitly process each mate so this covers
        // both fresh territory creation and joining an existing community at the template site.
        TerritoryManager.ensureTerritory(villagerMate, level);
        TerritoryManager.ensureTerritory(ironMate, level);
        TerritoryManager.ensureTerritory(snowMate, level);

        helper.assertTrue(villagerCommunity != null && ironCommunity != null && snowCommunity != null,
                "A profiled resident/guardian community failed to establish territory");
        helper.assertTrue(MobMindData.territoryId(villager) == MobMindData.territoryId(villagerMate)
                        && MobMindData.territoryId(iron) == MobMindData.territoryId(ironMate)
                        && MobMindData.territoryId(snow) == MobMindData.territoryId(snowMate),
                "Same-species village community members received different territory association");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void villagerHurtReportSharesDefenseMemoryWithoutTargets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Villager villager = spawn(helper, EntityType.VILLAGER, 4, 1, 4);
        IronGolem iron = spawn(helper, EntityType.IRON_GOLEM, 6, 1, 4);
        SnowGolem snow = spawn(helper, EntityType.SNOW_GOLEM, 6, 1, 6);
        Zombie attacker = spawn(helper, EntityType.ZOMBIE, 9, 1, 4);
        for (Mob mob : new Mob[]{villager, iron, snow, attacker}) {
            MobMindData.initialize(mob, level);
            mob.getNavigation().stop();
        }
        villager.setLastHurtByMob(attacker);

        VillageGuardianBehavior.tick(villager, level);

        helper.assertTrue(RelationService.natural(SpeciesType.VILLAGER, SpeciesType.IRON_GOLEM)
                        .kind() == RelationKind.SYMBIOTIC,
                "Villager/Iron Golem affinity is no longer symbiotic");
        helper.assertTrue(MobMindData.resolveThreat(iron, level)
                        .filter(entity -> entity == attacker).isPresent()
                        && MobMindData.resolveThreat(snow, level)
                        .filter(entity -> entity == attacker).isPresent(),
                "Nearby village guardians did not receive the Villager's direct-hurt memory");
        helper.assertTrue(iron.getTarget() == null && snow.getTarget() == null,
                "Shared village defense memory forced a guardian target");
        helper.assertTrue(iron.getNavigation().isDone() && snow.getNavigation().isDone(),
                "Defense memory replaced guardian target-selection/navigation goals");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void ironGolemDefenseAngerFlowerAndCreatorRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        IronGolem golem = spawn(helper, EntityType.IRON_GOLEM, 4, 1, 4);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 9, 1, 4);
        MobMindData.initialize(golem, level);
        MobMindData.initialize(target, level);
        golem.setPlayerCreated(true);
        golem.setPersistentAngerTarget(target.getUUID());
        golem.setRemainingPersistentAngerTime(500);
        golem.offerFlower(true);
        golem.setTarget(target);
        golem.getNavigation().stop();
        UUID angerTarget = golem.getPersistentAngerTarget();
        int flowerTicks = golem.getOfferFlowerTick();

        VillageGuardianBehavior.tick(golem, level);

        helper.assertTrue(golem.isPlayerCreated() && golem.getTarget() == target,
                "Living Ecology changed Iron Golem creator or legal defense target");
        helper.assertTrue(Objects.equals(golem.getPersistentAngerTarget(), angerTarget)
                        && golem.getRemainingPersistentAngerTime() == 500,
                "Living Ecology changed Iron Golem persistent anger");
        helper.assertTrue(golem.getOfferFlowerTick() == flowerTicks
                        && golem.getNavigation().isDone(),
                "Living Ecology changed Iron Golem flower or active defense navigation state");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void snowGolemRangedPumpkinAndPlayerBlocksRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SnowGolem golem = spawn(helper, EntityType.SNOW_GOLEM, 4, 1, 4);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 9, 1, 4);
        MobMindData.initialize(golem, level);
        MobMindData.initialize(target, level);
        golem.setPumpkin(false);
        golem.setTarget(target);
        golem.getNavigation().stop();
        BlockPos playerBlock = new BlockPos(7, 1, 7);
        helper.setBlock(playerBlock, Blocks.IRON_BLOCK);

        VillageGuardianBehavior.tick(golem, level);

        helper.assertTrue(!golem.hasPumpkin() && golem.getTarget() == target,
                "Living Ecology changed Snow Golem pumpkin or ranged defense target");
        helper.assertTrue(golem.getNavigation().isDone(),
                "Living Ecology replaced Snow Golem ranged positioning/navigation");
        helper.assertBlockPresent(Blocks.IRON_BLOCK, playerBlock);
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void villageDefenseMemoryIsBoundedToEightReceivers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Villager source = spawn(helper, EntityType.VILLAGER, 4, 1, 4);
        Zombie attacker = spawn(helper, EntityType.ZOMBIE, 9, 1, 4);
        MobMindData.initialize(source, level);
        MobMindData.initialize(attacker, level);
        source.setLastHurtByMob(attacker);
        List<Villager> receivers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Villager receiver = spawn(helper, EntityType.VILLAGER, 5, 1, 5);
            MobMindData.initialize(receiver, level);
            receivers.add(receiver);
        }

        VillageGuardianBehavior.tick(source, level);

        long informed = receivers.stream().filter(receiver -> MobMindData.resolveThreat(receiver, level)
                .filter(entity -> entity == attacker).isPresent()).count();
        helper.assertTrue(informed == 8,
                "Expected exactly eight bounded village memory receivers, got " + informed);
        helper.assertTrue(receivers.stream().allMatch(receiver -> receiver.getTarget() == null),
                "Bounded village memory assigned a Villager combat target");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void creativeTargetAndMemoryAreSanitizedWithoutGuardianTakeover(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        IronGolem golem = spawn(helper, EntityType.IRON_GOLEM, 4, 1, 4);
        MobMindData.initialize(golem, level);
        golem.setPlayerCreated(true);
        ServerPlayer creative = FakePlayerFactory.getMinecraft(level);
        creative.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        creative.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 2, 4))));
        golem.setTarget(creative);
        MobMindData.rememberThreat(golem, creative, 60, level);
        golem.getNavigation().stop();

        VillageGuardianBehavior.tick(golem, level);

        helper.assertTrue(golem.getTarget() == null
                        && MobMindData.resolveThreat(golem, level).isEmpty(),
                "Village guardian retained a Creative target or memory");
        helper.assertTrue(golem.isPlayerCreated() && golem.getNavigation().isDone(),
                "Creative sanitization changed Iron Golem creator or navigation state");
        creative.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
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
