package com.nxst.tomeofbinding.item;

import com.nxst.tomeofbinding.client.gui.PlayerSelectScreen;
import com.nxst.tomeofbinding.config.ModConfig;
import com.nxst.tomeofbinding.network.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class TomeItem extends Item {

    public TomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 1;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(itemStack);
        }

        if (level.isClientSide) {
            List<PlayerData> initialPlayerList = new ArrayList<>();
            Set<String> history = ModConfig.getPlayerHistoryForCurrentWorld();
            for (String entry : history) {
                String[] allParts = entry.split("(?<!\\\\):");
                if (allParts.length >= 6) {
                    String uuid = allParts[0].replace("\\:", ":");
                    String name = allParts[1].replace("\\:", ":");
                    String xStr = allParts[allParts.length - 3].replace("\\:", ":");
                    String yStr = allParts[allParts.length - 2].replace("\\:", ":");
                    String zStr = allParts[allParts.length - 1].replace("\\:", ":");
                    String dimension = String.join(":", Arrays.copyOfRange(allParts, 2, allParts.length - 3)).replace("\\:", ":");

                    double x = parseDoubleSafe(xStr.replace(",", "."), 0.0);
                    double y = parseDoubleSafe(yStr.replace(",", "."), 0.0);
                    double z = parseDoubleSafe(zStr.replace(",", "."), 0.0);

                    initialPlayerList.add(new PlayerData(name, dimension, -1, uuid, false, x, y, z));
                }
            }
            Minecraft.getInstance().setScreen(new PlayerSelectScreen(initialPlayerList));
        }

        return InteractionResultHolder.consume(itemStack);
    }

    private double parseDoubleSafe(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}