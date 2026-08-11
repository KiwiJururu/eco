package com.livingecology;

import com.livingecology.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LivingEcology.MODID)
public final class LivingEcology {
    public static final String MODID = "livingecology";
    public static final String VERSION = "0.0.1";

    public LivingEcology() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        // Common Forge events are discovered through @Mod.EventBusSubscriber.
        // The prototype intentionally has no external library, mixin or access transformer.
    }
}
