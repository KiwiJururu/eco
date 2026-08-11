package com.livingecology.registry;

import com.livingecology.LivingEcology;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LivingEcology.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(ModItems.DEBUG_ANALYZER);
        }
    }
}
