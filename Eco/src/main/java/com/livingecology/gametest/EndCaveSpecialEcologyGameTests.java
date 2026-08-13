package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.BehaviorUtil;
import com.livingecology.ai.EndCaveSpecialBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Objective integration coverage for issue #13's End/cave special family. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class EndCaveSpecialEcologyGameTests {
    private EndCaveSpecialEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void endermanVanillaSelectsEndermiteUnderNaturalWar(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        EnderMan enderman = spawn(helper, EntityType.ENDERMAN, 4, 1, 4);
        Endermite endermite = spawn(helper, EntityType.ENDERMITE, 7, 1, 4);
        MobMindData.initialize(enderman, level);
        MobMindData.initialize(endermite, level);

        helper.succeedWhen(() -> {
            EndCaveSpecialBehavior.tick(enderman, level);
            helper.assertTrue(enderman.getTarget() == endermite,
                    "Vanilla Enderman target selector did not retain Endermite priority");
            helper.assertTrue(RelationService.natural(SpeciesType.ENDERMAN, SpeciesType.ENDERMITE)
                            .kind() == RelationKind.WARLIKE,
                    "Enderman/Endermite natural relation is no longer warlike");
        });
    }

    @GameTest(template = "test_arena")
    public static void endermanCarriedBlockAngerAndMovementRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        EnderMan enderman = spawn(helper, EntityType.ENDERMAN, 4, 1, 4);
        Endermite target = spawn(helper, EntityType.ENDERMITE, 9, 1, 4);
        MobMindData.initialize(enderman, level);
        MobMindData.initialize(target, level);
        enderman.setCarriedBlock(Blocks.GRASS_BLOCK.defaultBlockState());
        UUID angerTarget = target.getUUID();
        enderman.setPersistentAngerTarget(angerTarget);
        enderman.setRemainingPersistentAngerTime(400);
        enderman.setTarget(target);
        Vec3 motion = new Vec3(0.12D, 0.03D, -0.08D);
        enderman.setDeltaMovement(motion);
        enderman.getNavigation().stop();

        EndCaveSpecialBehavior.tick(enderman, level);

        helper.assertTrue(enderman.getCarriedBlock() != null
                        && enderman.getCarriedBlock().is(Blocks.GRASS_BLOCK),
                "Living Ecology changed the Enderman carried block");
        helper.assertTrue(enderman.getTarget() == target
                        && Objects.equals(enderman.getPersistentAngerTarget(), angerTarget)
                        && enderman.getRemainingPersistentAngerTime() == 400,
                "Living Ecology changed Enderman target or persistent anger");
        helper.assertTrue(enderman.getDeltaMovement().equals(motion)
                        && enderman.getNavigation().isDone(),
                "Living Ecology replaced Enderman teleport/melee movement with navigation");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void endermiteLifetimeStaysTransientAndNonTerritorial(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Endermite first = spawn(helper, EntityType.ENDERMITE, 4, 1, 4);
        Endermite second = spawn(helper, EntityType.ENDERMITE, 6, 1, 4);
        MobMindData.initialize(first, level);
        MobMindData.initialize(second, level);
        first.getNavigation().stop();
        second.getNavigation().stop();
        CompoundTag before = new CompoundTag();
        first.addAdditionalSaveData(before);

        EndCaveSpecialBehavior.tick(first, level);
        EndCaveSpecialBehavior.tick(second, level);
        CompoundTag after = new CompoundTag();
        first.addAdditionalSaveData(after);

        helper.assertTrue(before.getInt("Lifetime") == after.getInt("Lifetime"),
                "Living Ecology changed Endermite lifetime/despawn state");
        helper.assertTrue(TerritoryManager.ensureTerritory(first, level) == null
                        && MobMindData.territoryId(first) == 0L
                        && MobMindData.territoryId(second) == 0L,
                "Transient Endermites incorrectly formed a persistent community");
        helper.assertTrue(first.getNavigation().isDone() && second.getNavigation().isDone(),
                "Living Ecology issued nest patrol navigation to Endermites");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void silverfishNestSharesBoundedMemoryWithoutForcingTargets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Silverfish source = spawn(helper, EntityType.SILVERFISH, 4, 1, 4);
        Silverfish receiver = spawn(helper, EntityType.SILVERFISH, 6, 1, 4);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 9, 1, 4);
        for (Mob mob : new Mob[]{source, receiver, target}) {
            MobMindData.initialize(mob, level);
            mob.getNavigation().stop();
        }
        source.setTarget(target);

        EndCaveSpecialBehavior.tick(source, level);
        // A repeated GameTest run may find an already persisted Silverfish territory at this
        // template position. Tick the receiver too so it explicitly joins that existing nest.
        EndCaveSpecialBehavior.tick(receiver, level);
        TerritoryRecord territory = TerritoryManager.ensureTerritory(source, level);

        helper.assertTrue(territory != null
                        && MobMindData.territoryId(source) == MobMindData.territoryId(receiver),
                "Nearby Silverfish failed to form one persistent nest");
        helper.assertTrue(MobMindData.resolveThreat(receiver, level)
                        .filter(entity -> entity.getUUID().equals(target.getUUID())).isPresent(),
                "Silverfish nest did not receive local combat memory");
        helper.assertTrue(receiver.getTarget() == null,
                "Shared Silverfish nest memory forced a combat target");
        helper.assertTrue(source.getNavigation().isDone() && receiver.getNavigation().isDone(),
                "Living Ecology replaced Silverfish hide/wake-friends movement goals");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void shulkerAttachmentPeekVariantAndCombatRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Shulker shulker = spawn(helper, EntityType.SHULKER, 4, 1, 4);
        Zombie target = spawn(helper, EntityType.ZOMBIE, 9, 1, 4);
        MobMindData.initialize(shulker, level);
        MobMindData.initialize(target, level);
        CompoundTag seeded = new CompoundTag();
        shulker.addAdditionalSaveData(seeded);
        seeded.putByte("Peek", (byte) 60);
        shulker.readAdditionalSaveData(seeded);
        shulker.setVariant(Optional.of(DyeColor.PURPLE));
        shulker.setTarget(target);
        shulker.getNavigation().stop();
        Direction attachFace = shulker.getAttachFace();
        CompoundTag before = new CompoundTag();
        shulker.addAdditionalSaveData(before);

        EndCaveSpecialBehavior.tick(shulker, level);
        CompoundTag after = new CompoundTag();
        shulker.addAdditionalSaveData(after);

        helper.assertTrue(shulker.getAttachFace() == attachFace
                        && before.getByte("Peek") == after.getByte("Peek")
                        && before.getByte("Color") == after.getByte("Color")
                        && shulker.getVariant().equals(Optional.of(DyeColor.PURPLE)),
                "Living Ecology changed Shulker attachment, peek or variant state");
        helper.assertTrue(shulker.getTarget() == target && shulker.getNavigation().isDone(),
                "Living Ecology replaced Shulker ranged-combat or teleport movement state");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void creativeTargetAndMemoryAreSanitizedWithoutSpecialStateTakeover(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Shulker shulker = spawn(helper, EntityType.SHULKER, 4, 1, 4);
        MobMindData.initialize(shulker, level);
        shulker.setVariant(Optional.of(DyeColor.CYAN));
        ServerPlayer creative = FakePlayerFactory.getMinecraft(level);
        creative.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        creative.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 2, 4))));
        shulker.setTarget(creative);
        MobMindData.rememberThreat(shulker, creative, 60, level);
        Direction attachFace = shulker.getAttachFace();

        EndCaveSpecialBehavior.tick(shulker, level);

        helper.assertTrue(shulker.getTarget() == null
                        && MobMindData.resolveThreat(shulker, level).isEmpty(),
                "Special End/cave mob retained a Creative target or memory");
        helper.assertTrue(shulker.getAttachFace() == attachFace
                        && shulker.getVariant().equals(Optional.of(DyeColor.CYAN)),
                "Creative sanitization changed Shulker attachment or variant");
        creative.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void landAndStaticDomainGatesUseActualWorldBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos land = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos air = helper.absolutePos(new BlockPos(6, 3, 6));

        helper.assertTrue(BehaviorUtil.isValidForDomain(level, land, MovementDomain.LAND),
                "Known floor position failed LAND domain validation");
        helper.assertTrue(!BehaviorUtil.isValidForDomain(level, air, MovementDomain.LAND),
                "Unsupported open air passed LAND domain validation");
        helper.assertTrue(!BehaviorUtil.isValidForDomain(level, land, MovementDomain.STATIC)
                        && !BehaviorUtil.isValidForDomain(level, air, MovementDomain.STATIC),
                "STATIC domain unexpectedly produced an ecological navigation target");
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
