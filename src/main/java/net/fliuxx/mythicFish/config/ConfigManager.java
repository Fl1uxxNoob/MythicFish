package net.fliuxx.mythicFish.config;

import net.fliuxx.mythicFish.MythicFish;
import org.bukkit.configuration.file.FileConfiguration;

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
    
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
}
