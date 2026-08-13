package com.livingecology.gametest;

import com.livingecology.LivingEcology;
import com.livingecology.ai.IllagerSocietyBehavior;
import com.livingecology.data.MobMindData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(LivingEcology.MODID)
@PrefixGameTestTemplate(false)
public final class IllagerNavigationDiagnosticGameTest {
    private IllagerNavigationDiagnosticGameTest() {}

    @GameTest(template = "test_arena")
    public static void pillagerNavigationPreconditionAndOverlay(GameTestHelper helper) {
        Pillager pillager = EntityType.PILLAGER.create(helper.getLevel());
        Villager villager = EntityType.VILLAGER.create(helper.getLevel());
        if (pillager == null || villager == null) throw new IllegalStateException("Could not create diagnostic actors");
        BlockPos p = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos v = helper.absolutePos(new BlockPos(9, 1, 4));
        pillager.moveTo(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 0, 0);
        villager.moveTo(v.getX() + 0.5D, v.getY() + 0.5D, v.getZ() + 0.5D, 0, 0);
        helper.getLevel().addFreshEntity(pillager);
        helper.getLevel().addFreshEntity(villager);
        MobMindData.initialize(pillager, helper.getLevel());
        pillager.setTarget(villager);
        PathNavigation nav = pillager.getNavigation();
        BlockPos openDestination = helper.absolutePos(new BlockPos(8, 1, 7));
        Path path = nav.createPath(openDestination, 0);
        boolean ordered = path != null && nav.moveTo(path, 0.6D);
        helper.assertTrue(ordered && nav.getPath() != null && !nav.isDone(),
                "DIAGNOSTIC: Pillager navigation fixture is already inactive before overlay");
        IllagerSocietyBehavior.tick(pillager, helper.getLevel());
        helper.assertTrue(nav.getPath() != null && !nav.isDone(),
                "DIAGNOSTIC: overlay stopped a confirmed-active Pillager path");
        helper.succeed();
    }
}
