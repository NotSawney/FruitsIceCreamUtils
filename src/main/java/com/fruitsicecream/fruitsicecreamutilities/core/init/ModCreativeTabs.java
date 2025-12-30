package com.fruitsicecream.fruitsicecreamutilities.core.init;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FruitsIceCreamUtilities.MOD_ID);

    public static final RegistryObject<CreativeModeTab> FRUITSICECREAM_TAB = CREATIVE_MODE_TABS.register("fruitsicecream_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.EXPERIENCE_CORE_MK5.get()))
                    .title(Component.translatable("creativetab.fruitsicecreamutilities"))
                    .displayItems((parameters, output) -> {
                        // Experience Cores
                        output.accept(ModItems.EXPERIENCE_CORE_MK1.get());
                        output.accept(ModItems.EXPERIENCE_CORE_MK2.get());
                        output.accept(ModItems.EXPERIENCE_CORE_MK3.get());
                        output.accept(ModItems.EXPERIENCE_CORE_MK4.get());
                        output.accept(ModItems.EXPERIENCE_CORE_MK5.get());

                        // Experience Collectors
                        output.accept(ModItems.BASIC_EXPERIENCE_COLLECTOR.get());
                        output.accept(ModItems.ADVANCED_EXPERIENCE_COLLECTOR.get());

                        // Debug Tools
                        output.accept(ModItems.DEBUG_STICK.get());
                        output.accept(ModItems.EXP_ADDITIONER.get());
                        output.accept(ModItems.EXP_TRANSFERER.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}