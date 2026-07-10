package net.fliuxx.mythicFish.gui;

import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.fish.Fish;
import net.fliuxx.mythicFish.player.PlayerData;
import net.fliuxx.mythicFish.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

public class CollectionGUI implements Listener {
    
    private final MythicFish plugin;
    private final Player player;
    private final Inventory inventory;
    private final Set<String> playerFish;
    
    public CollectionGUI(MythicFish plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.playerFish = plugin.getPlayerDataManager().getPlayerFish(player.getUniqueId());
        
        String title = ChatColor.translateAlternateColorCodes('&', 
                plugin.getMessagesManager().getMessage("collection-gui-title"));
        this.inventory = Bukkit.createInventory(null, 54, title);
        
        setupInventory();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    private void setupInventory() {
        List<Fish> allFish = plugin.getFishManager().getAllFish();
        
        int slot = 0;
        for (Fish fish : allFish) {
            if (slot == 49) slot++; // Skip bottom center slot for stats
            if (slot >= 54) break;
            
            ItemStack item;
            if (playerFish.contains(fish.getId())) {
                // Player has caught this fish - show full details
                item = createUnlockedFishItem(fish);
            } else {
                // Player hasn't caught this fish - show locked item
                item = createLockedFishItem(fish);
            }
            
            inventory.setItem(slot, item);
            slot++;
        }
        
        // Add player statistics item at bottom center (slot 49) - ONLY ONE
        inventory.setItem(49, createPlayerStatsItem());
        
        // Fill empty slots with glass panes, but use different colors for locked fish slots
        for (int i = 0; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                // Use gray glass panes for empty slots that don't represent fish
                inventory.setItem(i, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                        .setDisplayName(" ")
                        .build());
            }
        }
    }
    
    private ItemStack createUnlockedFishItem(Fish fish) {
        var messages = plugin.getMessagesManager();
        String coloredName = ChatColor.translateAlternateColorCodes('&', fish.getColor() + fish.getDisplayName());

        ItemBuilder builder = new ItemBuilder(fish.getMaterial())
                .setDisplayName(coloredName)
                .addLoreLine(messages.getRarityName(fish.getRarity()))
                .addLoreLine("");

        if (!fish.getDescription().isEmpty()) {
            builder.addLoreLine(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', fish.getDescription()));
            builder.addLoreLine("");
        }

        // Add biome information
        if (fish.getAllowedBiomes() != null && !fish.getAllowedBiomes().isEmpty()) {
            builder.addLoreLine(messages.getMessageOr("gui.collection.best-found", "&bBest found in:"));
            for (Biome biome : fish.getAllowedBiomes()) {
                builder.addLoreLine(messages.getMessageOr("gui.collection.biome-entry", "&3  • {biome}",
                        "{biome}", formatBiomeName(biome)));
            }
        }

        builder.addLoreLine("")
                .addLoreLine(messages.getMessageOr("gui.collection.unlocked", "&a✓ &7Unlocked"));

        return builder.build();
    }

    private ItemStack createLockedFishItem(Fish fish) {
        var messages = plugin.getMessagesManager();
        ItemBuilder builder = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setDisplayName(messages.getMessageOr("gui.collection.locked-name", "&8??? Locked Fish"))
                .addLoreLine(messages.getRarityName(fish.getRarity()))
                .addLoreLine("");

        // Show biome hint for locked fish
        if (fish.getAllowedBiomes() != null && !fish.getAllowedBiomes().isEmpty()) {
            builder.addLoreLine(messages.getMessageOr("gui.collection.can-be-found", "&bCan be found in:"));
            for (Biome biome : fish.getAllowedBiomes()) {
                builder.addLoreLine(messages.getMessageOr("gui.collection.biome-entry", "&3  • {biome}",
                        "{biome}", formatBiomeName(biome)));
            }
        } else {
            builder.addLoreLine(messages.getMessageOr("gui.collection.found-most-biomes", "&bCan be found in most biomes"));
        }

        builder.addLoreLine("")
                .addLoreLine(messages.getMessageOr("gui.collection.locked", "&c✗ &7Not unlocked"))
                .addLoreLine(messages.getMessageOr("gui.collection.locked-hint", "&7Catch this fish to unlock!"));

        return builder.build();
    }
    
    private ItemStack createPlayerStatsItem() {
        var messages = plugin.getMessagesManager();

        // Get player statistics from the in-memory cache
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
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
                .addLoreLine(messages.getMessageOr("gui.stats.collection-footer1", "&7Keep fishing to unlock more fish!"))
                .addLoreLine(messages.getMessageOr("gui.stats.collection-footer2", "&7Use /mfish quest to view quests"))
                .build();
    }
    
    private String formatBiomeName(Biome biome) {
        return biome.getKey().value().toLowerCase().replace('_', ' ').replace("biome", "").trim();
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

        // Prevent any clicking in the collection GUI
        // This is a view-only interface
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer() == player) {
            // Unregister this per-GUI listener to avoid leaking handler instances
            HandlerList.unregisterAll(this);
        }
    }
}
