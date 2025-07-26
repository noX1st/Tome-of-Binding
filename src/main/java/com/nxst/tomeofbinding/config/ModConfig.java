package com.nxst.tomeofbinding.config;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ModConfig {

    private static final String CONFIG_FILE_NAME = "client.properties";
    private static final String MOD_CONFIG_FOLDER = "tomeofbinding";
    private static final String PLAYER_HISTORY_PREFIX = "playerHistory.";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Map<String, Set<String>> playerHistoryByWorld = new HashMap<>();
    private static Map<String, Boolean> showCoordinatesMap = new HashMap<>();
    private static Map<String, Boolean> showDimensionMap = new HashMap<>();

    private static boolean isDarkTheme = true;
    private static boolean showOfflinePlayers = false;
    private static boolean globalShowCoordinates = true;
    private static boolean globalShowDimension = true;

    public static void loadConfig() {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            saveConfig();
            return;
        }

        Properties props = new Properties();
        try (FileReader reader = new FileReader(configFile)) {
            props.load(reader);
            isDarkTheme = Boolean.parseBoolean(props.getProperty("global.isDarkTheme", "true"));
            showOfflinePlayers = Boolean.parseBoolean(props.getProperty("global.showOfflinePlayers", "false"));
            globalShowCoordinates = Boolean.parseBoolean(props.getProperty("global.showCoordinates", "true"));
            globalShowDimension = Boolean.parseBoolean(props.getProperty("global.showDimension", "true"));

            playerHistoryByWorld.clear();
            showCoordinatesMap.clear();
            showDimensionMap.clear();

            for (String key : props.stringPropertyNames()) {
                if (key.startsWith(PLAYER_HISTORY_PREFIX)) {
                    String worldId = key.substring(PLAYER_HISTORY_PREFIX.length());
                    if (worldId.isEmpty()) continue;
                    String history = props.getProperty(key, "");
                    if (!history.isEmpty()) {
                        Set<String> players = new HashSet<>();
                        for (String entry : history.split(",")) {
                            if (!entry.trim().isEmpty()) {
                                players.add(entry.trim());
                            }
                        }
                        playerHistoryByWorld.put(worldId, players);
                    }
                } else if (key.startsWith("player.") && key.endsWith(".showCoordinates")) {
                    String uuid = key.substring(7, key.length() - 17);
                    showCoordinatesMap.put(uuid, Boolean.parseBoolean(props.getProperty(key)));
                } else if (key.startsWith("player.") && key.endsWith(".showDimension")) {
                    String uuid = key.substring(7, key.length() - 15);
                    showDimensionMap.put(uuid, Boolean.parseBoolean(props.getProperty(key)));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load Tome of Binding config: {}", e.getMessage());
        }
    }

    public static void saveConfig() {
        File configFile = getConfigFile();
        configFile.getParentFile().mkdirs();

        Properties props = new Properties();
        props.setProperty("global.isDarkTheme", String.valueOf(isDarkTheme));
        props.setProperty("global.showOfflinePlayers", String.valueOf(showOfflinePlayers));
        props.setProperty("global.showCoordinates", String.valueOf(globalShowCoordinates));
        props.setProperty("global.showDimension", String.valueOf(globalShowDimension));

        for (Map.Entry<String, Set<String>> entry : playerHistoryByWorld.entrySet()) {
            String worldId = entry.getKey();
            if (worldId != null && !worldId.isEmpty()) {
                props.setProperty(PLAYER_HISTORY_PREFIX + worldId, String.join(",", entry.getValue()));
            }
        }
        showCoordinatesMap.forEach((uuid, value) -> props.setProperty("player." + uuid + ".showCoordinates", String.valueOf(value)));
        showDimensionMap.forEach((uuid, value) -> props.setProperty("player." + uuid + ".showDimension", String.valueOf(value)));

        try (FileWriter writer = new FileWriter(configFile)) {
            props.store(writer, "Tome of Binding Client Configuration");
        } catch (IOException e) {
            LOGGER.error("Failed to save Tome of Binding config: {}", e.getMessage());
        }
    }

    private static File getConfigFile() {
        File gameDirectory = Minecraft.getInstance().gameDirectory;
        File configDir = new File(gameDirectory, "config");
        return new File(configDir, MOD_CONFIG_FOLDER + File.separator + CONFIG_FILE_NAME);
    }

    private static String getCurrentWorldId() {
        Minecraft mc = Minecraft.getInstance();
        ServerData serverData = mc.getCurrentServer();
        if (serverData != null) {
            return serverData.ip.replace(":", "_");
        }

        MinecraftServer integratedServer = mc.getSingleplayerServer();
        if (integratedServer != null) {
            try {
                Field storageSourceField = MinecraftServer.class.getDeclaredField("storageSource");
                storageSourceField.setAccessible(true);
                LevelStorageSource.LevelStorageAccess storageAccess = (LevelStorageSource.LevelStorageAccess) storageSourceField.get(integratedServer);
                return storageAccess.getLevelId();
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOGGER.error("Could not access 'storageSource' via reflection, falling back to level name.", e);
                return integratedServer.getWorldData().getLevelName();
            }
        }

        return "default_world";
    }

    public static Set<String> getPlayerHistoryForCurrentWorld() {
        String worldId = getCurrentWorldId();
        return playerHistoryByWorld.getOrDefault(worldId, new HashSet<>());
    }

    public static Set<String> getPlayerHistory(String worldId) {
        return playerHistoryByWorld.getOrDefault(worldId, new HashSet<>());
    }

    public static void addPlayerToHistory(String uuid, String name, String dimension, double x, double y, double z) {
        String worldId = getCurrentWorldId();
        Set<String> players = playerHistoryByWorld.computeIfAbsent(worldId, k -> new HashSet<>());
        String newEntry = String.format(Locale.US, "%s:%s:%s:%.1f:%.1f:%.1f", uuid, name, dimension, x, y, z);
        players.removeIf(entry -> entry.startsWith(uuid + ":"));
        players.add(newEntry);
    }

    public static boolean isDarkTheme() { return isDarkTheme; }
    public static void setDarkTheme(boolean theme) { isDarkTheme = theme; }

    public static boolean isShowOfflinePlayers() { return showOfflinePlayers; }
    public static void setShowOfflinePlayers(boolean show) { showOfflinePlayers = show; }

    public static boolean isShowPlayerCoordinates() { return globalShowCoordinates; }
    public static void setShowPlayerCoordinates(boolean show) { globalShowCoordinates = show; }

    public static boolean hasShowCoordinatesForPlayer(String uuid) {
        return showCoordinatesMap.containsKey(uuid);
    }

    public static boolean isShowCoordinatesForPlayer(String uuid) {
        return showCoordinatesMap.getOrDefault(uuid, globalShowCoordinates);
    }

    public static boolean isShowDimensionForPlayer(String uuid) {
        return showDimensionMap.getOrDefault(uuid, globalShowDimension);
    }

    public static void setShowCoordinatesForPlayer(String uuid, boolean show) {
        showCoordinatesMap.put(uuid, show);
    }

    public static void setShowDimensionForPlayer(String uuid, boolean show) {
        showDimensionMap.put(uuid, show);
    }

    public static void removePlayerSettings(String uuid) {
        showCoordinatesMap.remove(uuid);
    }
}