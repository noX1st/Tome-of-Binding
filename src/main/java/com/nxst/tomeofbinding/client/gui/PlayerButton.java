package com.nxst.tomeofbinding.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import com.nxst.tomeofbinding.config.ModConfig;
import java.util.UUID;

public class PlayerButton extends Button {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final String playerName;
    private final String playerDimension;
    private final int playerDistance;
    private final String playerUUID;
    private final boolean isOnline;
    private final boolean isDarkTheme;
    private final ResourceLocation darkButtonTexture;
    private final ResourceLocation lightButtonTexture;
    private final double playerX, playerY, playerZ;

    private static final int NORMAL_V_COORD = 46;
    private static final int HOVER_V_COORD = 66;

    public PlayerButton(int buttonX, int buttonY, int width, int height, String playerName, String playerDimension, int playerDistance, String playerUUID, boolean isOnline, double playerX, double playerY, double playerZ, OnPress onPress, boolean isDarkTheme, ResourceLocation darkTexture, ResourceLocation lightTexture) {
        super(buttonX, buttonY, width, height, Component.literal(""), onPress, DEFAULT_NARRATION);
        this.playerName = playerName;
        this.playerDimension = playerDimension;
        this.playerDistance = playerDistance;
        this.playerUUID = playerUUID;
        this.isOnline = isOnline;
        this.isDarkTheme = isDarkTheme;
        this.darkButtonTexture = darkTexture;
        this.lightButtonTexture = lightTexture;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
    }

    public String getPlayerUUID() {
        return this.playerUUID;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        ResourceLocation currentButtonTexture = isDarkTheme ? darkButtonTexture : lightButtonTexture;

        int backgroundV = this.isHoveredOrFocused() ? HOVER_V_COORD : NORMAL_V_COORD;
        graphics.blit(currentButtonTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0, backgroundV, 200, 20, 256, 256);

        final boolean isPlayerButton = playerUUID != null && !playerUUID.isEmpty();
        if (isPlayerButton) {
            try {
                UUID uuid = UUID.fromString(playerUUID);
                PlayerInfo playerInfo = minecraft.getConnection().getPlayerInfo(uuid);
                ResourceLocation skin;
                if (playerInfo != null) {
                    skin = playerInfo.getSkinLocation();
                } else {
                    GameProfile profile = new GameProfile(uuid, playerName);
                    skin = minecraft.getSkinManager().getInsecureSkinLocation(profile);
                }

                int headX = getX() + 7;
                int headY = getY() + (this.getHeight() - 16) / 2;
                graphics.blit(skin, headX, headY, 16, 16, 8, 8, 8, 8, 64, 64);
                graphics.blit(skin, headX, headY, 16, 16, 40, 8, 8, 8, 64, 64);

            } catch (Exception e) {
                LOGGER.error("Error rendering player skin for UUID: {}", playerUUID, e);
            }

            int nameColor = 0xFFFFFFFF;
            String nameText = playerName;
            if (!isOnline) {
                nameText += " (Offline)";
            }

            boolean showCoordinates = ModConfig.hasShowCoordinatesForPlayer(playerUUID) ? ModConfig.isShowCoordinatesForPlayer(playerUUID) : ModConfig.isShowPlayerCoordinates();
            boolean showDimension = ModConfig.isShowDimensionForPlayer(playerUUID) && playerDimension != null && !playerDimension.isEmpty();

            int lines = 1 + (showCoordinates ? 1 : 0) + (showDimension ? 1 : 0);
            int totalTextHeight = lines * font.lineHeight + (lines - 1) * 2;
            int nextY = getY() + (this.getHeight() - totalTextHeight) / 2;

            graphics.drawString(font, nameText, getX() + 30, nextY, nameColor, false);
            nextY += font.lineHeight + 2;

            if (showCoordinates) {
                String coordsText = String.format("X: %d, Y: %d, Z: %d", (int) playerX, (int) playerY, (int) playerZ);
                graphics.drawString(font, coordsText, getX() + 30, nextY, nameColor, false);
                nextY += font.lineHeight + 2;
            }

            if (showDimension) {
                int dimensionColor;
                if (!isDarkTheme) {
                    dimensionColor = 0xFFFFFFFF;
                } else {
                    if (!isOnline) {
                        dimensionColor = 0xFFCCCCCC;
                    } else {
                        String[] dimParts = playerDimension.split(":");
                        String dimPath = dimParts.length > 1 ? dimParts[1] : playerDimension;
                        switch (dimPath) {
                            case "overworld": dimensionColor = 0xFF55FF55; break;
                            case "the_nether": dimensionColor = 0xFFFF5555; break;
                            case "the_end": dimensionColor = 0xFFFF55FF; break;
                            default: dimensionColor = 0xA0A0A0; break;
                        }
                    }
                }
                String translationKey = "dimension." + playerDimension.replace(':', '.');
                Component dimensionComponent = Component.translatable(translationKey);
                graphics.drawString(font, dimensionComponent, getX() + 30, nextY, dimensionColor, false);
            }
        } else {
            int textColor = 0xFFFFFFFF;
            Component textToDraw = Component.literal(this.playerName);
            int textWidth = font.width(textToDraw);
            int textX = getX() + (this.getWidth() - textWidth) / 2;
            int textY = getY() + (this.getHeight() - font.lineHeight) / 2;
            graphics.drawString(font, textToDraw, textX, textY, textColor, false);
        }
    }
}