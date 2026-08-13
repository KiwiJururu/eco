package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.PiglinSocietyBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import com.livingecology.territory.TerritorySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
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

/** Objective integration coverage for issue #15's Piglin society and Bastion ecology. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class PiglinSocietyEcologyGameTests {
    private PiglinSocietyEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void piglinBrainGoldFearHuntAndPathRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Piglin piglin = spawn(helper, EntityType.PIGLIN, 4, 1, 4);
        ZombifiedPiglin avoid = spawn(helper, EntityType.ZOMBIFIED_PIGLIN, 9, 1, 4);
        MobMindData.initialize(piglin, level);
        MobMindData.initialize(avoid, level);
        piglin.setImmuneToZombification(true);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        piglin.setItemSlot(EquipmentSlot.OFFHAND, gold.copy());
        piglin.getBrain().setMemory(MemoryModuleType.ADMIRING_ITEM, true);
        piglin.getBrain().setMemory(MemoryModuleType.HUNTED_RECENTLY, true);
        piglin.getBrain().setMemory(MemoryModuleType.AVOID_TARGET, avoid);
        piglin.setOnGround(true);
        PathNavigation navigation = piglin.getNavigation();
        BlockPos destination = helper.absolutePos(new BlockPos(8, 1, 4));
        Path path = navigation.createPath(destination, 0);
        boolean ordered = path != null && navigation.moveTo(path, 0.5D);

        PiglinSocietyBehavior.tick(piglin, level);

        helper.assertTrue(piglin.getBrain().getMemory(MemoryModuleType.ADMIRING_ITEM)
                        .orElse(false)
                        && piglin.getBrain().getMemory(MemoryModuleType.HUNTED_RECENTLY)
                        .orElse(false),
                "Living Ecology changed Piglin admiration or hunt Brain state");
        helper.assertTrue(piglin.getBrain().getMemory(MemoryModuleType.AVOID_TARGET)
                        .filter(entity -> entity == avoid).isPresent(),
                "Living Ecology changed Piglin fear/avoid Brain ownership");
        helper.assertTrue(ItemStack.isSameItemSameTags(gold, piglin.getOffhandItem())
                        && piglin.isImmuneToZombification(),
                "Living Ecology changed Piglin gold/barter or zombification lifecycle state");
        helper.assertTrue(ordered && path != null && navigation.getPath() == path,
                "Living Ecology replaced an active Piglin Brain navigation path");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void piglinBruteBrainHomeTargetAndPathRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PiglinBrute brute = spawn(helper, EntityType.PIGLIN_BRUTE, 4, 1, 4);
        WitherSkeleton target = spawn(helper, EntityType.WITHER_SKELETON, 9, 1, 4);
        MobMindData.initialize(brute, level);
        MobMindData.initialize(target, level);
        brute.setImmuneToZombification(true);
        BlockPos homePos = helper.absolutePos(new BlockPos(4, 1, 4));
        GlobalPos home = GlobalPos.of(level.dimension(), homePos);
        brute.getBrain().setMemory(MemoryModuleType.HOME, home);
        brute.setTarget(target);
        brute.setOnGround(true);
        PathNavigation navigation = brute.getNavigation();
        Path path = navigation.createPath(target.blockPosition(), 0);
        boolean ordered = path != null && navigation.moveTo(path, 0.6D);

        PiglinSocietyBehavior.tick(brute, level);

        helper.assertTrue(brute.getTarget() == target && brute.isImmuneToZombification(),
                "Living Ecology changed Piglin Brute legal combat or zombification state");
        helper.assertTrue(brute.getBrain().getMemory(MemoryModuleType.HOME)
                        .filter(home::equals).isPresent(),
                "Living Ecology changed Piglin Brute Bastion HOME memory");
        helper.assertTrue(ordered && path != null && navigation.getPath() == path,
                "Living Ecology replaced Piglin Brute Brain navigation");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void livingPiglinCommunitiesRemainSpeciesScopedButCooperative(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Piglin piglin = spawn(helper, EntityType.PIGLIN, 3, 1, 3);
        Piglin piglinMate = spawn(helper, EntityType.PIGLIN, 5, 1, 3);
        PiglinBrute brute = spawn(helper, EntityType.PIGLIN_BRUTE, 3, 1, 7);
        PiglinBrute bruteMate = spawn(helper, EntityType.PIGLIN_BRUTE, 5, 1, 7);
        for (Mob mob : new Mob[]{piglin, piglinMate, brute, bruteMate}) {
            MobMindData.initialize(mob, level);
            mob.getNavigation().stop();
        }

        TerritoryRecord piglinCommunity = TerritoryManager.ensureTerritory(piglin, level);
        TerritoryRecord bruteCommunity = TerritoryManager.ensureTerritory(brute, level);
        TerritoryManager.ensureTerritory(piglinMate, level);
        TerritoryManager.ensureTerritory(bruteMate, level);

        helper.assertTrue(piglinCommunity != null && bruteCommunity != null,
                "Profiled Piglin/Brute communities failed to establish persistent territory");
        helper.assertTrue(MobMindData.territoryId(piglin) == MobMindData.territoryId(piglinMate)
                        && MobMindData.territoryId(brute) == MobMindData.territoryId(bruteMate),
                "Same-species Piglin society members received different territory association");
        helper.assertTrue(MobMindData.territoryId(piglin) != MobMindData.territoryId(brute),
                "Piglin and Brute communities were collapsed into one species identity");
        TerritorySavedData data = TerritorySavedData.get(level);
        helper.assertTrue(RelationService.cooperation(data, piglinCommunity, bruteCommunity) >= 80
                        && RelationService.natural(SpeciesType.PIGLIN, SpeciesType.PIGLIN_BRUTE)
                        .kind() == RelationKind.SYMBIOTIC,
                "Piglin/Brute society affinity no longer yields strong local cooperation");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void livingPiglinHurtReportSharesMemoryButExcludesZombifiedPiglins(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Piglin source = spawn(helper, EntityType.PIGLIN, 4, 1, 4);
        PiglinBrute receiver = spawn(helper, EntityType.PIGLIN_BRUTE, 6, 1, 4);
        ZombifiedPiglin outsider = spawn(helper, EntityType.ZOMBIFIED_PIGLIN, 6, 1, 6);
        WitherSkeleton attacker = spawn(helper, EntityType.WITHER_SKELETON, 9, 1, 4);
        for (Mob mob : new Mob[]{source, receiver, outsider, attacker}) {
            MobMindData.initialize(mob, level);
            mob.getNavigation().stop();
        }
        source.setLastHurtByMob(attacker);

        PiglinSocietyBehavior.tick(source, level);

        helper.assertTrue(MobMindData.resolveThreat(receiver, level)
                        .filter(entity -> entity == attacker).isPresent(),
                "Nearby Piglin Brute did not receive living-society threat memory");
        helper.assertTrue(MobMindData.resolveThreat(outsider, level).isEmpty(),
                "Zombified Piglin was collapsed into living Piglin society memory");
        helper.assertTrue(receiver.getTarget() == null && outsider.getTarget() == null,
                "Piglin society memory forced a combat target");
        helper.assertTrue(receiver.getNavigation().isDone() && outsider.getNavigation().isDone(),
                "Piglin society memory replaced Brain/undead navigation ownership");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void witherSkeletonRivalryDoesNotInventPiglinTargetOrPath(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Piglin piglin = spawn(helper, EntityType.PIGLIN, 4, 1, 4);
        spawn(helper, EntityType.WITHER_SKELETON, 8, 1, 4);
        MobMindData.initialize(piglin, level);
        piglin.getNavigation().stop();

        PiglinSocietyBehavior.tick(piglin, level);

        helper.assertTrue(RelationService.natural(SpeciesType.PIGLIN, SpeciesType.WITHER_SKELETON)
                        .kind() == RelationKind.WARLIKE,
                "Piglin/Wither Skeleton rivalry is no longer WARLIKE");
        helper.assertTrue(piglin.getTarget() == null && piglin.getNavigation().isDone(),
                "Ecological rivalry invented a Piglin target or navigation order");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void livingPiglinMemoryIsBoundedToEightReceivers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Piglin source = spawn(helper, EntityType.PIGLIN, 4, 1, 4);
        WitherSkeleton attacker = spawn(helper, EntityType.WITHER_SKELETON, 9, 1, 4);
        MobMindData.initialize(source, level);
        MobMindData.initialize(attacker, level);
        source.setLastHurtByMob(attacker);
        List<PiglinBrute> receivers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PiglinBrute receiver = spawn(helper, EntityType.PIGLIN_BRUTE, 5, 1, 5);
            MobMindData.initialize(receiver, level);
            receivers.add(receiver);
        }

        PiglinSocietyBehavior.tick(source, level);

        long informed = receivers.stream().filter(receiver -> MobMindData.resolveThreat(receiver, level)
                .filter(entity -> entity == attacker).isPresent()).count();
        helper.assertTrue(informed == 8,
                "Expected exactly eight bounded Piglin memory receivers, got " + informed);
        helper.assertTrue(receivers.stream().allMatch(receiver -> receiver.getTarget() == null),
                "Bounded Piglin society memory assigned a combat target");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void creativeTargetAndMemoryAreSanitizedWithoutBrainTakeover(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Piglin piglin = spawn(helper, EntityType.PIGLIN, 4, 1, 4);
        MobMindData.initialize(piglin, level);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        piglin.setItemSlot(EquipmentSlot.OFFHAND, gold.copy());
        ServerPlayer creative = FakePlayerFactory.getMinecraft(level);
        creative.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        creative.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 1, 4))));
        piglin.setTarget(creative);
        MobMindData.rememberThreat(piglin, creative, 60, level);
        piglin.getNavigation().stop();

        PiglinSocietyBehavior.tick(piglin, level);

        helper.assertTrue(piglin.getTarget() == null
                        && MobMindData.resolveThreat(piglin, level).isEmpty(),
                "Piglin retained a Creative player as target or threat memory");
        helper.assertTrue(ItemStack.isSameItemSameTags(gold, piglin.getOffhandItem())
                        && piglin.getNavigation().isDone(),
                "Creative sanitization changed Piglin barter item or Brain navigation");
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
