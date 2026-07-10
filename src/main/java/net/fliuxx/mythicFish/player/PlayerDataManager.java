package net.fliuxx.mythicFish.player;

import net.fliuxx.mythicFish.MythicFish;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the in-memory {@link PlayerData} cache for every online player.
 * <p>
 * The cache is the authoritative source for gameplay reads on the main thread. Data is loaded
 * asynchronously when a player connects ({@link #load(UUID, String)}) and dropped when they quit
 * ({@link #unload(UUID)}). Writes update the cache immediately and are persisted asynchronously by
 * the callers through {@code DatabaseManager}.
 */
public class PlayerDataManager {

    private final MythicFish plugin;
    private final Map<UUID, PlayerData> cache;

    public PlayerDataManager(MythicFish plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Load a player's data from the database (async) into the cache. Safe to call off the main thread.
     */
    public void load(UUID playerUUID, String name) {
        plugin.getDatabaseManager().loadPlayerData(playerUUID).thenAccept(data -> {
            data.setName(name);
            cache.put(playerUUID, data);
        });
    }

    public void unload(UUID playerUUID) {
        cache.remove(playerUUID);
    }

    /**
     * Return the cached data for a player, or {@code null} if they are not loaded (offline).
     */
    public PlayerData get(UUID playerUUID) {
        return cache.get(playerUUID);
    }

    public PlayerData get(Player player) {
        return get(player.getUniqueId());
    }

    public boolean isLoaded(UUID playerUUID) {
        return cache.containsKey(playerUUID);
    }

    public void clearCache() {
        cache.clear();
    }

    // --- Convenience accessors used across the plugin (all read from cache) ---

    public Set<String> getPlayerFish(UUID playerUUID) {
        PlayerData data = cache.get(playerUUID);
        return data != null ? data.getCaughtFish() : Set.of();
    }

    public Set<String> getPlayerFish(Player player) {
        return getPlayerFish(player.getUniqueId());
    }

    public boolean hasPlayerCaughtFish(UUID playerUUID, String fishId) {
        PlayerData data = cache.get(playerUUID);
        return data != null && data.hasCaughtFish(fishId);
    }

    public boolean hasPlayerCaughtFish(Player player, String fishId) {
        return hasPlayerCaughtFish(player.getUniqueId(), fishId);
    }

    public int getTotalFishCount(UUID playerUUID) {
        PlayerData data = cache.get(playerUUID);
        return data != null ? data.getUniqueFishCount() : 0;
    }

    public int getTotalFishCount(Player player) {
        return getTotalFishCount(player.getUniqueId());
    }
}
