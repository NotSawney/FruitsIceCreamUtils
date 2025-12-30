package com.fruitsicecream.fruitsicecreamutilities.network.packets;

import com.fruitsicecream.fruitsicecreamutilities.features.experience.blockEntity.ExperienceCollectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CollectCollectorExperiencePacket {
    private final BlockPos pos;

    public CollectCollectorExperiencePacket(BlockPos pos) {
        this.pos = pos;
    }

    public CollectCollectorExperiencePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.level() != null) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof ExperienceCollectorBlockEntity collectorEntity) {
                    collectorEntity.collectExperience(player);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}