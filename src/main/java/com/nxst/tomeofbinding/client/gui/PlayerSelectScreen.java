package com.nxst.tomeofbinding.client.gui;

import com.mojang.logging.LogUtils;
import com.nxst.tomeofbinding.Tome;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.nxst.tomeofbinding.network.data.PlayerData;
import com.nxst.tomeofbinding.network.data.PlayerListRequestMessage;
import com.nxst.tomeofbinding.network.PlayerSelectPacket;
import com.nxst.tomeofbinding.network.TeleportToLastLocationPacket;
import com.nxst.tomeofbinding.config.ModConfig;

public class PlayerSelectScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation DARK_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath("tomeofbinding", "textures/gui/dark_widget.png");
    private static final ResourceLocation LIGHT_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath("tomeofbinding", "textures/gui/light_widget.png");
    private static final int DARK_GUI_BACKGROUND_COLOR = 0xFF444444;
    private static final int DARK_GUI_TITLE_COLOR = 0xFFFFFFFF;
    private static final int LIGHT_GUI_BACKGROUND_COLOR = 0xFFc6c6c6;
    private static final int LIGHT_GUI_TITLE_COLOR = 0xFF000000;
    private static final int MIN_GUI_WIDTH = 184;
    private static final int TITLE_OFFSET_Y = 6;
    private static final int BUTTONS_START_OFFSET_Y = TITLE_OFFSET_Y + 16;
    private static final int SEARCH_BOX_HEIGHT = 20;
    private static final int EXTENDED_BUTTON_HEIGHT = 36;
    private static final int COORDINATES_OFF_BUTTON_HEIGHT = 26;
    private static final int ROW_GAP = 4;
    private static final int HORIZONTAL_BUTTON_GAP = 2;
    private static final int BOTTOM_AREA_HEIGHT = 35;
    private static final int MAX_ROWS_PER_PAGE = 5;
    private static final int CONTROL_BUTTON_SIZE = 14;
    private static final int CONTROL_BUTTON_GAP = 2;
    private static final int TEXT_MARGIN = 10;
    private EditBox searchBox;
    private String searchQuery = "";
    private boolean isSearchVisible = false;
    private boolean showOfflinePlayers = ModConfig.isShowOfflinePlayers();
    private boolean showPlayerCoordinates = ModConfig.isShowPlayerCoordinates();
    private List<PlayerData> fullPlayerDataList;
    private List<PlayerData> filteredPlayerDataList;
    private int updateTimer = 0;
    private int currentPageOffset = 0;
    private int guiLeft;
    private int guiTop;
    private int currentGuiWidth;
    private int currentGuiHeight;
    private int currentButtonWidth;
    private boolean useTwoColumnLayout = false;
    private Component currentTitleComponent;
    private boolean isDarkTheme = true;
    private List<String> pinnedPlayers = new ArrayList<>();
    private int currentPlayerRowsHeight;

    public PlayerSelectScreen(List<PlayerData> initialPlayerList) {
        super(Component.literal(""));
        this.fullPlayerDataList = new ArrayList<>(initialPlayerList);
        this.filteredPlayerDataList = new ArrayList<>(initialPlayerList);
        updateTitleComponent();
        this.isDarkTheme = ModConfig.isDarkTheme();
        this.showPlayerCoordinates = ModConfig.isShowPlayerCoordinates();
    }

    private void updateTitleComponent() {
        this.currentTitleComponent = this.filteredPlayerDataList.isEmpty() && this.searchQuery.isEmpty()
                ? Component.translatable("gui.tome.no_players_online")
                : Component.translatable("gui.tome.select_player_title");
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(this.font, 0, 0, 0, SEARCH_BOX_HEIGHT, Component.translatable("gui.tome.search_player"), DARK_BUTTON_TEXTURE, LIGHT_BUTTON_TEXTURE, isDarkTheme);
        this.searchBox.setMaxLength(50);
        this.searchBox.setResponder(this::onSearchQueryChanged);
        this.addRenderableWidget(this.searchBox);
        rebuildGui();
    }

    @Override
    public void tick() {
        super.tick();
        updateTimer++;
        if (updateTimer >= 20) {
            updateTimer = 0;
            Tome.INSTANCE.sendToServer(new PlayerListRequestMessage(showOfflinePlayers));
        }
    }

    public void updatePlayerData(List<PlayerData> newOnlinePlayers) {
        Map<String, PlayerData> onlinePlayerMap = newOnlinePlayers.stream()
                .collect(Collectors.toMap(PlayerData::getPlayerUUID, p -> p));

        List<PlayerData> combinedList = new ArrayList<>(newOnlinePlayers);

        if (showOfflinePlayers) {
            for (String entry : ModConfig.getPlayerHistoryForCurrentWorld()) {
                String[] allParts = entry.split("(?<!\\\\):");
                if (allParts.length >= 6) {
                    String uuid = allParts[0].replace("\\:", ":");
                    if (!onlinePlayerMap.containsKey(uuid)) {
                        String name = allParts[1].replace("\\:", ":");
                        String x = allParts[allParts.length - 3].replace("\\:", ":");
                        String y = allParts[allParts.length - 2].replace("\\:", ":");
                        String z = allParts[allParts.length - 1].replace("\\:", ":");
                        String dimension = String.join(":", Arrays.copyOfRange(allParts, 2, allParts.length - 3)).replace("\\:", ":");
                        double parsedX = parseDouble(x.replace(",", "."), 0.0);
                        double parsedY = parseDouble(y.replace(",", "."), 0.0);
                        double parsedZ = parseDouble(z.replace(",", "."), 0.0);
                        combinedList.add(new PlayerData(name, dimension, -1, uuid, false, parsedX, parsedY, parsedZ));
                    }
                }
            }
        }

        combinedList.sort(Comparator
                .comparing((PlayerData p) -> !pinnedPlayers.contains(p.getPlayerUUID()))
                .thenComparing(PlayerData::getPlayerName, String.CASE_INSENSITIVE_ORDER));

        this.fullPlayerDataList = combinedList;
        applyFilter();
    }


    private double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to parse double from '{}', using default {}", value, defaultValue);
            return defaultValue;
        }
    }

    private void onSearchQueryChanged(String query) {
        this.searchQuery = query;
        this.currentPageOffset = 0;
        applyFilter();
    }

    private void applyFilter() {
        if (searchQuery.isEmpty()) {
            this.filteredPlayerDataList = new ArrayList<>(this.fullPlayerDataList);
        } else {
            String lowerCaseQuery = searchQuery.toLowerCase();
            this.filteredPlayerDataList = this.fullPlayerDataList.stream()
                    .filter(player -> player.getPlayerName().toLowerCase().contains(lowerCaseQuery))
                    .collect(Collectors.toList());
        }
        updateTitleComponent();
        rebuildGui();
    }

    private int calculateMaximumTextWidth() {
        if (this.minecraft == null) return 120;
        int maxTextWidth = 0;
        List<PlayerData> listToCheck = fullPlayerDataList.isEmpty() ? List.of(new PlayerData("PlayerName (Offline)", "minecraft:overworld", 0, "", false, 1234, 123, 1234)) : this.fullPlayerDataList;

        for (PlayerData playerData : listToCheck) {
            String nameText = playerData.getPlayerName();
            if (!playerData.isOnline()) {
                nameText += " (Offline)";
            }
            maxTextWidth = Math.max(maxTextWidth, this.font.width(nameText));

            if (ModConfig.isShowDimensionForPlayer(playerData.getPlayerUUID())) {
                String translationKey = "dimension." + playerData.getPlayerDimension().replace(':', '.');
                Component dimensionComponent = Component.translatable(translationKey);
                maxTextWidth = Math.max(maxTextWidth, this.font.width(dimensionComponent));
            }

            boolean showCoordinates = ModConfig.hasShowCoordinatesForPlayer(playerData.getPlayerUUID()) ?
                    ModConfig.isShowCoordinatesForPlayer(playerData.getPlayerUUID()) :
                    ModConfig.isShowPlayerCoordinates();
            if (showCoordinates) {
                String coordsText = String.format("X: %d, Y: %d, Z: %d", (int) playerData.getX(), (int) playerData.getY(), (int) playerData.getZ());
                maxTextWidth = Math.max(maxTextWidth, this.font.width(coordsText));
            }
        }
        maxTextWidth = Math.max(maxTextWidth, this.font.width(CommonComponents.GUI_CANCEL));
        return maxTextWidth;
    }

    private void recalculateGuiDimensions() {
        this.currentButtonWidth = Math.max(144, calculateMaximumTextWidth() + 30 + 15);
        int buttonsAndControlsWidth = this.currentButtonWidth + CONTROL_BUTTON_SIZE + HORIZONTAL_BUTTON_GAP;
        int singleColumnGuiWidth = buttonsAndControlsWidth + 40;
        int titleWidth = this.font.width(this.currentTitleComponent) + 20;
        this.currentGuiWidth = Math.max(MIN_GUI_WIDTH, Math.max(singleColumnGuiWidth, titleWidth));

        int widthAvailableForButtons = this.currentGuiWidth - 24 - (CONTROL_BUTTON_SIZE + HORIZONTAL_BUTTON_GAP);
        this.useTwoColumnLayout = filteredPlayerDataList.size() > MAX_ROWS_PER_PAGE && widthAvailableForButtons >= (this.currentButtonWidth * 2 + HORIZONTAL_BUTTON_GAP);

        if (useTwoColumnLayout) {
            this.currentGuiWidth = Math.max(this.currentGuiWidth, (currentButtonWidth * 2) + HORIZONTAL_BUTTON_GAP * 3 + CONTROL_BUTTON_SIZE + 24);
        }

        int playersPerPage = getPlayersPerPage();
        int playersOnPage = Math.min(filteredPlayerDataList.size() - currentPageOffset, playersPerPage);
        int numRows = useTwoColumnLayout ? (int) Math.ceil(playersOnPage / 2.0) : playersOnPage;

        int playerRowsHeight = 0;
        if (playersOnPage > 0) {
            if (useTwoColumnLayout) {
                for (int i = 0; i < numRows; i++) {
                    int rowStartIndex = currentPageOffset + i * 2;
                    if(rowStartIndex >= filteredPlayerDataList.size()) continue;

                    PlayerData p1 = filteredPlayerDataList.get(rowStartIndex);
                    boolean p1Coords = ModConfig.hasShowCoordinatesForPlayer(p1.getPlayerUUID()) ? ModConfig.isShowCoordinatesForPlayer(p1.getPlayerUUID()) : ModConfig.isShowPlayerCoordinates();
                    int h1 = p1Coords ? EXTENDED_BUTTON_HEIGHT : COORDINATES_OFF_BUTTON_HEIGHT;
                    int h2 = 0;
                    if (rowStartIndex + 1 < filteredPlayerDataList.size() && (rowStartIndex + 1) < (currentPageOffset + playersOnPage)) {
                        PlayerData p2 = filteredPlayerDataList.get(rowStartIndex + 1);
                        boolean p2Coords = ModConfig.hasShowCoordinatesForPlayer(p2.getPlayerUUID()) ? ModConfig.isShowCoordinatesForPlayer(p2.getPlayerUUID()) : ModConfig.isShowPlayerCoordinates();
                        h2 = p2Coords ? EXTENDED_BUTTON_HEIGHT : COORDINATES_OFF_BUTTON_HEIGHT;
                    }
                    playerRowsHeight += Math.max(h1, h2) + ROW_GAP;
                }
            } else {
                for (int i = 0; i < playersOnPage; i++) {
                    PlayerData playerData = filteredPlayerDataList.get(currentPageOffset + i);
                    boolean showCoordinates = ModConfig.hasShowCoordinatesForPlayer(playerData.getPlayerUUID()) ? ModConfig.isShowCoordinatesForPlayer(playerData.getPlayerUUID()) : ModConfig.isShowPlayerCoordinates();
                    playerRowsHeight += (showCoordinates ? EXTENDED_BUTTON_HEIGHT : COORDINATES_OFF_BUTTON_HEIGHT) + ROW_GAP;
                }
            }
            playerRowsHeight -= ROW_GAP;
        } else {
            playerRowsHeight = COORDINATES_OFF_BUTTON_HEIGHT;
        }
        this.currentPlayerRowsHeight = playerRowsHeight;

        int gapAfterButtons = 8;
        int contentHeight = playerRowsHeight;
        if (isSearchVisible) {
            contentHeight += SEARCH_BOX_HEIGHT + gapAfterButtons;
        }

        this.currentGuiHeight = BUTTONS_START_OFFSET_Y + contentHeight + BOTTOM_AREA_HEIGHT;
        this.guiLeft = (this.width - this.currentGuiWidth) / 2;
        this.guiTop = (this.height - this.currentGuiHeight) / 2;
    }


    private int getPlayersPerPage() {
        return useTwoColumnLayout ? MAX_ROWS_PER_PAGE * 2 : MAX_ROWS_PER_PAGE;
    }

    private void rebuildGui() {
        if (this.minecraft != null) {
            recalculateGuiDimensions();
            updateScreenElements();
        }
    }

    private void updateScreenElements() {
        this.clearWidgets();
        this.addRenderableWidget(this.searchBox);

        int buttonsStartY = guiTop + BUTTONS_START_OFFSET_Y;
        int lastY = buttonsStartY;

        int controlButtonsTotalHeight = (CONTROL_BUTTON_SIZE * 4) + (CONTROL_BUTTON_GAP * 3);
        int verticalOffset = (this.currentPlayerRowsHeight - controlButtonsTotalHeight) / 2;
        int controlButtonsStartY = buttonsStartY + Math.max(0, verticalOffset);

        int controlButtonsX = guiLeft + 8;
        int controlButtonsY = controlButtonsStartY;

        this.addRenderableWidget(new ThemedButton(controlButtonsX, controlButtonsY, CONTROL_BUTTON_SIZE, CONTROL_BUTTON_SIZE, Component.literal("🔍"), (b) -> { isSearchVisible = !isSearchVisible; if (!isSearchVisible) onSearchQueryChanged(""); rebuildGui(); }, DARK_BUTTON_TEXTURE, LIGHT_BUTTON_TEXTURE, isDarkTheme, true, isSearchVisible));
        controlButtonsY += CONTROL_BUTTON_SIZE + CONTROL_BUTTON_GAP;
        this.addRenderableWidget(new ThemedButton(controlButtonsX, controlButtonsY, CONTROL_BUTTON_SIZE, CONTROL_BUTTON_SIZE, Component.literal("📍"), (b) -> { showPlayerCoordinates = !showPlayerCoordinates; ModConfig.setShowPlayerCoordinates(showPlayerCoordinates); ModConfig.saveConfig(); rebuildGui(); }, DARK_BUTTON_TEXTURE, LIGHT_BUTTON_TEXTURE, isDarkTheme, true, showPlayerCoordinates));
        controlButtonsY += CONTROL_BUTTON_SIZE + CONTROL_BUTTON_GAP;
        this.addRenderableWidget(new ThemedButton(controlButtonsX, controlButtonsY, CONTROL_BUTTON_SIZE, CONTROL_BUTTON_SIZE, Component.literal("👥"), (b) -> { showOfflinePlayers = !showOfflinePlayers; ModConfig.setShowOfflinePlayers(showOfflinePlayers); ModConfig.saveConfig(); rebuildGui(); }, DARK_BUTTON_TEXTURE, LIGHT_BUTTON_TEXTURE, isDarkTheme, true, showOfflinePlayers));
        controlButtonsY += CONTROL_BUTTON_SIZE + CONTROL_BUTTON_GAP;
        Component themeButtonText = isDarkTheme ? Component.literal("☼") : Component.literal("☾");
        this.addRenderableWidget(new ThemedButton(controlButtonsX, controlButtonsY, CONTROL_BUTTON_SIZE, CONTROL_BUTTON_SIZE, themeButtonText, (b) -> { isDarkTheme = !isDarkTheme; ModConfig.setDarkTheme(isDarkTheme); ModConfig.saveConfig(); rebuildGui(); }, DARK_BUTTON_TEXTURE, LIGHT_BUTTON_TEXTURE, isDarkTheme, true, false));

        if (!filteredPlayerDataList.isEmpty()) {
            int playersPerPage = getPlayersPerPage();
            int startIndex = currentPageOffset;
            int endIndex = Math.min(currentPageOffset + playersPerPage, filteredPlayerDataList.size());

            int buttonsBlockWidth = useTwoColumnLayout ? (currentButtonWidth * 2) + HORIZONTAL_BUTTON_GAP : currentButtonWidth;
            int playerButtonsStartXAdjusted = guiLeft + (currentGuiWidth - buttonsBlockWidth) / 2;

            int cumulativeY = 0;
            for (int i = startIndex; i < endIndex; i++) {
                PlayerData playerData = filteredPlayerDataList.get(i);
                int indexOnPage = i - startIndex;
                int buttonX, buttonY;

                boolean showCoordinates = ModConfig.hasShowCoordinatesForPlayer(playerData.getPlayerUUID()) ?
                        ModConfig.isShowCoordinatesForPlayer(playerData.getPlayerUUID()) :
                        ModConfig.isShowPlayerCoordinates();
                int buttonHeight = showCoordinates ? EXTENDED_BUTTON_HEIGHT : COORDINATES_OFF_BUTTON_HEIGHT;

                if (useTwoColumnLayout) {
                    int row = indexOnPage / 2;
                    int col = indexOnPage % 2;
                    buttonX = playerButtonsStartXAdjusted + (col * (currentButtonWidth + HORIZONTAL_BUTTON_GAP));

                    int h1 = 0;
                    if(i < endIndex) {
                        boolean p1Coords = ModConfig.hasShowCoordinatesForPlayer(filteredPlayerDataList.get(i).getPlayerUUID()) ? ModConfig.isShowCoordinatesForPlayer(filteredPlayerDataList.get(i).getPlayerUUID()) : ModConfig.isShowPlayerCoordinates();
                        h1 = p1Coords ? EXTENDED_BUTTON_HEIGHT : COORDINATES_OFF_BUTTON_HEIGHT;
                    }
                    int h2 = 0;
                    if (i + 1 < endIndex) {
                        boolean p2Coords = ModConfig.hasShowCoordinatesForPlayer(filteredPlayerDataList.get(i + 1).getPlayerUUID()) ? ModConfig.isShowCoordinatesForPlayer(filteredPlayerDataList.get(i+1).getPlayerUUID()) : ModConfig.isShowPlayerCoordinates();
                        h2 = p2Coords ? EXTENDED_BUTTON_HEIGHT : COORDINATES_OFF_BUTTON_HEIGHT;
                    }

                    if (col == 0) {
                        buttonY = buttonsStartY + cumulativeY;
                        if(row > 0) cumulativeY += Math.max(h1, h2) + ROW_GAP;
                    } else {
                        buttonY = buttonsStartY + cumulativeY;
                    }

                } else {
                    buttonX = playerButtonsStartXAdjusted;
                    buttonY = buttonsStartY + cumulativeY;
                    cumulativeY += buttonHeight + ROW_GAP;
                }
                this.addRenderableWidget(new PlayerButton(buttonX, buttonY, this.currentButtonWidth, buttonHeight, playerData.getPlayerName(), playerData.getPlayerDimension(), playerData.getPlayerDistance(), playerData.getPlayerUUID(), playerData.isOnline(), playerData.getX(), playerData.getY(), playerData.getZ(), button -> onPlayerSelected(playerData), isDarkTheme, DARK_BUTTON_TEXTURE, LIGHT_BUTTON_TEXTURE));

                int playerControlButtonsX = buttonX + this.currentButtonWidth + HORIZONTAL_BUTTON_GAP;
                int playerControlButtonsY = buttonY + (buttonHeight - (2 * CONTROL_BUTTON_SIZE + CONTROL_BUTTON_GAP)) / 2;

                this.addRenderableWidget(new ThemedButton(playerControlButtonsX, playerControlButtonsY, CONTROL_BUTTON_SIZE, CONTROL_BUTTON_SIZE, Component.literal("📌"), (b) -> togglePinPlayer(playerData), DARK_BUTTON_TEXTURE, LIGHT_BUTTON_TEXTURE, isDarkTheme, true, pinnedPlayers.contains(playerData.getPlayerUUID())));
                playerControlButtonsY += CONTROL_BUTTON_SIZE + CONTROL_BUTTON_GAP;
                this.addRenderableWidget(new ThemedButton(playerControlButtonsX, playerControlButtonsY, CONTROL_BUTTON_SIZE, CONTROL_BUTTON_SIZE, Component.literal("⚙"), (b) -> Minecraft.getInstance().setScreen(new PlayerSettingsScreen(playerData, this)), DARK_BUTTON_TEXTURE, LIGHT_BUTTON_TEXTURE, isDarkTheme, true, false));

                if (i == endIndex - 1) {
                    lastY = buttonY + buttonHeight;
                }
            }
        } else {
            lastY = buttonsStartY + COORDINATES_OFF_BUTTON_HEIGHT;
        }

        this.searchBox.setVisible(isSearchVisible);
        if (isSearchVisible) {
            int searchBoxX = guiLeft + (currentGuiWidth - 144) / 2;
            int searchBoxY = lastY + 8;
            this.searchBox.setPosition(searchBoxX, searchBoxY);
            this.searchBox.setWidth(144);
            this.searchBox.setTheme(isDarkTheme);
            lastY = searchBoxY + SEARCH_BOX_HEIGHT;
        }

        int bottomY = guiTop + currentGuiHeight - 25;
        int cancelWidth = 150;
        int cancelX = guiLeft + (this.currentGuiWidth - cancelWidth) / 2;
        this.addRenderableWidget(new ThemedButton(cancelX, bottomY, cancelWidth, 20, CommonComponents.GUI_CANCEL, (b) -> this.onClose(), DARK_BUTTON_TEXTURE, LIGHT_BUTTON_TEXTURE, isDarkTheme));
    }

    private void togglePinPlayer(PlayerData playerData) {
        String uuid = playerData.getPlayerUUID();
        if (pinnedPlayers.contains(uuid)) {
            pinnedPlayers.remove(uuid);
        } else {
            pinnedPlayers.add(uuid);
        }
        fullPlayerDataList.sort(Comparator
                .comparing((PlayerData p) -> !pinnedPlayers.contains(p.getPlayerUUID()))
                .thenComparing(PlayerData::getPlayerName, String.CASE_INSENSITIVE_ORDER));
        applyFilter();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int currentBackgroundColor = isDarkTheme ? DARK_GUI_BACKGROUND_COLOR : LIGHT_GUI_BACKGROUND_COLOR;
        int currentTitleColor = isDarkTheme ? DARK_GUI_TITLE_COLOR : LIGHT_GUI_TITLE_COLOR;

        graphics.fill(guiLeft, guiTop, guiLeft + currentGuiWidth, guiTop + currentGuiHeight, currentBackgroundColor);

        int titleX = guiLeft + (this.currentGuiWidth - this.font.width(this.currentTitleComponent)) / 2;
        graphics.drawString(this.font, this.currentTitleComponent, titleX, guiTop + TITLE_OFFSET_Y, currentTitleColor, false);

        if (filteredPlayerDataList.isEmpty() && !searchQuery.isEmpty()) {
            Component noResults = Component.translatable("gui.tome.no_players_found");
            int noResultsColor = isDarkTheme ? 0xA0A0A0 : 0xFF000000;
            int textWidth = this.font.width(noResults);
            int textX = guiLeft + (this.currentGuiWidth - textWidth) / 2;
            graphics.drawString(this.font, noResults, textX, guiTop + BUTTONS_START_OFFSET_Y + 10, noResultsColor, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused() && keyCode == 256) {
            this.searchBox.setFocused(false);
            return true;
        }
        if (keyCode == 256 || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int playersPerPage = getPlayersPerPage();
        if (filteredPlayerDataList.size() <= playersPerPage) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (delta > 0) {
            if (currentPageOffset > 0) {
                currentPageOffset = Math.max(0, currentPageOffset - getPlayersPerPage());
                rebuildGui();
            }
        } else if (delta < 0) {
            if (currentPageOffset + playersPerPage < filteredPlayerDataList.size()) {
                currentPageOffset += getPlayersPerPage();
                rebuildGui();
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void onPlayerSelected(PlayerData targetPlayerData) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        String currentDimension = this.minecraft.player.level().dimension().location().toString();
        String targetDimension = targetPlayerData.getPlayerDimension();

        if (!currentDimension.equals(targetDimension) && !targetPlayerData.isOnline()) {
            this.minecraft.setScreen(new TeleportConfirmScreen(
                    Component.translatable("gui.tome.dimension_mismatch"),
                    false,
                    (button) -> this.minecraft.setScreen(this)
            ));
        } else {
            String translationKey = targetPlayerData.isOnline()
                    ? "gui.tome.confirm_teleport"
                    : "gui.tome.confirm_teleport_offline";
            this.minecraft.setScreen(new TeleportConfirmScreen(
                    Component.translatable(translationKey, targetPlayerData.getPlayerName()),
                    true,
                    (button) -> {
                        if (targetPlayerData.isOnline()) {
                            Tome.INSTANCE.sendToServer(new PlayerSelectPacket(targetPlayerData.getPlayerName()));
                        } else {
                            Tome.INSTANCE.sendToServer(new TeleportToLastLocationPacket(
                                    targetPlayerData.getPlayerName(),
                                    targetPlayerData.getPlayerDimension(),
                                    targetPlayerData.getX(),
                                    targetPlayerData.getY(),
                                    targetPlayerData.getZ()
                            ));
                        }
                        this.onClose();
                    },
                    (button) -> this.minecraft.setScreen(this)
            ));
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}