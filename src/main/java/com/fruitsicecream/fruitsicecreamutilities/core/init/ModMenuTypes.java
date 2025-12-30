package com.fruitsicecream.fruitsicecreamutilities.core.init;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.menu.ExperienceCollectorMenu;
import com.fruitsicecream.fruitsicecreamutilities.features.experience.menu.ExperienceCoreMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, FruitsIceCreamUtilities.MOD_ID);

    public static final RegistryObject<MenuType<ExperienceCoreMenu>> EXPERIENCE_CORE_MENU =
            MENU_TYPES.register("experience_core_menu",
                    () -> IForgeMenuType.create(ExperienceCoreMenu::new));

    public static final RegistryObject<MenuType<ExperienceCollectorMenu>> EXPERIENCE_COLLECTOR_MENU =
            MENU_TYPES.register("experience_collector_menu",
                    () -> IForgeMenuType.create(ExperienceCollectorMenu::new));

    public static void register(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }
}