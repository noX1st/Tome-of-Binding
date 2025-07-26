package com.nxst.tomeofbinding.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ThemedButton extends Button {

    private final ResourceLocation darkTexture;
    private final ResourceLocation lightTexture;
    private final boolean isDarkTheme;
    private final boolean isControlButton;
    private final boolean isChecked;

    private static final int NORMAL_V_COORD = 46;
    private static final int HOVER_V_COORD = 66;

    private static final int CTRL_NORMAL_V = 106;
    private static final int CTRL_HOVER_V = 126;
    private static final int CTRL_CHECKED_V = 146;

    public ThemedButton(int x, int y, int width, int height, Component message, OnPress onPress, ResourceLocation darkTexture, ResourceLocation lightTexture, boolean isDarkTheme, boolean isControlButton, boolean isChecked) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.darkTexture = darkTexture;
        this.lightTexture = lightTexture;
        this.isDarkTheme = isDarkTheme;
        this.isControlButton = isControlButton;
        this.isChecked = isChecked;
    }

    public ThemedButton(int x, int y, int width, int height, Component message, OnPress onPress, ResourceLocation darkTexture, ResourceLocation lightTexture, boolean isDarkTheme) {
        this(x, y, width, height, message, onPress, darkTexture, lightTexture, isDarkTheme, false, false);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation currentTexture = isDarkTheme ? darkTexture : lightTexture;

        if (isControlButton) {
            int backgroundV;
            if (this.isChecked) {
                backgroundV = CTRL_CHECKED_V;
            } else {
                backgroundV = this.isHoveredOrFocused() ? CTRL_HOVER_V : CTRL_NORMAL_V;
            }
            graphics.blit(currentTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0, backgroundV, 20, 20, 256, 256);
        } else {
            int backgroundV = this.isHoveredOrFocused() ? HOVER_V_COORD : NORMAL_V_COORD;
            graphics.blit(currentTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0, backgroundV, 200, 20, 256, 256);
        }

        Font font = Minecraft.getInstance().font;
        int textColor = 0xFFFFFFFF;
        int textWidth = font.width(this.getMessage());
        int textX = this.getX() + (this.getWidth() - textWidth) / 2;
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        graphics.drawString(font, this.getMessage(), textX, textY, textColor, true);
    }
}