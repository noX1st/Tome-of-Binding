package com.nxst.tomeofbinding.network.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.nxst.tomeofbinding.client.gui.PlayerSelectScreen;

public class PlayerListResponseMessage {
    private final List<PlayerData> players;

    public PlayerListResponseMessage(List<PlayerData> players) {
        this.players = players;
    }

    public static void encode(PlayerListResponseMessage msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.players.size());
        for (PlayerData playerData : msg.players) {
            PlayerData.encode(playerData, buf);
        }
    }

    public static PlayerListResponseMessage decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<PlayerData> players = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            players.add(PlayerData.decode(buf));
        }
        return new PlayerListResponseMessage(players);
    }

    public static void handle(PlayerListResponseMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof PlayerSelectScreen) {
                ((PlayerSelectScreen) mc.screen).updatePlayerData(msg.players);
            }
        });
        context.setPacketHandled(true);
    }
}