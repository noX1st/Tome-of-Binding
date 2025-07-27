package com.nxst.tomeofbinding.network;

import com.nxst.tomeofbinding.config.ModConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RespondWorldIdPacket {

    private final String worldId;

    public RespondWorldIdPacket(String worldId) {
        this.worldId = worldId;
    }

    public static void encode(RespondWorldIdPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.worldId);
    }

    public static RespondWorldIdPacket decode(FriendlyByteBuf buf) {
        return new RespondWorldIdPacket(buf.readUtf());
    }

    public static void handle(RespondWorldIdPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ModConfig.setRemoteWorldId(msg.worldId));
        });
        context.setPacketHandled(true);
    }
}