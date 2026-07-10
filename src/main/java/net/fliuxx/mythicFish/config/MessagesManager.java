package net.fliuxx.mythicFish.config;

import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.fish.FishRarity;
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
        return getMessage(key, new String[0]);
    }

    /**
     * Resolve a message: substitute placeholders on the raw string first, then translate the
     * '&' colour codes once at the end. Doing the translation last ensures placeholder values
     * that themselves contain colour codes (e.g. a fish display name like "&6Golden Salmon")
     * are coloured instead of showing the literal '&'.
     */
    public String getMessage(String key, String... placeholders) {
        return applyAndTranslate(messages.getString(key, "&cMessage not found: " + key), placeholders);
    }

    /**
     * Like {@link #getMessage(String, String...)} but falls back to {@code def} when the key is
     * missing from messages.yml. Used by the GUIs so installations with an older messages.yml
     * (that predates the new keys) still show sensible text instead of "Message not found".
     */
    public String getMessageOr(String key, String def, String... placeholders) {
        return applyAndTranslate(messages.getString(key, def), placeholders);
    }

    private String applyAndTranslate(String message, String... placeholders) {
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Coloured display name for a rarity, configurable via the {@code rarities.<NAME>} section of
     * messages.yml. Falls back to the enum's built-in coloured name if not configured.
     */
    public String getRarityName(FishRarity rarity) {
        return ChatColor.translateAlternateColorCodes('&',
                messages.getString("rarities." + rarity.name(), rarity.getColoredDisplayName()));
    }

    /**
     * Human-readable duration such as "1h 5m 3s" / "45s", used for quest cooldown countdowns.
     */
    public String formatDuration(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (hours > 0 || minutes > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(secs).append("s");
        return sb.toString();
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
