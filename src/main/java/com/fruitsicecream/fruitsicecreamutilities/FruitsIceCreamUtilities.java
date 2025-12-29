package com.fruitsicecream.fruitsicecreamutilities;

import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlocks;
import com.fruitsicecream.fruitsicecreamutilities.core.init.ModCreativeTabs;
import com.fruitsicecream.fruitsicecreamutilities.core.init.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(FruitsIceCreamUtilities.MOD_ID)
public class FruitsIceCreamUtilities {
    public static final String MOD_ID = "fruitsicecreamutilities";
    private static final Logger LOGGER = LogUtils.getLogger();

    public FruitsIceCreamUtilities() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Registrar todo lo del mod
        ModCreativeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);

        // Eventos del ciclo de vida
        modEventBus.addListener(this::commonSetup);

        // Registrar en el event bus de Forge
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("FruitsIceCream Utilities cargando...");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("FruitsIceCream Utilities - Common Setup completado!");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("FruitsIceCream Utilities activo en el servidor!");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("FruitsIceCream Utilities - Cliente configurado!");
        }
    }
}