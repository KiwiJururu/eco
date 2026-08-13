package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.GuardianSocietyBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.RelationKind;
import com.livingecology.territory.RelationService;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import com.livingecology.territory.TerritorySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Objective coverage for issue #17. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class GuardianSocietyEcologyGameTests {
    private GuardianSocietyEcologyGameTests() {}

    @GameTest(template = "test_arena")
    public static void guardianCombatTargetRemainsVanillaOwnedWithoutEcologicalPath(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Guardian guardian = spawn(helper, EntityType.GUARDIAN, 4, 1, 4);
        Squid target = spawn(helper, EntityType.SQUID, 8, 1, 4);
        MobMindData.initialize(guardian, level);
        guardian.setTarget(target);
        guardian.getNavigation().stop();
        GuardianSocietyBehavior.tick(guardian, level);
        helper.assertTrue(guardian.getTarget() == target,
                "Living Ecology changed a legal Guardian beam-combat target");
        helper.assertTrue(guardian.getNavigation().isDone(),
                "Guardian ecology issued navigation while vanilla combat owned movement");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void elderCombatAndRawAttributesRemainVanillaOwned(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ElderGuardian elder = spawn(helper, EntityType.ELDER_GUARDIAN, 4, 1, 4);
        Squid target = spawn(helper, EntityType.SQUID, 8, 1, 4);
        MobMindData.initialize(elder, level);
        double health = elder.getAttributeValue(Attributes.MAX_HEALTH);
        double damage = elder.getAttributeValue(Attributes.ATTACK_DAMAGE);
        elder.setTarget(target);
        elder.getNavigation().stop();
        GuardianSocietyBehavior.tick(elder, level);
        helper.assertTrue(elder.getTarget() == target && elder.getNavigation().isDone(),
                "Elder ecology changed combat target or issued combat navigation");
        helper.assertTrue(elder.getAttributeValue(Attributes.MAX_HEALTH) == health
                        && elder.getAttributeValue(Attributes.ATTACK_DAMAGE) == damage,
                "Elder influence used raw health/damage inflation");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void guardianAndElderKeepSpeciesScopedTerritoriesWithStrongAffinity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Guardian guardian = spawn(helper, EntityType.GUARDIAN, 3, 1, 3);
        ElderGuardian elder = spawn(helper, EntityType.ELDER_GUARDIAN, 7, 1, 3);
        MobMindData.initialize(guardian, level);
        MobMindData.initialize(elder, level);
        TerritoryRecord guardianTerritory = TerritoryManager.ensureTerritory(guardian, level);
        TerritoryRecord elderTerritory = TerritoryManager.ensureTerritory(elder, level);
        helper.assertTrue(guardianTerritory != null && elderTerritory != null,
                "Guardian monument territories failed to form");
        helper.assertTrue(guardianTerritory.id() != elderTerritory.id()
                        && guardianTerritory.species() == SpeciesType.GUARDIAN
                        && elderTerritory.species() == SpeciesType.ELDER_GUARDIAN,
                "Guardian and Elder identity was collapsed into one territory record");
        helper.assertTrue(RelationService.natural(SpeciesType.GUARDIAN, SpeciesType.ELDER_GUARDIAN)
                        .kind() == RelationKind.SYMBIOTIC,
                "Guardian/Elder relation lost monument affinity");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void normalGuardianMemoryIsBoundedToEightReceivers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Guardian source = spawn(helper, EntityType.GUARDIAN, 4, 1, 4);
        Squid threat = spawn(helper, EntityType.SQUID, 9, 1, 4);
        MobMindData.initialize(source, level);
        source.setLastHurtByMob(threat);
        List<Guardian> receivers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Guardian receiver = spawn(helper, EntityType.GUARDIAN, 5, 1, 5);
            MobMindData.initialize(receiver, level);
            receiver.getNavigation().stop();
            receivers.add(receiver);
        }
        GuardianSocietyBehavior.tick(source, level);
        long informed = receivers.stream().filter(receiver -> MobMindData.resolveThreat(receiver, level)
                .filter(entity -> entity == threat).isPresent()).count();
        helper.assertTrue(informed == 8, "Expected exactly eight Guardian memory receivers, got " + informed);
        helper.assertTrue(receivers.stream().allMatch(r -> r.getTarget() == null && r.getNavigation().isDone()),
                "Guardian memory assigned targets or paths");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void elderInfluenceIsLocallyBoundedToTwelveWithoutStatBuffs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ElderGuardian elder = spawn(helper, EntityType.ELDER_GUARDIAN, 4, 1, 4);
        Squid threat = spawn(helper, EntityType.SQUID, 9, 1, 4);
        MobMindData.initialize(elder, level);
        elder.setLastHurtByMob(threat);
        List<Guardian> receivers = new ArrayList<>();
        List<Double> healthBefore = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            Guardian receiver = spawn(helper, EntityType.GUARDIAN, 5, 1, 5);
            MobMindData.initialize(receiver, level);
            receivers.add(receiver);
            healthBefore.add(receiver.getAttributeValue(Attributes.MAX_HEALTH));
        }
        GuardianSocietyBehavior.tick(elder, level);
        long informed = receivers.stream().filter(receiver -> MobMindData.resolveThreat(receiver, level)
                .filter(entity -> entity == threat).isPresent()).count();
        helper.assertTrue(informed == 12, "Expected exactly twelve Elder-influenced receivers, got " + informed);
        for (int i = 0; i < receivers.size(); i++) {
            helper.assertTrue(receivers.get(i).getAttributeValue(Attributes.MAX_HEALTH) == healthBefore.get(i),
                    "Elder local influence changed raw Guardian health");
        }
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void dryIdleGuardianNeverReceivesEcologicalNavigation(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Guardian guardian = spawn(helper, EntityType.GUARDIAN, 4, 1, 4);
        MobMindData.initialize(guardian, level);
        guardian.getNavigation().stop();
        GuardianSocietyBehavior.tick(guardian, level);
        helper.assertTrue(guardian.getNavigation().isDone(),
                "Guardian ecology issued a land/path order outside water");
        helper.succeed();
    }

    @GameTest(template = "test_arena")
    public static void creativeTargetAndMemoryAreSanitizedWithoutSwimOrder(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Guardian guardian = spawn(helper, EntityType.GUARDIAN, 4, 1, 4);
        MobMindData.initialize(guardian, level);
        ServerPlayer creative = FakePlayerFactory.getMinecraft(level);
        creative.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        creative.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(7, 1, 4))));
        guardian.setTarget(creative);
        MobMindData.rememberThreat(guardian, creative, 60, level);
        guardian.getNavigation().stop();
        GuardianSocietyBehavior.tick(guardian, level);
        helper.assertTrue(guardian.getTarget() == null && MobMindData.resolveThreat(guardian, level).isEmpty(),
                "Guardian retained Creative target or threat memory");
        helper.assertTrue(guardian.getNavigation().isDone(),
                "Creative sanitization issued Guardian navigation");
        creative.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
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
