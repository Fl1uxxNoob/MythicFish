package net.fliuxx.mythicFish.gui;

import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.quest.Quest;
import net.fliuxx.mythicFish.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestGUI implements Listener {
    
    private final MythicFish plugin;
    private final Player player;
    private final Inventory inventory;
    private final UUID playerUUID;
    
    public QuestGUI(MythicFish plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.playerUUID = player.getUniqueId();
        
        String title = ChatColor.translateAlternateColorCodes('&', 
                plugin.getMessagesManager().getMessage("quest-gui-title"));
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        setupInventory();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    private void setupInventory() {
        // Quest items
        List<Quest> allQuests = new ArrayList<>(plugin.getQuestManager().getAllQuests());
        int slot = 0;
        
        for (Quest quest : allQuests) {
            if (slot == 49) slot++; // Skip bottom center slot for stats
            if (slot >= 54) break;
            
            ItemStack questItem = createQuestItem(quest);
            inventory.setItem(slot, questItem);
            slot++;
        }
        
        // Player statistics item at bottom center (slot 49)
        inventory.setItem(49, createPlayerStatsItem());
        
        // Fill empty slots with glass panes
        for (int i = 0; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .setDisplayName(" ")
                        .build());
            }
        }
    }
    
    private ItemStack createPlayerStatsItem() {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + player.getName() + "'s Statistics");
        
        // Get player statistics
        int totalFish = plugin.getPlayerDataManager().getTotalFishCount(playerUUID);
        int completedQuests = plugin.getDatabaseManager().getCompletedQuestCount(playerUUID);
        int claimedQuests = plugin.getDatabaseManager().getClaimedQuestCount(playerUUID);
        
        builder.addLoreLine("")
                .addLoreLine(ChatColor.AQUA + "Fish Collected: " + ChatColor.WHITE + totalFish)
                .addLoreLine(ChatColor.GREEN + "Quests Completed: " + ChatColor.WHITE + completedQuests)
                .addLoreLine(ChatColor.GOLD + "Quests Claimed: " + ChatColor.WHITE + claimedQuests)
                .addLoreLine("")
                .addLoreLine(ChatColor.GRAY + "Keep fishing to complete more quests!");
        
        return builder.build();
    }
    
    private ItemStack createQuestItem(Quest quest) {
        boolean isCompleted = plugin.getDatabaseManager().hasPlayerCompletedQuest(playerUUID, quest.getId());
        boolean isClaimed = plugin.getDatabaseManager().hasPlayerClaimedQuest(playerUUID, quest.getId());
        
        String coloredName = ChatColor.translateAlternateColorCodes('&', quest.getGuiColor() + quest.getDisplayName());
        
        ItemBuilder builder = new ItemBuilder(quest.getGuiMaterial())
                .setDisplayName(coloredName)
                .addLoreLine("")
                .addLoreLine(ChatColor.GRAY + quest.getDescription())
                .addLoreLine("");
        
        // Add quest requirements
        builder.addLoreLine(ChatColor.YELLOW + "Requirement:");
        switch (quest.getType()) {
            case CATCH_TOTAL:
                int currentTotal = plugin.getPlayerDataManager().getTotalFishCount(playerUUID);
                builder.addLoreLine(ChatColor.WHITE + "Catch " + quest.getRequiredAmount() + " fish total")
                        .addLoreLine(ChatColor.GRAY + "Progress: " + currentTotal + "/" + quest.getRequiredAmount());
                break;
            case CATCH_SPECIFIC:
                boolean hasFish = plugin.getDatabaseManager().hasPlayerCaughtFish(playerUUID, quest.getTarget().getValue());
                builder.addLoreLine(ChatColor.WHITE + "Catch: " + quest.getTarget().getValue())
                        .addLoreLine(ChatColor.GRAY + "Status: " + (hasFish ? ChatColor.GREEN + "✓ Caught" : ChatColor.RED + "✗ Not caught"));
                break;
            case CATCH_RARITY:
                try {
                    net.fliuxx.mythicFish.fish.FishRarity rarity = net.fliuxx.mythicFish.fish.FishRarity.valueOf(quest.getTarget().getValue().toUpperCase());
                    int rarityCount = plugin.getDatabaseManager().getPlayerFishCountByRarity(playerUUID, rarity);
                    builder.addLoreLine(ChatColor.WHITE + "Catch " + quest.getRequiredAmount() + " " + rarity.getDisplayName() + " fish")
                            .addLoreLine(ChatColor.GRAY + "Progress: " + rarityCount + "/" + quest.getRequiredAmount());
                } catch (IllegalArgumentException e) {
                    builder.addLoreLine(ChatColor.RED + "Invalid quest configuration");
                }
                break;
        }
        
        builder.addLoreLine("");
        
        // Add rewards info
        if (!quest.getRewardDisplay().isEmpty()) {
            builder.addLoreLine(ChatColor.GOLD + "Rewards:");
            for (String rewardDisplay : quest.getRewardDisplay()) {
                builder.addLoreLine(ChatColor.translateAlternateColorCodes('&', rewardDisplay));
            }
            builder.addLoreLine("");
        }
        
        // Add status
        if (isClaimed) {
            builder.addLoreLine(ChatColor.GREEN + "✓ " + ChatColor.GRAY + "Completed & Claimed");
        } else if (isCompleted) {
            builder.addLoreLine(ChatColor.GREEN + "✓ " + ChatColor.GRAY + "Completed - Click to claim rewards!");
        } else {
            builder.addLoreLine(ChatColor.RED + "✗ " + ChatColor.GRAY + "Not completed");
        }
        
        return builder.build();
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }
        
        event.setCancelled(true);
        
        if (event.getWhoClicked() != player) {
            return;
        }
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }
        
        // Find the quest for this slot
        List<Quest> allQuests = new ArrayList<>(plugin.getQuestManager().getAllQuests());
        int slot = event.getSlot();
        
        if (slot == 22) return; // Player stats item
        
        int questIndex = slot > 22 ? slot - 1 : slot;
        if (questIndex >= 0 && questIndex < allQuests.size()) {
            Quest quest = allQuests.get(questIndex);
            
            boolean isCompleted = plugin.getDatabaseManager().hasPlayerCompletedQuest(playerUUID, quest.getId());
            boolean isClaimed = plugin.getDatabaseManager().hasPlayerClaimedQuest(playerUUID, quest.getId());
            
            if (isCompleted && !isClaimed) {
                plugin.getQuestManager().giveQuestRewards(player, quest);
                // Refresh the GUI
                setupInventory();
            } else if (isClaimed) {
                player.sendMessage(plugin.getMessagesManager().getMessage("quest-already-claimed"));
            } else {
                player.sendMessage(plugin.getMessagesManager().getMessage("quest-not-completed"));
            }
        }
    }
}