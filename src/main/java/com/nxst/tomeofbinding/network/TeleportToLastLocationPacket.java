package com.nxst.tomeofbinding.network;

import com.mojang.logging.LogUtils;
import com.nxst.tomeofbinding.Tome;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class TeleportToLastLocationPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final String playerName;
    private final String dimension;
    private final double x;
    private final double y;
    private final double z;

    public TeleportToLastLocationPacket(String playerName, String dimension, double x, double y, double z) {
        this.playerName = playerName;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(TeleportToLastLocationPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.playerName);
        buf.writeUtf(msg.dimension);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
    }

    public static TeleportToLastLocationPacket decode(FriendlyByteBuf buf) {
        return new TeleportToLastLocationPacket(
                buf.readUtf(),
                buf.readUtf(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );
    }

    public static void handle(TeleportToLastLocationPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                ResourceLocation dimensionLocation = ResourceLocation.tryParse(msg.dimension);
                if (dimensionLocation == null) {
                    LOGGER.warn("Failed to parse dimension {} for teleportation of {}", msg.dimension, sender.getName().getString());
                    return;
                }
                ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
                ServerLevel targetLevel = sender.getServer().getLevel(dimensionKey);
                if (targetLevel != null && targetLevel.dimension().location().equals(dimensionLocation)) {
                    sender.teleportTo(targetLevel, msg.x, msg.y, msg.z, sender.getYRot(), sender.getXRot());
                    LOGGER.info("Teleported {} to last location of {} at ({}, {}, {}) in {}",
                            sender.getName().getString(), msg.playerName, msg.x, msg.y, msg.z, msg.dimension);

                    targetLevel.playSound(null, msg.x, msg.y, msg.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                    targetLevel.sendParticles(ParticleTypes.PORTAL, msg.x, msg.y + targetLevel.random.nextDouble(), msg.z, 30, 0.5D, 0.5D, 0.5D, 0.2D);

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
                } else {
                    LOGGER.warn("Failed to teleport {} to dimension {}: invalid dimension",
                            sender.getName().getString(), msg.dimension);
                }
            }
        });
        context.setPacketHandled(true);
    }
}