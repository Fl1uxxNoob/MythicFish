package net.fliuxx.mythicFish.listeners;

import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.fish.Fish;
import net.fliuxx.mythicFish.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public class FishingListener implements Listener {

    private final MythicFish plugin;

    public FishingListener(MythicFish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        // Force retract fishing hook immediately for any fishing action to prevent spam
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH || 
            event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT ||
            event.getState() == PlayerFishEvent.State.REEL_IN) {

            // Force hook retraction by running a delayed task
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (event.getHook() != null && event.getHook().isValid()) {
                    event.getHook().remove();
                }
            });
        }

        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        // Check if player has permission to fish
        if (!player.hasPermission("mythicfish.fish")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessagesManager().getMessage("no-permission"));
            return;
        }

        // Get the biome where the player is fishing
        Biome biome = player.getLocation().getBlock().getBiome();

        // Get a random fish for this biome
        Fish caughtFish = plugin.getFishManager().getRandomFish(biome);

        if (caughtFish == null) {
            if (plugin.getConfigManager().isVanillaFishingDisabled()) {
                event.setCancelled(true);
                player.sendMessage(plugin.getMessagesManager().getMessage("no-fish-in-biome"));
            }
            return;
        }

        // Always cancel the event to prevent vanilla behavior
        event.setCancelled(true);

        // Create custom fish item
        ItemStack fishItem = createFishItem(caughtFish);

        // Add fish to player's collection and update statistics
        boolean isNewFish = !plugin.getDatabaseManager().hasPlayerCaughtFish(player.getUniqueId(), caughtFish.getId());

        // Always add the catch to database for statistics tracking and update cache
        plugin.getPlayerDataManager().addFishToPlayer(player.getUniqueId(), caughtFish.getId(), biome.name());

        if (isNewFish) {
            player.sendMessage(plugin.getMessagesManager().getMessage("new-fish-caught", 
                    "{fish}", caughtFish.getDisplayName(),
                    "{rarity}", caughtFish.getRarity().getColoredDisplayName()));

            // Check and complete quests ONLY for new fish
            plugin.getQuestManager().checkQuestCompletion(player, caughtFish);
        } else {
            player.sendMessage(plugin.getMessagesManager().getMessage("fish-caught",
                    "{fish}", caughtFish.getDisplayName(),
                    "{rarity}", caughtFish.getRarity().getColoredDisplayName()));
        }

        // Add fish item to player inventory
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(fishItem);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), fishItem);
        }

        // Give experience
        player.giveExp(1 + (int)(caughtFish.getRarity().ordinal() * 2));
    }

    private ItemStack createFishItem(Fish fish) {
        String coloredName = ChatColor.translateAlternateColorCodes('&', fish.getColor() + fish.getDisplayName());

        return new ItemBuilder(fish.getMaterial())
                .setDisplayName(coloredName)
                .addLoreLine(fish.getRarity().getColoredDisplayName())
                .addLoreLine("")
                .addLoreLine(ChatColor.GRAY + fish.getDescription())
                .addLoreLine("")
                .addLoreLine(ChatColor.DARK_GRAY + "Caught with MythicFish")
                .build();
    }
}