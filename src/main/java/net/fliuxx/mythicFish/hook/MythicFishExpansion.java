package net.fliuxx.mythicFish.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.player.PlayerData;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion exposing per-player MythicFish stats. Values are read from the in-memory
 * cache, so placeholders resolve instantly without touching the database.
 * <p>
 * Placeholders: {@code %mythicfish_total_catches%}, {@code %mythicfish_unique_fish%},
 * {@code %mythicfish_completed_quests%}, {@code %mythicfish_claimed_quests%}.
 */
public class MythicFishExpansion extends PlaceholderExpansion {

    private final MythicFish plugin;

    public MythicFishExpansion(MythicFish plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mythicfish";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Fl1uxxNoob";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        // Keep the expansion registered across PlaceholderAPI reloads
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) {
            // Player is offline / not cached
            return "0";
        }

        return switch (params.toLowerCase()) {
            case "total_catches" -> String.valueOf(data.getTotalCatches());
            case "unique_fish" -> String.valueOf(data.getUniqueFishCount());
            case "completed_quests" -> String.valueOf(data.getCompletedQuestCount());
            case "claimed_quests" -> String.valueOf(data.getClaimedQuestCount());
            default -> null;
        };
    }
}
