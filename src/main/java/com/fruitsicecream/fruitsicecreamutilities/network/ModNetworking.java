package com.fruitsicecream.fruitsicecreamutilities.network;

import com.fruitsicecream.fruitsicecreamutilities.FruitsIceCreamUtilities;
import com.fruitsicecream.fruitsicecreamutilities.network.packets.CollectCollectorExperiencePacket;
import com.fruitsicecream.fruitsicecreamutilities.network.packets.CollectExperiencePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FruitsIceCreamUtilities.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.messageBuilder(CollectExperiencePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CollectExperiencePacket::new)
                .encoder(CollectExperiencePacket::encode)
                .consumerMainThread(CollectExperiencePacket::handle)
                .add();

        INSTANCE.messageBuilder(CollectCollectorExperiencePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CollectCollectorExperiencePacket::new)
                .encoder(CollectCollectorExperiencePacket::encode)
                .consumerMainThread(CollectCollectorExperiencePacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}