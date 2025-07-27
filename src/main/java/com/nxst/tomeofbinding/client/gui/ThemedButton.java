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
    private Component tooltip;

    private static long hoveredButtonId = -1;
    private static int hoverTicks = 0;
    private static final int TOOLTIP_DELAY = 70;

    private static final int NORMAL_V_COORD = 46;
    private static final int HOVER_V_COORD = 66;

    private static final int CTRL_NORMAL_V = 106;
    private static final int CTRL_HOVER_V = 126;
    private static final int CTRL_CHECKED_V = 146;

    public ThemedButton(int x, int y, int width, int height, Component message, OnPress onPress, ResourceLocation darkTexture, ResourceLocation lightTexture, boolean isDarkTheme, boolean isControlButton, boolean isChecked, Component tooltip) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.darkTexture = darkTexture;
        this.lightTexture = lightTexture;
        this.isDarkTheme = isDarkTheme;
        this.isControlButton = isControlButton;
        this.isChecked = isChecked;
        this.tooltip = tooltip;
    }

    private static long buttonId(Button b) {
        return ((long)b.getX() << 32) | b.getY();
    }

    public ThemedButton(int x, int y, int width, int height, Component message, OnPress onPress, ResourceLocation darkTexture, ResourceLocation lightTexture, boolean isDarkTheme) {
        this(x, y, width, height, message, onPress, darkTexture, lightTexture, isDarkTheme, false, false, null);
    }

    public void setTooltip(Component tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long currentId = buttonId(this);

        if (this.isHovered()) {
            if (hoveredButtonId == currentId) {
                hoverTicks++;
            } else {
                hoveredButtonId = currentId;
                hoverTicks = 1;
            }
        } else {
            if (hoveredButtonId == currentId) {
                hoveredButtonId = -1;
                hoverTicks = 0;
            }
        }

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

        if (this.isHovered() && this.tooltip != null && hoverTicks > TOOLTIP_DELAY) {
            graphics.renderTooltip(font, this.tooltip, mouseX, mouseY);
        }
    }
}