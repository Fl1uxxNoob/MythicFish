package net.fliuxx.mythicFish.config;

import net.fliuxx.mythicFish.MythicFish;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    
    private final MythicFish plugin;
    private FileConfiguration config;
    
    public ConfigManager(MythicFish plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public boolean isVanillaFishingDisabled() {
        return config.getBoolean("settings.disable-vanilla-fishing", true);
    }
    
    public boolean isDatabaseEnabled() {
        return config.getBoolean("database.enabled", true);
    }
    
    public String getDatabasePath() {
        return config.getString("database.path", "plugins/MythicFish/data.db");
    }

    public boolean isAsyncDatabase() {
        return config.getBoolean("settings.async-database", true);
    }

    // --- Rare-catch announcements ---

    public boolean areAnnouncementsEnabled() {
        return config.getBoolean("announcements.enabled", true);
    }

    public boolean isAnnounceOnlyFirstCatch() {
        return config.getBoolean("announcements.only-first-catch", true);
    }

    public List<String> getAnnouncementRarities() {
        return config.getStringList("announcements.rarities");
    }

    public String getAnnouncementSound() {
        return config.getString("announcements.sound", "");
    }

    // --- Leaderboard ---

    public boolean isLeaderboardEnabled() {
        return config.getBoolean("leaderboard.enabled", true);
    }

    public int getLeaderboardSize() {
        return config.getInt("leaderboard.size", 10);
    }

    // --- PlaceholderAPI ---

    public boolean isPlaceholderApiEnabled() {
        return config.getBoolean("placeholderapi.enabled", true);
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
}
