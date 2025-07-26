package com.nxst.tomeofbinding.network.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PlayerData {
    private final String playerName;
    private final String playerDimension;
    private final int playerDistance;
    private final String playerUUID;
    private final boolean isOnline;
    private final double x, y, z;

    public PlayerData(String playerName, String playerDimension, int playerDistance, String playerUUID, boolean isOnline, double x, double y, double z) {
        this.playerName = playerName;
        this.playerDimension = playerDimension;
        this.playerDistance = playerDistance;
        this.playerUUID = playerUUID;
        this.isOnline = isOnline;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PlayerData(ServerPlayer player, ServerPlayer requestingPlayer) {
        this.playerName = player.getGameProfile().getName();
        this.playerUUID = player.getUUID().toString();
        ResourceLocation dimLocation = player.level().dimension().location();
        this.playerDimension = dimLocation.getPath();
        if (player.level().dimension() == requestingPlayer.level().dimension()) {
            this.playerDistance = (int) player.position().distanceTo(requestingPlayer.position());
        } else {
            this.playerDistance = -1;
        }
        this.isOnline = true;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }

    public PlayerData(String playerName, String playerUUID, String playerDimension) {
        this.playerName = playerName;
        this.playerDimension = playerDimension;
        this.playerDistance = -1;
        this.playerUUID = playerUUID;
        this.isOnline = false;
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
    }

    public static void encode(PlayerData data, FriendlyByteBuf buf) {
        buf.writeUtf(data.playerName);
        buf.writeUtf(data.playerDimension);
        buf.writeInt(data.playerDistance);
        buf.writeUtf(data.playerUUID);
        buf.writeBoolean(data.isOnline);
        buf.writeDouble(data.x);
        buf.writeDouble(data.y);
        buf.writeDouble(data.z);
    }

    public static PlayerData decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        String dimension = buf.readUtf();
        int distance = buf.readInt();
        String uuid = buf.readUtf();
        boolean isOnline = buf.readBoolean();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        return new PlayerData(name, dimension, distance, uuid, isOnline, x, y, z);
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerDimension() {
        return playerDimension;
    }

    public int getPlayerDistance() {
        return playerDistance;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
