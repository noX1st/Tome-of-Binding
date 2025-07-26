package com.nxst.tomeofbinding.client.gui;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.nxst.tomeofbinding.network.data.PlayerData;
import com.nxst.tomeofbinding.config.ModConfig;
import org.slf4j.Logger;
import java.util.function.Consumer;

public class PlayerSettingsScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PlayerData playerData;
    private final Screen previousScreen;
    private static final ResourceLocation DARK_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath("tomeofbinding", "textures/gui/dark_widget.png");
    private static final ResourceLocation LIGHT_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath("tomeofbinding", "textures/gui/light_widget.png");
    private final boolean isDarkTheme;

    public PlayerSettingsScreen(PlayerData playerData, Screen previousScreen) {
        super(Component.translatable("gui.tome.player_settings_title", playerData.getPlayerName()));
        this.playerData = playerData;
        this.previousScreen = previousScreen;
        this.isDarkTheme = ModConfig.isDarkTheme();
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - 200) / 2;
        int checkboxHeight = 20;
        int gap = 5;

        int headerY = 30;
        int y = headerY + this.font.lineHeight + 8;

        String uuid = playerData.getPlayerUUID();
        boolean isManagedIndividually = ModConfig.hasShowCoordinatesForPlayer(uuid);

        ConfigCheckbox useGlobalCoordsCheckbox = new ConfigCheckbox(x, y, 200, checkboxHeight,
                Component.translatable("gui.tome.use_global_coords"),
                !isManagedIndividually,
                (newState) -> {
                    if (isManagedIndividually) {
                        ModConfig.removePlayerSettings(uuid);
                        ModConfig.saveConfig();
                        this.rebuildWidgets();
                    }
                }
        );
        this.addRenderableWidget(useGlobalCoordsCheckbox);
        y += checkboxHeight + gap;

        if (!isManagedIndividually) {
            String globalStateKey = ModConfig.isShowPlayerCoordinates() ? "gui.tome.enabled" : "gui.tome.disabled";

            Component stateComponent = Component.translatable(globalStateKey);
            Component labelComponent = Component.translatable("gui.tome.global_coords_status").append(stateComponent);
            this.addRenderableWidget(new Label(x + 20, y, labelComponent));

            y += this.font.lineHeight + gap;
        }


        ConfigCheckbox manageIndividuallyCheckbox = new ConfigCheckbox(x, y, 200, checkboxHeight,
                Component.translatable("gui.tome.show_player_coords"),
                isManagedIndividually,
                (newState) -> {
                    if (!isManagedIndividually) {
                        ModConfig.setShowCoordinatesForPlayer(uuid, true);
                        ModConfig.saveConfig();
                        this.rebuildWidgets();
                    }
                }
        );
        this.addRenderableWidget(manageIndividuallyCheckbox);
        y += checkboxHeight + gap;

        if (isManagedIndividually) {
            boolean isCurrentlyEnabled = ModConfig.isShowCoordinatesForPlayer(uuid);

            ConfigCheckbox enableCheckbox = new ConfigCheckbox(x + 20, y, 180, checkboxHeight,
                    Component.translatable("gui.tome.enable"),
                    isCurrentlyEnabled,
                    (newState) -> {
                        if (!isCurrentlyEnabled) {
                            ModConfig.setShowCoordinatesForPlayer(uuid, true);
                            ModConfig.saveConfig();
                            this.rebuildWidgets();
                        }
                    }
            );
            this.addRenderableWidget(enableCheckbox);
            y += checkboxHeight + gap;

            ConfigCheckbox disableCheckbox = new ConfigCheckbox(x + 20, y, 180, checkboxHeight,
                    Component.translatable("gui.tome.disable"),
                    !isCurrentlyEnabled,
                    (newState) -> {
                        if (isCurrentlyEnabled) {
                            ModConfig.setShowCoordinatesForPlayer(uuid, false);
                            ModConfig.saveConfig();
                            this.rebuildWidgets();
                        }
                    }
            );
            this.addRenderableWidget(disableCheckbox);
            y += checkboxHeight + gap;
        }

        y += 15;
        ConfigCheckbox showDimensionCheckbox = new ConfigCheckbox(x, y, 200, checkboxHeight,
                Component.translatable("gui.tome.show_player_dimension"),
                ModConfig.isShowDimensionForPlayer(uuid),
                (newState) -> {
                    ModConfig.setShowDimensionForPlayer(uuid, !ModConfig.isShowDimensionForPlayer(uuid));
                    ModConfig.saveConfig();
                    this.rebuildWidgets();
                }
        );
        this.addRenderableWidget(showDimensionCheckbox);

        int backButtonY = this.height - 30;
        this.addRenderableWidget(new ThemedButton(x, backButtonY, 200, 20, Component.translatable("gui.back"), (b) -> {
            this.minecraft.setScreen(this.previousScreen);
        }, DARK_BUTTON_TEXTURE, LIGHT_BUTTON_TEXTURE, isDarkTheme));
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        this.init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = (this.width - 200) / 2;
        int titleY = 15;
        int headerY = 30;

        int textColor = 0xFFFFFFFF;

        graphics.drawString(this.font, this.title, (this.width - this.font.width(this.title)) / 2, titleY, textColor, false);
        graphics.drawString(this.font, Component.translatable("gui.tome.player_info"), x, headerY, textColor, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public class ConfigCheckbox extends Button {
        private final boolean checked;
        private final Consumer<Boolean> onToggle;

        public ConfigCheckbox(int x, int y, int width, int height, Component message, boolean initialState, Consumer<Boolean> onToggle) {
            super(x, y, width, height, message, (button) -> {}, Button.DEFAULT_NARRATION);
            this.checked = initialState;
            this.onToggle = onToggle;
        }

        @Override
        public void onPress() {
            if (onToggle != null) {
                onToggle.accept(!this.checked);
            }
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            int textColor = 0xFFFFFFFF;

            ResourceLocation texture = isDarkTheme ? DARK_BUTTON_TEXTURE : LIGHT_BUTTON_TEXTURE;
            int vOffset = this.isHoveredOrFocused() ? 66 : 46;
            graphics.blit(texture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0, vOffset, 200, 20, 256, 256);

            int checkboxSize = 10;
            int checkboxX = this.getX() + 5;
            int checkboxY = this.getY() + (this.getHeight() - checkboxSize) / 2;

            graphics.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF000000);
            graphics.fill(checkboxX + 1, checkboxY + 1, checkboxX + checkboxSize - 1, checkboxY + checkboxSize - 1, 0xFFFFFFFF);
            if (this.checked) {
                graphics.fill(checkboxX + 3, checkboxY + 3, checkboxX + checkboxSize - 3, checkboxY + checkboxSize - 3, 0xFF000000);
            }

            graphics.drawString(minecraft.font, this.getMessage(), this.getX() + checkboxSize + 8, this.getY() + (this.getHeight() - 8) / 2, textColor);
        }
    }

    public class Label extends AbstractWidget {
        public Label(int x, int y, Component message) {
            super(x, y, Minecraft.getInstance().font.width(message), Minecraft.getInstance().font.lineHeight, message);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX(), this.getY() + 2, 0xFFCCCCCC, false);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }

        @Override
        public boolean mouseClicked(double p_93722_, double p_93723_, int p_93724_) {
            return false;
        }
    }
}