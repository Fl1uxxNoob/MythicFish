package net.fliuxx.mythicFish.player;

import net.fliuxx.mythicFish.MythicFish;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerDataManager {

    private final MythicFish plugin;
    private final Map<UUID, Set<String>> playerFishCache;

    public PlayerDataManager(MythicFish plugin) {
        this.plugin = plugin;
        this.playerFishCache = new HashMap<>();
    }

    public Set<String> getPlayerFish(Player player) {
        return getPlayerFish(player.getUniqueId());
    }

    public Set<String> getPlayerFish(UUID playerUUID) {
        if (!playerFishCache.containsKey(playerUUID)) {
            Set<String> fish = plugin.getDatabaseManager().getPlayerFish(playerUUID);
            playerFishCache.put(playerUUID, fish);
        }
        return playerFishCache.get(playerUUID);
    }

    public void addFishToPlayer(Player player, String fishId, String biome) {
        addFishToPlayer(player.getUniqueId(), fishId, biome);
    }

    public void addFishToPlayer(UUID playerUUID, String fishId, String biome) {
        plugin.getDatabaseManager().addFishToPlayer(playerUUID, fishId, biome);

        // Update cache - ensure cache exists and add the fish
        if (playerFishCache.containsKey(playerUUID)) {
            playerFishCache.get(playerUUID).add(fishId);
        } else {
            // If cache doesn't exist, invalidate it so it gets refreshed next time
            invalidateCache(playerUUID);
        }
    }

    public boolean hasPlayerCaughtFish(Player player, String fishId) {
        return hasPlayerCaughtFish(player.getUniqueId(), fishId);
    }

    public boolean hasPlayerCaughtFish(UUID playerUUID, String fishId) {
        return getPlayerFish(playerUUID).contains(fishId);
    }

    public void invalidateCache(UUID playerUUID) {
        playerFishCache.remove(playerUUID);
    }

    public void clearCache() {
        playerFishCache.clear();
    }

    public int getTotalFishCount(Player player) {
        return getPlayerFish(player).size();
    }

    public int getTotalFishCount(UUID playerUUID) {
        return getPlayerFish(playerUUID).size();
    }
}