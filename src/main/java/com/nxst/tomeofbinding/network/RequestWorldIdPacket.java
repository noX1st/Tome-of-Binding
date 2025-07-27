package com.nxst.tomeofbinding.network;

import com.mojang.logging.LogUtils;
import com.nxst.tomeofbinding.Tome;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.function.Supplier;

public class RequestWorldIdPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    public RequestWorldIdPacket() {
    }

    public static void encode(RequestWorldIdPacket msg, FriendlyByteBuf buf) {
    }

    public static RequestWorldIdPacket decode(FriendlyByteBuf buf) {
        return new RequestWorldIdPacket();
    }

    public static void handle(RequestWorldIdPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.getServer() == null) {
                return;
            }

            String worldId = "default_world_fallback";
            MinecraftServer server = sender.getServer();

            try {
                Field storageSourceField = MinecraftServer.class.getDeclaredField("storageSource");
                storageSourceField.setAccessible(true);
                LevelStorageSource.LevelStorageAccess storageAccess = (LevelStorageSource.LevelStorageAccess) storageSourceField.get(server);
                worldId = storageAccess.getLevelId();
            } catch (Exception e) {
                LOGGER.error("Tome of Binding: Failed to get worldId via reflection. Falling back.", e);
                try {
                    worldId = server.getWorldPath(LevelResource.ROOT).getFileName().toString();
                } catch (Exception e2) {
                    LOGGER.error("Tome of Binding: Failed to get worldId via getWorldPath. Falling back to level name.", e2);
                    worldId = server.getWorldData().getLevelName();
                }
            }

            if (worldId == null || worldId.trim().isEmpty() || worldId.equals(".") || worldId.equals("..")) {
                LOGGER.error("Tome of Binding: Generated an invalid worldId ('{}'). Falling back to level name to prevent client issues.", worldId);
                worldId = server.getWorldData().getLevelName();
            }

            Tome.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sender), new RespondWorldIdPacket(worldId));
        });
        context.setPacketHandled(true);
    }
}