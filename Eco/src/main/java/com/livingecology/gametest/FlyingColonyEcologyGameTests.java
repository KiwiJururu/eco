package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.BehaviorUtil;
import com.livingecology.ai.FlyingColonyBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Objective integration coverage for issue #8's flying-passive and colony behavior. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class FlyingColonyEcologyGameTests {
    private FlyingColonyEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void restingBatRetainsVanillaHangingOwnership(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Bat bat = spawn(helper, EntityType.BAT, 4, 4, 4);
        MobMindData.initialize(bat, level);
        bat.setResting(true);
        bat.getNavigation().stop();

        FlyingColonyBehavior.tick(bat, level);

        helper.assertTrue(bat.isResting(),
                "Living Ecology replaced Bat's vanilla hanging state");
        helper.assertTrue(bat.getNavigation().isDone(),
                "Living Ecology issued PathNavigation movement to a Bat");
        helper.assertTrue(MobMindData.isResting(bat),
                "Bat vanilla resting state was not mirrored for ecological debug state");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void tamedSittingParrotKeepsOwnerCommandState(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Parrot parrot = spawn(helper, EntityType.PARROT, 4, 2, 4);
        Player owner = helper.makeMockPlayer();
        parrot.tame(owner);
        parrot.setOrderedToSit(true);
        parrot.getNavigation().stop();
        MobMindData.initialize(parrot, level);

        FlyingColonyBehavior.tick(parrot, level);

        helper.assertTrue(parrot.isTame(),
                "Living Ecology cleared Parrot's tame state");
        helper.assertTrue(owner.getUUID().equals(parrot.getOwnerUUID()),
                "Living Ecology changed Parrot's persisted owner UUID");
        helper.assertTrue(parrot.isOrderedToSit(),
                "Living Ecology cleared Parrot's sit command");
        helper.assertTrue(parrot.getNavigation().isDone(),
                "Living Ecology stole navigation from a player-owned Parrot");
        helper.assertTrue(MobMindData.territoryId(parrot) == 0L,
                "Player-owned Parrot retained a wild persistent territory");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void allayHeldItemAndDanceRemainBrainOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Allay allay = spawn(helper, EntityType.ALLAY, 4, 3, 4);
        ItemStack held = new ItemStack(Items.DIAMOND);
        allay.setItemInHand(InteractionHand.MAIN_HAND, held.copy());
        allay.setDancing(true);
        allay.getNavigation().stop();
        MobMindData.initialize(allay, level);

        FlyingColonyBehavior.tick(allay, level);

        helper.assertTrue(allay.getMainHandItem().is(Items.DIAMOND),
                "Living Ecology changed Allay's held item contract");
        helper.assertTrue(allay.isDancing(),
                "Living Ecology replaced Allay's jukebox/dance state");
        helper.assertTrue(allay.getNavigation().isDone(),
                "Living Ecology issued movement while AllayAi owned the brain");
        helper.assertTrue(MobMindData.territoryId(allay) == 0L,
                "Non-territorial Allay received a persistent territory");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void beeTerritoryAnchorsToLoadedHiveAndAlarmIsBounded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos relativeHive = new BlockPos(3, 2, 3);
        helper.setBlock(relativeHive, Blocks.BEE_NEST);
        BlockPos hive = helper.absolutePos(relativeHive);

        Bee source = spawn(helper, EntityType.BEE, 4, 3, 3);
        setHive(source, hive);
        MobMindData.initialize(source, level);
        List<Bee> receivers = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            Bee receiver = spawn(helper, EntityType.BEE,
                    4 + i % 5, 3 + i % 2, 4 + i / 5);
            setHive(receiver, hive);
            MobMindData.initialize(receiver, level);
            receivers.add(receiver);
        }
        Zombie threat = spawn(helper, EntityType.ZOMBIE, 9, 2, 8);
        MobMindData.initialize(threat, level);
        source.setTarget(threat);

        TerritoryRecord territory = TerritoryManager.ensureTerritory(source, level);
        helper.assertTrue(territory != null, "Bee colony failed to establish a territory");
        // GameTest worlds persist SavedData between Gradle invocations. Exercise the normal join
        // path for every receiver as production ticks would, whether this run creates or reuses it.
        receivers.forEach(bee -> TerritoryManager.ensureTerritory(bee, level));
        source.getNavigation().stop();
        FlyingColonyBehavior.tick(source, level);

        // SavedData survives Gradle GameTest runs. A nearby colony may therefore already be
        // anchored to another still-loaded hive, which intentionally wins to avoid two bees
        // moving the shared record back and forth between valid hives.
        helper.assertTrue(territory.center().equals(territory.core())
                        && level.getBlockEntity(territory.core()) instanceof BeehiveBlockEntity,
                "Bee territory was not centered on a loaded vanilla hive");
        helper.assertTrue(source.getNavigation().isDone(),
                "Living Ecology replaced Bee hive/pollination navigation");
        long informed = receivers.stream()
                .filter(bee -> MobMindData.resolveThreat(bee, level)
                        .filter(entity -> entity.getUUID().equals(threat.getUUID())).isPresent())
                .count();
        helper.assertTrue(informed > 0L && informed <= 12L,
                "Bee colony alarm was absent or exceeded its 12-receiver budget: " + informed);
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void flyingPatrolSelectionNeverFallsBackToLandOnlyTarget(GameTestHelper helper) {
        fillDeepPool(helper);
        ServerLevel level = helper.getLevel();
        Parrot parrot = spawn(helper, EntityType.PARROT, 6, 11, 6);
        MobMindData.initialize(parrot, level);

        Optional<Vec3> target = BehaviorUtil.safePatrolPoint(parrot, level,
                SpeciesProfile.of(SpeciesType.PARROT), parrot.position(), 5.0D);

        helper.assertTrue(target.isPresent(), "AIR patrol selection found no loaded open-air target");
        BlockPos pos = BlockPos.containing(target.orElseThrow());
        helper.assertTrue(BehaviorUtil.isValidForDomain(level, pos, MovementDomain.AIR),
                "Flying patrol target was not valid for AIR");
        helper.assertTrue(!BehaviorUtil.isValidForDomain(level, pos, MovementDomain.LAND),
                "Flying patrol selection fell back to a land-only target");
        helper.succeed();
    }

    private static void fillDeepPool(GameTestHelper helper) {
        for (int x = 1; x <= 11; x++) {
            for (int z = 1; z <= 11; z++) {
                for (int y = 1; y <= 3; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
            }
        }
    }

    private static void setHive(Bee bee, BlockPos hive) {
        CompoundTag tag = new CompoundTag();
        bee.addAdditionalSaveData(tag);
        tag.put("HivePos", NbtUtils.writeBlockPos(hive));
        bee.readAdditionalSaveData(tag);
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
