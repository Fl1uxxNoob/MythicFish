package net.fliuxx.mythicFish.listeners;

import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.fish.Fish;
import net.fliuxx.mythicFish.player.PlayerData;
import net.fliuxx.mythicFish.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

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

        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getPlayerDataManager().get(uuid);

        // Determine novelty from the in-memory cache (no DB read on the main thread)
        boolean isNewFish = data == null || !data.hasCaughtFish(caughtFish.getId());

        // Update the cache immediately, then persist asynchronously
        if (data != null) {
            data.addCaughtFish(caughtFish.getId());
            data.incrementTotalCatches();
        }
        plugin.getDatabaseManager().addFishToPlayer(uuid, caughtFish.getId(), biome.getKey().toString());
        plugin.getDatabaseManager().incrementTotalCatches(uuid, player.getName());

        String rarityName = plugin.getMessagesManager().getRarityName(caughtFish.getRarity());
        if (isNewFish) {
            player.sendMessage(plugin.getMessagesManager().getMessage("new-fish-caught",
                    "{fish}", caughtFish.getDisplayName(),
                    "{rarity}", rarityName));
        } else {
            player.sendMessage(plugin.getMessagesManager().getMessage("fish-caught",
                    "{fish}", caughtFish.getDisplayName(),
                    "{rarity}", rarityName));
        }

        // Server-wide announcement for rare catches
        announceRareCatch(player, caughtFish, isNewFish);

        // Check and progress quests on every catch (repeats count toward totals)
        plugin.getQuestManager().checkQuestCompletion(player, caughtFish);

        // Add fish item to player inventory
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(fishItem);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), fishItem);
        }

        // Give experience
        player.giveExp(1 + (int)(caughtFish.getRarity().ordinal() * 2));
    }

    private void announceRareCatch(Player player, Fish fish, boolean isNewFish) {
        if (!plugin.getConfigManager().areAnnouncementsEnabled()) {
            return;
        }
        if (plugin.getConfigManager().isAnnounceOnlyFirstCatch() && !isNewFish) {
            return;
        }
        List<String> rarities = plugin.getConfigManager().getAnnouncementRarities();
        if (!rarities.contains(fish.getRarity().name())) {
            return;
        }

        String message = plugin.getMessagesManager().getMessage("announcement-rare-catch",
                "{player}", player.getName(),
                "{fish}", fish.getDisplayName(),
                "{rarity}", plugin.getMessagesManager().getRarityName(fish.getRarity()));
        Bukkit.broadcastMessage(message);

        // Optional broadcast sound, referenced by its sound key (e.g. "entity.player.levelup")
        String soundKey = plugin.getConfigManager().getAnnouncementSound();
        if (soundKey != null && !soundKey.isEmpty()) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.playSound(online.getLocation(), soundKey, 1.0f, 1.0f);
            }
        }
    }

    private ItemStack createFishItem(Fish fish) {
        String coloredName = ChatColor.translateAlternateColorCodes('&', fish.getColor() + fish.getDisplayName());
        String description = ChatColor.translateAlternateColorCodes('&', fish.getDescription());
        String caughtWith = plugin.getMessagesManager().getMessageOr("fish-item.caught-with",
                "&8Caught with MythicFish");

        return new ItemBuilder(fish.getMaterial())
                .setDisplayName(coloredName)
                .addLoreLine(plugin.getMessagesManager().getRarityName(fish.getRarity()))
                .addLoreLine("")
                .addLoreLine(ChatColor.GRAY + description)
                .addLoreLine("")
                .addLoreLine(caughtWith)
                .build();
    }
}