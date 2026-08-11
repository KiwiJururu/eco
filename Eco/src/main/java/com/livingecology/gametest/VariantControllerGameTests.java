package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.CowBehavior;
import com.livingecology.data.MobMindData;
import com.livingecology.data.SpeciesType;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.territory.TerritoryRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Tests that vanilla subclasses routed to specialized controllers retain their own ecology identity. */
@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class VariantControllerGameTests {
    private VariantControllerGameTests() {}

    @GameTest(template = "test_arena")
    public static void mooshroomUsesBovineControllerWithoutBecomingCowTerritory(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Cow mooshroom = spawn(helper, EntityType.MOOSHROOM, 5, 1, 5);
        MobMindData.initialize(mooshroom, level);

        CowBehavior.tick(mooshroom, level);

        TerritoryRecord territory = TerritoryManager.ensureTerritory(mooshroom, level);
        helper.assertTrue(territory != null, "Mooshroom bovine controller failed to establish its home range");
        helper.assertTrue(territory.species() == SpeciesType.MOOSHROOM,
                "Mooshroom specialized behavior collapsed its ecology identity into Cow");
        helper.assertTrue(MobMindData.territoryId(mooshroom) == territory.id(),
                "Mooshroom did not retain the territory produced by its specialized controller");
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
