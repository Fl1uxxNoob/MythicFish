package net.fliuxx.mythicFish.commands;

import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.gui.CollectionGUI;
import net.fliuxx.mythicFish.gui.QuestGUI;
import net.fliuxx.mythicFish.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MythicFishCommand implements CommandExecutor, TabCompleter {
    
    private final MythicFish plugin;
    
    public MythicFishCommand(MythicFish plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("help-message"));
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "collection":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getMessagesManager().getMessage("players-only"));
                    return true;
                }
                
                if (!player.hasPermission("mythicfish.collection")) {
                    player.sendMessage(plugin.getMessagesManager().getMessage("no-permission"));
                    return true;
                }
                
                new CollectionGUI(plugin, player).open();
                break;
                
            case "quest":
            case "quests":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getMessagesManager().getMessage("players-only"));
                    return true;
                }
                
                if (!player.hasPermission("mythicfish.quest")) {
                    player.sendMessage(plugin.getMessagesManager().getMessage("no-permission"));
                    return true;
                }
                
                new QuestGUI(plugin, player).open();
                break;
                
            case "top":
            case "leaderboard":
                if (!sender.hasPermission("mythicfish.top")) {
                    sender.sendMessage(plugin.getMessagesManager().getMessage("no-permission"));
                    return true;
                }
                handleTopCommand(sender);
                break;

            case "credits":
                // Available to everyone, no permission required
                sender.sendMessage(plugin.getMessagesManager().getMessage("credits-message"));
                break;

            case "admin":
                if (!sender.hasPermission("mythicfish.admin")) {
                    sender.sendMessage(plugin.getMessagesManager().getMessage("no-permission"));
                    return true;
                }

                handleAdminCommand(sender, args);
                break;
                
            case "reload":
                if (!sender.hasPermission("mythicfish.admin.reload")) {
                    sender.sendMessage(plugin.getMessagesManager().getMessage("no-permission"));
                    return true;
                }
                
                plugin.getConfigManager().reloadConfig();
                plugin.getMessagesManager().reloadMessages();
                plugin.getFishManager().loadFish();
                plugin.getQuestManager().loadQuests();
                sender.sendMessage(plugin.getMessagesManager().getMessage("plugin-reloaded"));
                break;
                
            case "help":
            default:
                sender.sendMessage(plugin.getMessagesManager().getMessage("help-message"));
                break;
        }
        
        return true;
    }
    
    private void handleTopCommand(CommandSender sender) {
        if (!plugin.getConfigManager().isLeaderboardEnabled()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("leaderboard-disabled"));
            return;
        }

        int size = plugin.getConfigManager().getLeaderboardSize();
        plugin.getDatabaseManager().getTopCatches(size).thenAccept(top ->
                // Hop back to the main thread to send messages
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (top.isEmpty()) {
                        sender.sendMessage(plugin.getMessagesManager().getMessage("leaderboard-empty"));
                        return;
                    }
                    sender.sendMessage(plugin.getMessagesManager().getMessage("leaderboard-header"));
                    int rank = 1;
                    for (var entry : top) {
                        sender.sendMessage(plugin.getMessagesManager().getMessage("leaderboard-line",
                                "{rank}", String.valueOf(rank),
                                "{player}", entry.name() != null ? entry.name() : "Unknown",
                                "{catches}", String.valueOf(entry.catches())));
                        rank++;
                    }
                }));
    }

    private void handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("admin-help"));
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "setstats":
                handleSetStatsCommand(sender, args);
                break;
            case "give":
                handleGiveFishCommand(sender, args);
                break;
            case "remove":
                handleRemoveFishCommand(sender, args);
                break;
            case "reset":
                handleResetCommand(sender, args);
                break;
            case "resetquest":
                handleResetQuestCommand(sender, args);
                break;
            default:
                sender.sendMessage(plugin.getMessagesManager().getMessage("admin-help"));
                break;
        }
    }

    private void handleResetQuestCommand(CommandSender sender, String[] args) {
        // /mfish admin resetquest <player> <questId>
        if (args.length < 4) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("admin-resetquest-usage"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("player-not-found", "{player}", args[2]));
            return;
        }

        String questId = args[3];
        if (plugin.getQuestManager().getQuest(questId) == null) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("quest-not-found", "{quest}", questId));
            return;
        }

        plugin.getDatabaseManager().resetQuest(target.getUniqueId(), questId);
        if (target.isOnline()) {
            PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
            if (data != null) {
                data.resetQuest(questId);
            }
        }

        sender.sendMessage(plugin.getMessagesManager().getMessage("admin-quest-reset",
                "{player}", target.getName(), "{quest}", questId));
    }
    
    private void handleSetStatsCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("admin-setstats-usage"));
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("player-not-found", "{player}", args[2]));
            return;
        }
        
        String action = args[3].toLowerCase();
        
        switch (action) {
            case "give":
                if (args.length < 5) {
                    sender.sendMessage(plugin.getMessagesManager().getMessage("admin-give-usage"));
                    return;
                }
                String fishId = args[4];
                if (plugin.getFishManager().getFish(fishId) == null) {
                    sender.sendMessage(plugin.getMessagesManager().getMessage("fish-not-found", "{fish}", fishId));
                    return;
                }
                
                plugin.getDatabaseManager().addFishToPlayer(target.getUniqueId(), fishId, "ADMIN_GIVEN");
                plugin.getDatabaseManager().incrementTotalCatches(target.getUniqueId(), target.getName());

                // Update the live cache and quest progress if the target is online
                if (target.isOnline()) {
                    Player onlinePlayer = (Player) target;
                    PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
                    if (data != null) {
                        data.addCaughtFish(fishId);
                        data.incrementTotalCatches();
                    }
                    net.fliuxx.mythicFish.fish.Fish fish = plugin.getFishManager().getFish(fishId);
                    if (fish != null) {
                        plugin.getQuestManager().checkQuestCompletion(onlinePlayer, fish);
                    }
                }

                sender.sendMessage(plugin.getMessagesManager().getMessage("admin-fish-given",
                        "{player}", target.getName(), "{fish}", fishId));
                break;

            case "remove":
                if (args.length < 5) {
                    sender.sendMessage(plugin.getMessagesManager().getMessage("admin-remove-usage"));
                    return;
                }
                String removeFishId = args[4];

                plugin.getDatabaseManager().removeFishFromPlayer(target.getUniqueId(), removeFishId);
                if (target.isOnline()) {
                    PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
                    if (data != null) {
                        data.removeCaughtFish(removeFishId);
                    }
                }
                sender.sendMessage(plugin.getMessagesManager().getMessage("admin-fish-removed",
                        "{player}", target.getName(), "{fish}", removeFishId));
                break;

            case "reset":
                plugin.getDatabaseManager().resetPlayerCollection(target.getUniqueId());
                if (target.isOnline()) {
                    PlayerData data = plugin.getPlayerDataManager().get(target.getUniqueId());
                    if (data != null) {
                        data.clear();
                    }
                }
                sender.sendMessage(plugin.getMessagesManager().getMessage("admin-collection-reset",
                        "{player}", target.getName()));
                break;
                
            default:
                sender.sendMessage(plugin.getMessagesManager().getMessage("admin-setstats-usage"));
                break;
        }
    }
    
    private void handleGiveFishCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("admin-give-usage"));
            return;
        }
        handleSetStatsCommand(sender, new String[]{"admin", "setstats", args[2], "give", args[3]});
    }

    private void handleRemoveFishCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("admin-remove-usage"));
            return;
        }
        handleSetStatsCommand(sender, new String[]{"admin", "setstats", args[2], "remove", args[3]});
    }

    private void handleResetCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessagesManager().getMessage("admin-reset-usage"));
            return;
        }
        handleSetStatsCommand(sender, new String[]{"admin", "setstats", args[2], "reset"});
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("mythicfish.collection")) {
                completions.add("collection");
            }
            if (sender.hasPermission("mythicfish.quest")) {
                completions.add("quest");
            }
            if (sender.hasPermission("mythicfish.top")) {
                completions.add("top");
            }
            if (sender.hasPermission("mythicfish.admin")) {
                completions.add("admin");
            }
            if (sender.hasPermission("mythicfish.admin.reload")) {
                completions.add("reload");
            }
            completions.add("credits");
            completions.add("help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            if (sender.hasPermission("mythicfish.admin")) {
                completions.addAll(Arrays.asList("setstats", "give", "remove", "reset", "resetquest"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            // Player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("setstats")) {
            completions.addAll(Arrays.asList("give", "remove", "reset"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("resetquest")) {
            // Quest IDs
            for (var quest : plugin.getQuestManager().getAllQuests()) {
                completions.add(quest.getId());
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin") &&
                  (args[1].equalsIgnoreCase("setstats") || args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("remove"))) {
            // Fish IDs
            completions.addAll(plugin.getFishManager().getFishMap().keySet());
        }
        
        return completions;
    }
}
