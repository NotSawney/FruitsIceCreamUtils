package com.fruitsicecream.fruitsicecreamutilities.events;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import com.fruitsicecream.fruitsicecreamutilities.util.SaveStateTracker;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FruitsIceCreamUtilities.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SaveEventHandler {

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        // Marcar que estamos guardando
        SaveStateTracker.markSaving();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        // Reset cuando se descarga el mundo
        SaveStateTracker.markSaved();
    }
}