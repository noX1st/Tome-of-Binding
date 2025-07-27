package com.nxst.tomeofbinding.network;

import com.nxst.tomeofbinding.Tome;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerSelectPacket {
    private final String targetPlayerName;

    public PlayerSelectPacket(String targetPlayerName) {
        this.targetPlayerName = targetPlayerName;
    }

    public static void encode(PlayerSelectPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.targetPlayerName);
    }

    public static PlayerSelectPacket decode(FriendlyByteBuf buf) {
        return new PlayerSelectPacket(buf.readUtf());
    }

    public static void handle(PlayerSelectPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            MinecraftServer server = sender.getServer();
            if (server == null) {
                return;
            }

            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(msg.targetPlayerName);

            if (targetPlayer != null) {
                sender.teleportTo(
                        targetPlayer.serverLevel(),
                        targetPlayer.getX(),
                        targetPlayer.getY(),
                        targetPlayer.getZ(),
                        targetPlayer.getYRot(),
                        targetPlayer.getXRot()
                );

                ServerLevel world = sender.serverLevel();
                double x = sender.getX();
                double y = sender.getY();
                double z = sender.getZ();

                world.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                world.sendParticles(ParticleTypes.PORTAL, x, y + world.random.nextDouble(), z, 30, (sender.getBbWidth() / 2.0D), 0.5D, (sender.getBbWidth() / 2.0D), 0.2D);

                ItemStack mainHandItem = sender.getMainHandItem();
                ItemStack offHandItem = sender.getOffhandItem();

                Item itemInHand = null;
                if (mainHandItem.getItem() == Tome.TOME.get() || mainHandItem.getItem() == Tome.SCROLL_OF_VISION.get()) {
                    itemInHand = mainHandItem.getItem();
                } else if (offHandItem.getItem() == Tome.TOME.get() || offHandItem.getItem() == Tome.SCROLL_OF_VISION.get()) {
                    itemInHand = offHandItem.getItem();
                }

                if (itemInHand == Tome.SCROLL_OF_VISION.get()) {
                    sender.getCooldowns().addCooldown(itemInHand, 300);
                    if (mainHandItem.getItem() == itemInHand) mainHandItem.shrink(1);
                    else offHandItem.shrink(1);
                } else if (itemInHand == Tome.TOME.get()) {
                    sender.getCooldowns().addCooldown(itemInHand, 100);
                }
            }
        });
        context.setPacketHandled(true);
    }
}