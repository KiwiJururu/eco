package com.livingecology.registry;

import com.livingecology.LivingEcology;
import com.livingecology.item.DebugAnalyzerItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LivingEcology.MODID);

    public static final RegistryObject<Item> DEBUG_ANALYZER = ITEMS.register("debug_analyzer",
            () -> new DebugAnalyzerItem(new Item.Properties().stacksTo(1)));
}
