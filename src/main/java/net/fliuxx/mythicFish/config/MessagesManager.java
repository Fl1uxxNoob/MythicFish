package net.fliuxx.mythicFish.config;

import net.fliuxx.mythicFish.MythicFish;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessagesManager {
    
    private final MythicFish plugin;
    private FileConfiguration messages;
    private File messagesFile;
    
    public MessagesManager(MythicFish plugin) {
        this.plugin = plugin;
    }
    
    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString(key, "&cMessage not found: " + key));
    }
    
    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        
        return message;
    }
    
    public void reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }
}
