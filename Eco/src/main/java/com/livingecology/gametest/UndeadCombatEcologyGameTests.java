package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.UndeadCombatBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;
import java.util.UUID;

/** Objective integration coverage for issue #11's skeleton and undead-combat family. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class UndeadCombatEcologyGameTests {
    private UndeadCombatEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void skeletonBowPositionAndSkeletonStrayMemoryRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Skeleton skeleton = spawn(helper, EntityType.SKELETON, 4, 1, 4);
        Stray receiver = spawn(helper, EntityType.STRAY, 6, 1, 4);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 9, 1, 4);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new ItemStack(Items.BOW));
        skeleton.reassessWeaponGoal();
        skeleton.setOnGround(true);
        skeleton.getNavigation().stop();
        MobMindData.initialize(skeleton, level);
        MobMindData.initialize(receiver, level);
        MobMindData.initialize(target, level);
        skeleton.setTarget(target);

        UndeadCombatBehavior.tick(skeleton, level);

        helper.assertTrue(skeleton.getTarget() == target,
                "Living Ecology replaced the Skeleton's legal vanilla target");
        helper.assertTrue(skeleton.getNavigation().isDone(),
                "Living Ecology replaced bow strafe/ranged positioning with path recovery");
        helper.assertTrue(MobMindData.resolveThreat(receiver, level)
                        .filter(entity -> entity.getUUID().equals(target.getUUID())).isPresent(),
                "Skeleton/Stray combat knowledge was not shared locally");
        helper.assertTrue(receiver.getTarget() == null,
                "Shared combat knowledge forced a Stray target");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void witherSkeletonRivalryNeedsVanillaTargetAndMeleeCanRecover(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WitherSkeleton skeleton = spawn(helper, EntityType.WITHER_SKELETON, 4, 1, 4);
        Piglin piglin = spawn(helper, EntityType.PIGLIN, 9, 1, 4);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new ItemStack(Items.STONE_SWORD));
        skeleton.setOnGround(true);
        skeleton.getNavigation().stop();
        MobMindData.initialize(skeleton, level);
        MobMindData.initialize(piglin, level);

        UndeadCombatBehavior.tick(skeleton, level);
        helper.assertTrue(RelationService.natural(SpeciesType.PIGLIN, SpeciesType.WITHER_SKELETON).kind()
                        == RelationKind.WARLIKE && skeleton.getTarget() == null,
                "Natural rivalry forced a target outside vanilla target selection");

        skeleton.setTarget(piglin);
        UndeadCombatBehavior.tick(skeleton, level);
        helper.assertTrue(skeleton.getTarget() == piglin,
                "Living Ecology replaced the Wither Skeleton's legal Piglin target");
        helper.assertTrue(!skeleton.getNavigation().isDone(),
                "Idle melee Wither Skeleton did not recover navigation toward a distant target");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void zombifiedPiglinAngerAndGroupMemoryRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ZombifiedPiglin source = spawn(helper, EntityType.ZOMBIFIED_PIGLIN, 4, 1, 4);
        ZombifiedPiglin receiver = spawn(helper, EntityType.ZOMBIFIED_PIGLIN, 6, 1, 4);
        WitherSkeleton target = spawn(helper, EntityType.WITHER_SKELETON, 9, 1, 4);
        source.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new ItemStack(Items.GOLDEN_SWORD));
        source.setOnGround(true);
        source.getNavigation().stop();
        MobMindData.initialize(source, level);
        MobMindData.initialize(receiver, level);
        MobMindData.initialize(target, level);
        source.setPersistentAngerTarget(target.getUUID());
        source.setRemainingPersistentAngerTime(400);
        source.setTarget(target);
        UUID angerTarget = source.getPersistentAngerTarget();
        int angerTime = source.getRemainingPersistentAngerTime();

        UndeadCombatBehavior.tick(source, level);

        helper.assertTrue(source.getTarget() == target
                        && Objects.equals(angerTarget, source.getPersistentAngerTarget())
                        && source.getRemainingPersistentAngerTime() == angerTime,
                "Living Ecology replaced Zombified Piglin persistent anger");
        helper.assertTrue(MobMindData.resolveThreat(receiver, level)
                        .filter(entity -> entity.getUUID().equals(target.getUUID())).isPresent(),
                "Nearby Zombified Piglin did not receive bounded group combat memory");
        helper.assertTrue(receiver.getTarget() == null,
                "Zombified Piglin group memory forced an aggression target");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void creativePlayerTargetAndMemoryAreSanitized(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Skeleton skeleton = spawn(helper, EntityType.SKELETON, 4, 1, 4);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new ItemStack(Items.BOW));
        skeleton.reassessWeaponGoal();
        skeleton.getNavigation().stop();
        MobMindData.initialize(skeleton, level);
        ServerPlayer creative = FakePlayerFactory.getMinecraft(level);
        creative.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        creative.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 1, 4))));
        skeleton.setTarget(creative);
        MobMindData.rememberThreat(skeleton, creative, 60, level);

        UndeadCombatBehavior.tick(skeleton, level);

        helper.assertTrue(skeleton.getTarget() == null,
                "Skeleton retained a Creative player as target");
        helper.assertTrue(MobMindData.resolveThreat(skeleton, level).isEmpty(),
                "Skeleton retained a Creative player in combat memory");
        creative.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void daylightFireEquipmentAndBowMovementRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long originalDayTime = level.getDayTime();
        level.setDayTime(6000L);
        Skeleton skeleton = spawn(helper, EntityType.SKELETON, 4, 1, 4);
        ItemStack bow = new ItemStack(Items.BOW);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, bow.copy());
        skeleton.reassessWeaponGoal();
        skeleton.setRemainingFireTicks(80);
        skeleton.getNavigation().stop();
        MobMindData.initialize(skeleton, level);

        UndeadCombatBehavior.tick(skeleton, level);

        helper.assertTrue(ItemStack.isSameItemSameTags(bow, skeleton.getMainHandItem()),
                "Living Ecology changed Skeleton combat equipment in daylight");
        helper.assertTrue(skeleton.getRemainingFireTicks() == 80,
                "Living Ecology replaced vanilla daylight/fire ownership");
        helper.assertTrue(skeleton.getNavigation().isDone(),
                "Living Ecology replaced vanilla daylight avoidance movement");
        level.setDayTime(originalDayTime);
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
