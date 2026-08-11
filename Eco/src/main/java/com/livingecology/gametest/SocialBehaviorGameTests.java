package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.SocialAlarmBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesProfile;
import com.livingecology.data.SpeciesType;
import com.livingecology.data.StateType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Focused GameTests for local social information transfer. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class SocialBehaviorGameTests {
    private SocialBehaviorGameTests() {}

    @GameTest(template = "test_arena")
    public static void passiveHerdSharesThreatThroughVisibleGroupmate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Sheep observer = spawn(helper, EntityType.SHEEP, 4, 1, 4);
        Sheep receiver = spawn(helper, EntityType.SHEEP, 5, 1, 4);
        Zombie threat = spawn(helper, EntityType.ZOMBIE, 8, 1, 4);
        MobMindData.initialize(observer, level);
        MobMindData.initialize(receiver, level);
        MobMindData.initialize(threat, level);

        MobMindData.rememberThreat(observer, threat, 60, level);
        helper.assertTrue(MobMindData.resolveThreat(receiver, level).isEmpty(),
                "Receiver started with threat knowledge before social sharing");

        SocialAlarmBehavior.tick(receiver, level, SpeciesProfile.of(SpeciesType.SHEEP));

        helper.assertTrue(MobMindData.resolveThreat(receiver, level)
                        .filter(entity -> entity.getUUID().equals(threat.getUUID())).isPresent(),
                "Visible herd mate did not transmit a nearby remembered threat");
        helper.assertTrue(MobMindData.getState(receiver, StateType.FEAR) >= 1,
                "Receiver learned the threat without entering an alert/fear state");
        helper.succeed();
    }

    private static <T extends Mob> T spawn(GameTestHelper helper, EntityType<T> type, int x, int y, int z) {
        T mob = type.create(helper.getLevel());
        if (mob == null) throw new IllegalStateException("Could not create " + BuiltInRegistries.ENTITY_TYPE.getKey(type));
        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }
}
