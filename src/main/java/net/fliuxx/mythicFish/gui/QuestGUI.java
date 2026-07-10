package net.fliuxx.mythicFish.gui;

import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.player.PlayerData;
import net.fliuxx.mythicFish.quest.Quest;
import net.fliuxx.mythicFish.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
        // Reset any repeatable quests whose cooldown has elapsed before rendering.
        plugin.getQuestManager().applyQuestCooldowns(playerUUID, plugin.getPlayerDataManager().get(playerUUID));

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
        var messages = plugin.getMessagesManager();

        // Get player statistics from the in-memory cache
        PlayerData data = plugin.getPlayerDataManager().get(playerUUID);
        int totalFish = data != null ? data.getUniqueFishCount() : 0;
        int completedQuests = data != null ? data.getCompletedQuestCount() : 0;
        int claimedQuests = data != null ? data.getClaimedQuestCount() : 0;

        return new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(messages.getMessageOr("gui.stats.title", "&6&l{player}'s Statistics",
                        "{player}", player.getName()))
                .addLoreLine("")
                .addLoreLine(messages.getMessageOr("gui.stats.fish-collected", "&bFish Collected: &f{amount}",
                        "{amount}", String.valueOf(totalFish)))
                .addLoreLine(messages.getMessageOr("gui.stats.quests-completed", "&aQuests Completed: &f{amount}",
                        "{amount}", String.valueOf(completedQuests)))
                .addLoreLine(messages.getMessageOr("gui.stats.quests-claimed", "&6Quests Claimed: &f{amount}",
                        "{amount}", String.valueOf(claimedQuests)))
                .addLoreLine("")
                .addLoreLine(messages.getMessageOr("gui.stats.quest-footer", "&7Keep fishing to complete more quests!"))
                .build();
    }
    
    private ItemStack createQuestItem(Quest quest) {
        var messages = plugin.getMessagesManager();
        PlayerData data = plugin.getPlayerDataManager().get(playerUUID);
        boolean isCompleted = data != null && data.hasCompletedQuest(quest.getId());
        boolean isClaimed = data != null && data.hasClaimedQuest(quest.getId());

        String coloredName = ChatColor.translateAlternateColorCodes('&', quest.getGuiColor() + quest.getDisplayName());
        String required = String.valueOf(quest.getRequiredAmount());

        ItemBuilder builder = new ItemBuilder(quest.getGuiMaterial())
                .setDisplayName(coloredName)
                .addLoreLine("")
                .addLoreLine(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', quest.getDescription()))
                .addLoreLine("");

        // Add quest requirements
        builder.addLoreLine(messages.getMessageOr("gui.quest.requirement", "&eRequirement:"));
        switch (quest.getType()) {
            case CATCH_TOTAL:
                // Repeatable CATCH_TOTAL tracks per-cycle progress; one-time uses lifetime total.
                int currentTotal = data == null ? 0
                        : (quest.isRepeatable() ? data.getQuestProgress(quest.getId()) : data.getTotalCatches());
                builder.addLoreLine(messages.getMessageOr("gui.quest.req-total", "&fCatch {amount} fish total",
                                "{amount}", required))
                        .addLoreLine(messages.getMessageOr("gui.quest.progress", "&7Progress: {current}/{required}",
                                "{current}", String.valueOf(currentTotal), "{required}", required));
                break;
            case CATCH_SPECIFIC:
                int specificProgress = data != null ? data.getQuestProgress(quest.getId()) : 0;
                builder.addLoreLine(messages.getMessageOr("gui.quest.req-specific", "&fCatch {amount}x {target}",
                                "{amount}", required, "{target}", quest.getTarget().getValue()))
                        .addLoreLine(messages.getMessageOr("gui.quest.progress", "&7Progress: {current}/{required}",
                                "{current}", String.valueOf(specificProgress), "{required}", required));
                break;
            case CATCH_RARITY:
                try {
                    net.fliuxx.mythicFish.fish.FishRarity rarity = net.fliuxx.mythicFish.fish.FishRarity.valueOf(quest.getTarget().getValue().toUpperCase());
                    int rarityProgress = data != null ? data.getQuestProgress(quest.getId()) : 0;
                    builder.addLoreLine(messages.getMessageOr("gui.quest.req-rarity", "&fCatch {amount} {rarity} &ffish",
                                    "{amount}", required, "{rarity}", messages.getRarityName(rarity)))
                            .addLoreLine(messages.getMessageOr("gui.quest.progress", "&7Progress: {current}/{required}",
                                    "{current}", String.valueOf(rarityProgress), "{required}", required));
                } catch (IllegalArgumentException e) {
                    builder.addLoreLine(messages.getMessageOr("gui.quest.invalid", "&cInvalid quest configuration"));
                }
                break;
        }

        builder.addLoreLine("");

        // Add rewards info
        if (!quest.getRewardDisplay().isEmpty()) {
            builder.addLoreLine(messages.getMessageOr("gui.quest.rewards", "&6Rewards:"));
            for (String rewardDisplay : quest.getRewardDisplay()) {
                builder.addLoreLine(ChatColor.translateAlternateColorCodes('&', rewardDisplay));
            }
            builder.addLoreLine("");
        }

        // Add status
        if (isClaimed) {
            long remaining = plugin.getQuestManager().getRemainingCooldown(data, quest);
            if (quest.isRepeatable() && remaining > 0) {
                builder.addLoreLine(messages.getMessageOr("gui.quest.status-cooldown",
                        "&e⏳ &7Available again in &e{time}",
                        "{time}", messages.formatDuration(remaining)));
            } else {
                builder.addLoreLine(messages.getMessageOr("gui.quest.status-claimed", "&a✓ &7Completed & Claimed"));
            }
        } else if (isCompleted) {
            builder.addLoreLine(messages.getMessageOr("gui.quest.status-completed", "&a✓ &7Completed - Click to claim rewards!"));
        } else {
            builder.addLoreLine(messages.getMessageOr("gui.quest.status-incomplete", "&c✗ &7Not completed"));
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

        // Only process clicks inside the quest GUI itself, not the player's own inventory
        if (!inventory.equals(event.getClickedInventory())) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        int slot = event.getSlot();

        if (slot == 49) return; // Player stats item

        // Quests are placed sequentially in slots 0..n-1 (see setupInventory)
        List<Quest> allQuests = new ArrayList<>(plugin.getQuestManager().getAllQuests());
        if (slot >= 0 && slot < allQuests.size()) {
            Quest quest = allQuests.get(slot);

            PlayerData data = plugin.getPlayerDataManager().get(playerUUID);
            boolean isCompleted = data != null && data.hasCompletedQuest(quest.getId());
            boolean isClaimed = data != null && data.hasClaimedQuest(quest.getId());

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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer() == player) {
            // Unregister this per-GUI listener to avoid leaking handler instances
            HandlerList.unregisterAll(this);
        }
    }
}