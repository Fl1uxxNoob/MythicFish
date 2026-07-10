package net.fliuxx.mythicFish.listeners;

import net.fliuxx.mythicFish.MythicFish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Loads a player's {@link net.fliuxx.mythicFish.player.PlayerData} cache when they connect and
 * drops it when they leave. The load is triggered on {@link AsyncPlayerPreLoginEvent}, which already
 * runs off the main thread and completes before the player fully joins, so the cache is ready by the
 * time they can fish.
 */
public class PlayerConnectionListener implements Listener {

    private final MythicFish plugin;

    public PlayerConnectionListener(MythicFish plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        plugin.getPlayerDataManager().load(event.getUniqueId(), event.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().unload(event.getPlayer().getUniqueId());
    }
}
