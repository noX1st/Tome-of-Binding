package com.nxst.tomeofbinding.client.gui;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.nxst.tomeofbinding.config.ModConfig;
import org.slf4j.Logger;

public class TeleportConfirmScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation DARK_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath("tomeofbinding", "textures/gui/dark_widget.png");
    private static final ResourceLocation LIGHT_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath("tomeofbinding", "textures/gui/light_widget.png");
    private static final int DARK_GUI_BACKGROUND_COLOR = 0xFF444444;
    private static final int DARK_GUI_TITLE_COLOR = 0xFFFFFFFF;
    private static final int LIGHT_GUI_BACKGROUND_COLOR = 0xFFc6c6c6;
    private static final int LIGHT_GUI_TITLE_COLOR = 0xFF000000;
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 100;
    private static final int TITLE_OFFSET_Y = 20;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 10;
    private static final int TEXT_MARGIN = 10;

    private final Component message;
    private final boolean showConfirmButtons;
    private final Button.OnPress onConfirm;
    private final Button.OnPress onCancel;
    private final boolean isDarkTheme;

    public TeleportConfirmScreen(Component message, boolean showConfirmButtons, Button.OnPress onConfirm) {
        this(message, showConfirmButtons, onConfirm, (button) -> {});
    }

    public TeleportConfirmScreen(Component message, boolean showConfirmButtons, Button.OnPress onConfirm, Button.OnPress onCancel) {
        super(Component.literal(""));
        this.message = message;
        this.showConfirmButtons = showConfirmButtons;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        this.isDarkTheme = ModConfig.isDarkTheme();
        LOGGER.info("Initialized TeleportConfirmScreen with message: {}", message.getString());
    }

    @Override
    protected void init() {
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;

        if (showConfirmButtons) {
            int buttonY = guiTop + GUI_HEIGHT - BUTTON_HEIGHT - 10;
            this.addRenderableWidget(new ThemedButton(
                    guiLeft + (GUI_WIDTH - BUTTON_WIDTH * 2 - BUTTON_GAP) / 2,
                    buttonY,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    Component.translatable("gui.yes"),
                    onConfirm,
                    DARK_BUTTON_TEXTURE,
                    LIGHT_BUTTON_TEXTURE,
                    isDarkTheme
            ));
            this.addRenderableWidget(new ThemedButton(
                    guiLeft + (GUI_WIDTH - BUTTON_WIDTH * 2 - BUTTON_GAP) / 2 + BUTTON_WIDTH + BUTTON_GAP,
                    buttonY,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    CommonComponents.GUI_CANCEL,
                    onCancel,
                    DARK_BUTTON_TEXTURE,
                    LIGHT_BUTTON_TEXTURE,
                    isDarkTheme
            ));
        } else {
            this.addRenderableWidget(new ThemedButton(
                    guiLeft + (GUI_WIDTH - BUTTON_WIDTH) / 2,
                    guiTop + GUI_HEIGHT - BUTTON_HEIGHT - 10,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    CommonComponents.GUI_OK,
                    onConfirm,
                    DARK_BUTTON_TEXTURE,
                    LIGHT_BUTTON_TEXTURE,
                    isDarkTheme
            ));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        int currentBackgroundColor = isDarkTheme ? DARK_GUI_BACKGROUND_COLOR : LIGHT_GUI_BACKGROUND_COLOR;
        int currentTitleColor = isDarkTheme ? DARK_GUI_TITLE_COLOR : LIGHT_GUI_TITLE_COLOR;

        graphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, currentBackgroundColor);

        graphics.drawWordWrap(
                font,
                this.message,
                guiLeft + TEXT_MARGIN,
                guiTop + TITLE_OFFSET_Y,
                GUI_WIDTH - 2 * TEXT_MARGIN,
                currentTitleColor
        );

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}