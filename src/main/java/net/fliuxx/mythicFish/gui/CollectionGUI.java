package net.fliuxx.mythicFish.gui;

import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.fish.Fish;
import net.fliuxx.mythicFish.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        this.playerFish = plugin.getDatabaseManager().getPlayerFish(player.getUniqueId());
        
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
        String coloredName = ChatColor.translateAlternateColorCodes('&', fish.getColor() + fish.getDisplayName());
        
        ItemBuilder builder = new ItemBuilder(fish.getMaterial())
                .setDisplayName(coloredName)
                .addLoreLine(fish.getRarity().getColoredDisplayName())
                .addLoreLine("");
        
        if (!fish.getDescription().isEmpty()) {
            builder.addLoreLine(ChatColor.GRAY + fish.getDescription());
            builder.addLoreLine("");
        }
        
        // Add biome information
        if (fish.getAllowedBiomes() != null && !fish.getAllowedBiomes().isEmpty()) {
            builder.addLoreLine(ChatColor.AQUA + "Best found in:");
            for (Biome biome : fish.getAllowedBiomes()) {
                builder.addLoreLine(ChatColor.DARK_AQUA + "  • " + formatBiomeName(biome));
            }
        }
        
        builder.addLoreLine("")
                .addLoreLine(ChatColor.GREEN + "✓ " + ChatColor.GRAY + "Unlocked");
        
        return builder.build();
    }
    
    private ItemStack createLockedFishItem(Fish fish) {
        ItemBuilder builder = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setDisplayName(ChatColor.DARK_GRAY + "??? Locked Fish")
                .addLoreLine(fish.getRarity().getColoredDisplayName())
                .addLoreLine("");
        
        // Show biome hint for locked fish
        if (fish.getAllowedBiomes() != null && !fish.getAllowedBiomes().isEmpty()) {
            builder.addLoreLine(ChatColor.AQUA + "Can be found in:");
            for (Biome biome : fish.getAllowedBiomes()) {
                builder.addLoreLine(ChatColor.DARK_AQUA + "  • " + formatBiomeName(biome));
            }
        } else {
            builder.addLoreLine(ChatColor.AQUA + "Can be found in most biomes");
        }
        
        builder.addLoreLine("")
                .addLoreLine(ChatColor.RED + "✗ " + ChatColor.GRAY + "Not unlocked")
                .addLoreLine(ChatColor.GRAY + "Catch this fish to unlock!");
        
        return builder.build();
    }
    
    private ItemStack createPlayerStatsItem() {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD)
                .setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + player.getName() + "'s Statistics");
        
        // Get player statistics - refresh cache first to ensure up-to-date data
        plugin.getPlayerDataManager().invalidateCache(player.getUniqueId());
        
        // Get unique fish count (collection size)
        Set<String> uniqueFish = plugin.getDatabaseManager().getPlayerFish(player.getUniqueId());
        int totalFish = uniqueFish.size();
        
        int completedQuests = plugin.getDatabaseManager().getCompletedQuestCount(player.getUniqueId());
        int claimedQuests = plugin.getDatabaseManager().getClaimedQuestCount(player.getUniqueId());
        
        builder.addLoreLine("")
                .addLoreLine(ChatColor.AQUA + "Fish Collected: " + ChatColor.WHITE + totalFish)
                .addLoreLine(ChatColor.GREEN + "Quests Completed: " + ChatColor.WHITE + completedQuests)
                .addLoreLine(ChatColor.GOLD + "Quests Claimed: " + ChatColor.WHITE + claimedQuests)
                .addLoreLine("")
                .addLoreLine(ChatColor.GRAY + "Keep fishing to unlock more fish!")
                .addLoreLine(ChatColor.GRAY + "Use /mfish quest to view quests");
        
        return builder.build();
    }
    
    private String formatBiomeName(Biome biome) {
        return biome.name().toLowerCase().replace('_', ' ').replace("biome", "").trim();
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
}
