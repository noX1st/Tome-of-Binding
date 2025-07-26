package com.nxst.tomeofbinding.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class EditBox extends net.minecraft.client.gui.components.EditBox {

    private final ResourceLocation darkTexture;
    private final ResourceLocation lightTexture;
    private boolean isDarkTheme;
    private final Font fontRenderer;
    private Component customSuggestionComponent;

    private static final int NORMAL_V_COORD = 46;
    private static final int HOVER_V_COORD = 66;

    public EditBox(Font font, int x, int y, int width, int height, Component message, ResourceLocation darkTexture, ResourceLocation lightTexture, boolean isDarkTheme) {
        super(font, x, y, width, height, Component.empty());
        this.darkTexture = darkTexture;
        this.lightTexture = lightTexture;
        this.isDarkTheme = isDarkTheme;
        this.fontRenderer = font;
        this.setBordered(false);
        this.customSuggestionComponent = message;
    }

    public void setTheme(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
    }

    public void setCustomSuggestion(Component newSuggestion) {
        this.customSuggestionComponent = newSuggestion;
    }

    public Component getCustomSuggestion() {
        return this.customSuggestionComponent;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.isVisible()) {
            return;
        }

        ResourceLocation currentTexture = isDarkTheme ? darkTexture : lightTexture;
        int backgroundV = this.isHoveredOrFocused() ? HOVER_V_COORD : NORMAL_V_COORD;
        graphics.blit(currentTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0, backgroundV, 200, 20, 256, 256);

        int textColor = 0xFFFFFFFF;
        int textY = this.getY() + (this.getHeight() - 8) / 2;

        if (this.getValue().isEmpty() && !this.isFocused() && this.customSuggestionComponent != null) {
            int suggestionColor = 0xFFA0A0A0;
            int suggestionWidth = this.fontRenderer.width(this.customSuggestionComponent);
            int suggestionX = this.getX() + (this.getWidth() - suggestionWidth) / 2;
            graphics.drawString(this.fontRenderer, this.customSuggestionComponent, suggestionX, textY, suggestionColor);
        } else {
            String value = this.getValue();
            String visibleText = this.fontRenderer.plainSubstrByWidth(value, this.getInnerWidth(), true);

            int textWidth = this.fontRenderer.width(visibleText);
            int textX = this.getX() + (this.getWidth() - textWidth) / 2;

            graphics.drawString(this.fontRenderer, visibleText, textX, textY, textColor);
        }
    }
}