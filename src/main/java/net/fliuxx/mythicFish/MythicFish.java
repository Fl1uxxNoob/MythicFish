package net.fliuxx.mythicFish;

import net.fliuxx.mythicFish.commands.MythicFishCommand;
import net.fliuxx.mythicFish.config.ConfigManager;
import net.fliuxx.mythicFish.config.MessagesManager;
import net.fliuxx.mythicFish.database.DatabaseManager;
import net.fliuxx.mythicFish.fish.FishManager;
import net.fliuxx.mythicFish.listeners.FishingListener;
import net.fliuxx.mythicFish.listeners.PlayerConnectionListener;
import net.fliuxx.mythicFish.player.PlayerDataManager;
import net.fliuxx.mythicFish.quest.QuestManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MythicFish extends JavaPlugin {
    
    private static MythicFish instance;
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private DatabaseManager databaseManager;
    private FishManager fishManager;
    private PlayerDataManager playerDataManager;
    private QuestManager questManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.messagesManager = new MessagesManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.fishManager = new FishManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.questManager = new QuestManager(this);
        
        // Load configurations
        configManager.loadConfig();
        messagesManager.loadMessages();
        
        // Initialize database
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Load fish data and quests
        fishManager.loadFish();
        questManager.loadQuests();
        
        // Register events
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);

        // Register commands
        getCommand("mythicfish").setExecutor(new MythicFishCommand(this));

        // Register PlaceholderAPI expansion if the plugin is present and enabled
        if (configManager.isPlaceholderApiEnabled()
                && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new net.fliuxx.mythicFish.hook.MythicFishExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        // Preload data for players already online (e.g. after a /reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataManager.load(player.getUniqueId(), player.getName());
        }

        getLogger().info("MythicFish plugin has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info("MythicFish plugin has been disabled!");
    }
    
    public static MythicFish getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public FishManager getFishManager() {
        return fishManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public QuestManager getQuestManager() {
        return questManager;
    }
}
