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

            if (msg.includeOffline) {
                String worldId = sender.getServer().getWorldPath(LevelResource.ROOT).getFileName().toString();
                for (String entry : ModConfig.getPlayerHistory(worldId)) {
                    try {
                        String[] allParts = entry.split(":");
                        if (allParts.length >= 6) {
                            String uuid = allParts[0];
                            if (serverPlayers.stream().noneMatch(p -> p.getStringUUID().equals(uuid))) {
                                String name = allParts[1];
                                String xStr = allParts[allParts.length - 3];
                                String yStr = allParts[allParts.length - 2];
                                String zStr = allParts[allParts.length - 1];
                                String dimension = String.join(":", Arrays.copyOfRange(allParts, 2, allParts.length - 3));

                                double x = Double.parseDouble(xStr);
                                double y = Double.parseDouble(yStr);
                                double z = Double.parseDouble(zStr);
                                playerList.add(new PlayerData(name, dimension, -1, uuid, false, x, y, z));
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse player history entry: {}", entry, e);
                    }
                }
            }

            Tome.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sender), new PlayerListResponseMessage(playerList));
        });
        context.setPacketHandled(true);
    }
}