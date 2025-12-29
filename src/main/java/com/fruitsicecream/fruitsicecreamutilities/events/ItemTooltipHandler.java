package com.fruitsicecream.fruitsicecreamutilities.events;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import com.fruitsicecream.fruitsicecreamutilities.core.init.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FruitsIceCreamUtilities.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ItemTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // Verificar si es un Experience Core
        if (stack.is(ModBlocks.EXPERIENCE_CORE_MK1.get().asItem()) ||
                stack.is(ModBlocks.EXPERIENCE_CORE_MK2.get().asItem()) ||
                stack.is(ModBlocks.EXPERIENCE_CORE_MK3.get().asItem()) ||
                stack.is(ModBlocks.EXPERIENCE_CORE_MK4.get().asItem()) ||
                stack.is(ModBlocks.EXPERIENCE_CORE_MK5.get().asItem())) {

            // Verificar si tiene XP almacenada
            if (stack.hasTag() && stack.getTag().contains("StoredExperience")) {
                int storedXP = stack.getTag().getInt("StoredExperience");

                if (storedXP > 0) {
                    event.getToolTip().add(
                            Component.translatable("tooltip.fruitsicecreamutilities.stored_experience", storedXP)
                                    .withStyle(ChatFormatting.AQUA)
                    );
                }
            }
        }
    }
}