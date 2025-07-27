package com.nxst.tomeofbinding.network.data;

import com.mojang.logging.LogUtils;
import com.nxst.tomeofbinding.Tome;
import com.nxst.tomeofbinding.config.ModConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.Arrays;

public class PlayerListRequestMessage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final boolean includeOffline;

    public PlayerListRequestMessage(boolean includeOffline) {
        this.includeOffline = includeOffline;
    }

    public static void encode(PlayerListRequestMessage msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.includeOffline);
    }

    public static PlayerListRequestMessage decode(FriendlyByteBuf buf) {
        return new PlayerListRequestMessage(buf.readBoolean());
    }

    public static void handle(PlayerListRequestMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            List<PlayerData> playerList = new ArrayList<>();
            List<ServerPlayer> serverPlayers = sender.getServer().getPlayerList().getPlayers();
            for (ServerPlayer player : serverPlayers) {
                if (!player.getUUID().equals(sender.getUUID())) {
                    playerList.add(new PlayerData(
                            player.getName().getString(),
                            player.level().dimension().location().toString(),
                            (int) player.distanceToSqr(sender),
                            player.getStringUUID(),
                            true,
                            player.getX(),
                            player.getY(),
                            player.getZ()
                    ));
                }
            }
            
            Tome.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sender), new PlayerListResponseMessage(playerList));
        });
        context.setPacketHandled(true);
    }
}