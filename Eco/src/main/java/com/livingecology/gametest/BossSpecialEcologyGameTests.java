package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.BossSpecialBehavior;
import com.livingecology.data.MobMindData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Conservative runtime coverage for issue #18. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class BossSpecialEcologyGameTests {
    private BossSpecialEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void wardenTargetAndRawCombatAttributesRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Warden warden = spawn(helper, EntityType.WARDEN, 4, 1, 4);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 8, 1, 4);
        MobMindData.initialize(warden, level);
        double health = warden.getAttributeValue(Attributes.MAX_HEALTH);
        double damage = warden.getAttributeValue(Attributes.ATTACK_DAMAGE);
        warden.setTarget(target);
        warden.getNavigation().stop();
        BossSpecialBehavior.tick(warden, level);
        helper.assertTrue(warden.getTarget() == target && warden.getNavigation().isDone(),
                "Ecology replaced Warden target/navigation ownership");
        helper.assertTrue(warden.getAttributeValue(Attributes.MAX_HEALTH) == health
                        && warden.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage,
                "Ecology inflated Warden raw combat attributes");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void witherInvulnerabilityTargetAndAttributesRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WitherBoss wither = spawn(helper, EntityType.WITHER, 4, 1, 4);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 8, 1, 4);
        MobMindData.initialize(wither, level);
        wither.setInvulnerableTicks(37);
        wither.setTarget(target);
        double health = wither.getAttributeValue(Attributes.MAX_HEALTH);
        double damage = wither.getAttributeValue(Attributes.ATTACK_DAMAGE);
        BossSpecialBehavior.tick(wither, level);
        helper.assertTrue(wither.getInvulnerableTicks() == 37 && wither.getTarget() == target,
                "Ecology changed Wither invulnerability phase or target");
        helper.assertTrue(wither.getAttributeValue(Attributes.MAX_HEALTH) == health
                        && wither.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage,
                "Ecology inflated Wither raw combat attributes");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void dragonPhaseAndRawHealthRemainUntouched(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        EnderDragon dragon = spawn(helper, EntityType.ENDER_DRAGON, 6, 5, 6);
        MobMindData.initialize(dragon, level);
        Object phase = dragon.getPhaseManager().getCurrentPhase();
        double health = dragon.getAttributeValue(Attributes.MAX_HEALTH);
        BossSpecialBehavior.tick(dragon, level);
        helper.assertTrue(dragon.getPhaseManager().getCurrentPhase() == phase,
                "Ecology replaced Ender Dragon phase manager state");
        helper.assertTrue(dragon.getAttributeValue(Attributes.MAX_HEALTH) == health,
                "Ecology inflated Ender Dragon max health");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void giantBaselineDoesNotInventTargetOrNavigation(GameTestHelper helper) {
        Giant giant = spawn(helper, EntityType.GIANT, 4, 1, 4);
        MobMindData.initialize(giant, helper.getLevel());
        giant.setTarget(null);
        giant.getNavigation().stop();
        BossSpecialBehavior.tick(giant, helper.getLevel());
        helper.assertTrue(giant.getTarget() == null && giant.getNavigation().isDone(),
                "Ecology invented unsupported Giant target/navigation behavior");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void skeletonHorseTrapAndTameStateRemainVanillaOwned(GameTestHelper helper) {
        SkeletonHorse horse = spawn(helper, EntityType.SKELETON_HORSE, 4, 1, 4);
        MobMindData.initialize(horse, helper.getLevel());
        horse.setTrap(true);
        horse.setTamed(true);
        BossSpecialBehavior.tick(horse, helper.getLevel());
        helper.assertTrue(horse.isTrap() && horse.isTamed(),
                "Ecology changed Skeleton Horse trap/tame state");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void zombieHorseTameStateAndRawHealthRemainVanillaOwned(GameTestHelper helper) {
        ZombieHorse horse = spawn(helper, EntityType.ZOMBIE_HORSE, 4, 1, 4);
        MobMindData.initialize(horse, helper.getLevel());
        horse.setTamed(true);
        double health = horse.getAttributeValue(Attributes.MAX_HEALTH);
        BossSpecialBehavior.tick(horse, helper.getLevel());
        helper.assertTrue(horse.isTamed(), "Ecology changed Zombie Horse tame state");
        helper.assertTrue(horse.getAttributeValue(Attributes.MAX_HEALTH) == health,
                "Ecology changed Zombie Horse raw health");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void idleSpecialBatchNeverReceivesGenericPatrolOrders(GameTestHelper helper) {
        Mob[] actors = {
                spawn(helper, EntityType.WARDEN, 2, 1, 2),
                spawn(helper, EntityType.WITHER, 5, 3, 2),
                spawn(helper, EntityType.GIANT, 8, 1, 2),
                spawn(helper, EntityType.SKELETON_HORSE, 11, 1, 2),
                spawn(helper, EntityType.ZOMBIE_HORSE, 14, 1, 2)
        };
        for (Mob mob : actors) {
            MobMindData.initialize(mob, helper.getLevel());
            mob.setTarget(null);
            mob.getNavigation().stop();
            BossSpecialBehavior.tick(mob, helper.getLevel());
            helper.assertTrue(mob.getTarget() == null && mob.getNavigation().isDone(),
                    "Special overlay invented generic patrol/target behavior for " + mob.getType());
        }
        helper.succeed();
    }

    private static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type, int x, int y, int z) {
        T mob = type.create(helper.getLevel());
        if (mob == null) throw new IllegalStateException("Could not create " + BuiltInRegistries.ENTITY_TYPE.getKey(type));
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        mob.moveTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }
}
