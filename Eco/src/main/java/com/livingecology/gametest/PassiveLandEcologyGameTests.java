package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.PassiveLandBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
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
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Objective integration coverage for issue #9's common passive land animals. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class PassiveLandEcologyGameTests {
    private PassiveLandEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void camelIsNotMisclassifiedAsPlayerOwnedHorse(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Camel first = spawn(helper, EntityType.CAMEL, 4, 2, 4);
        Camel second = spawn(helper, EntityType.CAMEL, 6, 2, 4);
        MobMindData.initialize(first, level);
        MobMindData.initialize(second, level);
        first.sitDown();
        first.getNavigation().stop();

        TerritoryRecord territory = TerritoryManager.ensureTerritory(first, level);
        PassiveLandBehavior.tick(first, level);

        helper.assertTrue(first.isTamed(), "Vanilla Camel classification unexpectedly changed");
        helper.assertTrue(ReproductionManager.isEligibleBase(first),
                "Always-tamed vanilla Camel remained excluded from ecological reproduction");
        helper.assertTrue(territory != null && MobMindData.territoryId(first) == territory.id(),
                "Camel failed to establish its profiled desert home range");
        helper.assertTrue(first.isCamelSitting() && first.getNavigation().isDone(),
                "Living Ecology replaced Camel's Brain-owned sitting state");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void tamedDonkeyKeepsOwnerChestAndNoWildTerritory(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Donkey donkey = spawn(helper, EntityType.DONKEY, 4, 2, 4);
        UUID owner = UUID.randomUUID();
        donkey.setOwnerUUID(owner);
        donkey.setTamed(true);
        donkey.setChest(true);
        donkey.getNavigation().stop();
        MobMindData.initialize(donkey, level);

        PassiveLandBehavior.tick(donkey, level);

        helper.assertTrue(donkey.isTamed() && owner.equals(donkey.getOwnerUUID()),
                "Living Ecology changed Donkey tame/owner state");
        helper.assertTrue(donkey.hasChest(), "Living Ecology changed Donkey chest inventory state");
        helper.assertTrue(MobMindData.territoryId(donkey) == 0L,
                "Player-owned Donkey retained a wild home range");
        helper.assertTrue(donkey.getNavigation().isDone(),
                "Living Ecology stole navigation from a player-owned Donkey");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void riddenPigKeepsMountControlAndCannotEnterEcologicalLove(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Pig pig = spawn(helper, EntityType.PIG, 4, 2, 4);
        Player rider = helper.makeMockPlayer();
        pig.equipSaddle(SoundSource.NEUTRAL);
        rider.startRiding(pig, true);
        pig.getNavigation().stop();
        MobMindData.initialize(pig, level);

        PassiveLandBehavior.tick(pig, level);

        helper.assertTrue(pig.isSaddled() && pig.isVehicle() && rider.getVehicle() == pig,
                "Living Ecology changed Pig saddle/rider state");
        helper.assertTrue(!ReproductionManager.isEligibleBase(pig),
                "Ridden Pig remained eligible for automatic ecological reproduction");
        helper.assertTrue(pig.getNavigation().isDone(),
                "Living Ecology stole navigation from a ridden Pig");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void killerRabbitRetainsVariantAndCombatOwnership(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Rabbit rabbit = spawn(helper, EntityType.RABBIT, 4, 2, 4);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 8, 2, 4);
        rabbit.setVariant(Rabbit.Variant.EVIL);
        rabbit.setTarget(target);
        rabbit.getNavigation().stop();
        MobMindData.initialize(rabbit, level);
        MobMindData.initialize(target, level);

        PassiveLandBehavior.tick(rabbit, level);

        helper.assertTrue(rabbit.getVariant() == Rabbit.Variant.EVIL,
                "Living Ecology changed Killer Bunny variant");
        helper.assertTrue(rabbit.getTarget() == target,
                "Passive overlay replaced Killer Bunny combat target");
        helper.assertTrue(rabbit.getNavigation().isDone(),
                "Passive overlay issued a herd path during Killer Bunny combat");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void goatRamMemoryRemainsBrainOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Goat goat = spawn(helper, EntityType.GOAT, 4, 2, 4);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 8, 2, 4);
        MobMindData.initialize(goat, level);
        MobMindData.initialize(target, level);
        goat.getBrain().setMemory(MemoryModuleType.RAM_TARGET, target.position());
        goat.getNavigation().stop();

        PassiveLandBehavior.tick(goat, level);

        helper.assertTrue(goat.getBrain().getMemory(MemoryModuleType.RAM_TARGET)
                        .filter(position -> position.equals(target.position())).isPresent(),
                "Living Ecology replaced Goat's Brain ram target");
        helper.assertTrue(goat.getNavigation().isDone(),
                "Living Ecology issued generic herd movement during Goat ram preparation");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void snifferSearchStateRemainsBrainOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Sniffer sniffer = spawn(helper, EntityType.SNIFFER, 4, 2, 4);
        MobMindData.initialize(sniffer, level);
        sniffer.transitionTo(Sniffer.State.SEARCHING);
        sniffer.getNavigation().stop();

        PassiveLandBehavior.tick(sniffer, level);

        helper.assertTrue(sniffer.isSearching(),
                "Living Ecology replaced Sniffer's search/dig state machine");
        helper.assertTrue(sniffer.getNavigation().isDone(),
                "Living Ecology issued generic wandering during Sniffer search");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void llamaCaravanAndTraderLifecycleRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Llama head = spawn(helper, EntityType.LLAMA, 4, 2, 4);
        Llama tail = spawn(helper, EntityType.LLAMA, 6, 2, 4);
        TraderLlama trader = spawn(helper, EntityType.TRADER_LLAMA, 8, 2, 4);
        for (Llama llama : new Llama[]{head, tail, trader}) MobMindData.initialize(llama, level);
        tail.joinCaravan(head);
        tail.getNavigation().stop();
        trader.getNavigation().stop();

        PassiveLandBehavior.tick(tail, level);
        PassiveLandBehavior.tick(trader, level);

        helper.assertTrue(tail.inCaravan() && tail.getCaravanHead() == head,
                "Living Ecology changed Llama caravan links");
        helper.assertTrue(tail.getNavigation().isDone(),
                "Living Ecology replaced caravan navigation");
        helper.assertTrue(!SpeciesProfile.of(SpeciesType.TRADER_LLAMA).formsPersistentTerritory(false)
                        && MobMindData.territoryId(trader) == 0L,
                "Trader Llama received persistent territory despite trader lifecycle ownership");
        helper.assertTrue(trader.getNavigation().isDone(),
                "Living Ecology replaced Trader Llama leash/defense/despawn navigation");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void sheepReproductionUsesVanillaLoveAndCapacity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Sheep first = spawn(helper, EntityType.SHEEP, 4, 2, 5);
        Sheep second = spawn(helper, EntityType.SHEEP, 6, 2, 5);
        for (Sheep sheep : new Sheep[]{first, second}) {
            MobMindData.initialize(sheep, level);
            MobMindData.get(sheep).putLong("nextReproduction", 0L);
        }
        TerritoryRecord territory = TerritoryManager.ensureTerritory(first, level);
        helper.assertTrue(territory != null, "Sheep pair failed to establish a home range");
        AABB bounds = new AABB(helper.absolutePos(new BlockPos(1, 1, 1)),
                helper.absolutePos(new BlockPos(11, 4, 11)));

        int pairs = ReproductionManager.triggerEligiblePairs(level, bounds);

        helper.assertTrue(pairs == 1, "Expected one eligible Sheep pair, got " + pairs);
        helper.assertTrue(first.isInLove() && second.isInLove(),
                "Ecological reproduction bypassed Sheep's vanilla love state");
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
