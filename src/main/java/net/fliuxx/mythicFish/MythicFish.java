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
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MythicFish extends JavaPlugin {

    // bStats plugin id: https://bstats.org/plugin/bukkit/MythicFish/32521
    private static final int BSTATS_PLUGIN_ID = 32521;

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
        boolean placeholderApiHooked = false;
        if (configManager.isPlaceholderApiEnabled()
                && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new net.fliuxx.mythicFish.hook.MythicFishExpansion(this).register();
            placeholderApiHooked = true;
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        // Anonymous usage statistics (bStats)
        setupMetrics(placeholderApiHooked);

        // Preload data for players already online (e.g. after a /reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataManager.load(player.getUniqueId(), player.getName());
        }

        getLogger().info("MythicFish plugin has been enabled successfully!");
    }

    private void setupMetrics(boolean placeholderApiHooked) {
        if (!configManager.isMetricsEnabled()) {
            return;
        }
        if (BSTATS_PLUGIN_ID <= 0) {
            getLogger().info("bStats metrics are not active yet: set BSTATS_PLUGIN_ID "
                    + "(register at https://bstats.org).");
            return;
        }

        Metrics metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("fish_count",
                () -> String.valueOf(fishManager.getAllFish().size())));
        metrics.addCustomChart(new SimplePie("quest_count",
                () -> String.valueOf(questManager.getAllQuests().size())));
        metrics.addCustomChart(new SimplePie("async_database",
                () -> configManager.isAsyncDatabase() ? "enabled" : "disabled"));
        metrics.addCustomChart(new SimplePie("placeholderapi_hooked",
                () -> placeholderApiHooked ? "yes" : "no"));
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
