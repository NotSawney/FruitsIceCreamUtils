package com.fruitsicecream.fruitsicecreamutilities.core.init;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import com.fruitsicecream.fruitsicecreamutilities.features.debug.items.DebugStickItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FruitsIceCreamUtilities.MOD_ID);

    // BlockItems para los Experience Cores
    public static final RegistryObject<Item> EXPERIENCE_CORE_MK1 = ITEMS.register("exp_core_mk1",
            () -> new BlockItem(ModBlocks.EXPERIENCE_CORE_MK1.get(), new Item.Properties()));

    public static final RegistryObject<Item> EXPERIENCE_CORE_MK2 = ITEMS.register("exp_core_mk2",
            () -> new BlockItem(ModBlocks.EXPERIENCE_CORE_MK2.get(), new Item.Properties()));

    public static final RegistryObject<Item> EXPERIENCE_CORE_MK3 = ITEMS.register("exp_core_mk3",
            () -> new BlockItem(ModBlocks.EXPERIENCE_CORE_MK3.get(), new Item.Properties()));

    public static final RegistryObject<Item> EXPERIENCE_CORE_MK4 = ITEMS.register("exp_core_mk4",
            () -> new BlockItem(ModBlocks.EXPERIENCE_CORE_MK4.get(), new Item.Properties()));

    public static final RegistryObject<Item> EXPERIENCE_CORE_MK5 = ITEMS.register("exp_core_mk5",
            () -> new BlockItem(ModBlocks.EXPERIENCE_CORE_MK5.get(), new Item.Properties()));

    // Debug Stick
    public static final RegistryObject<Item> DEBUG_STICK = ITEMS.register("debug_stick",
            () -> new DebugStickItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}