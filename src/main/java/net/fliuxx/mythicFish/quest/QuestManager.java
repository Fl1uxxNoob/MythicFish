package net.fliuxx.mythicFish.quest;

import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.fish.Fish;
import net.fliuxx.mythicFish.fish.FishRarity;
import net.fliuxx.mythicFish.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class QuestManager {

    private final MythicFish plugin;
    private final Map<String, Quest> quests;

    public QuestManager(MythicFish plugin) {
        this.plugin = plugin;
        // LinkedHashMap preserves config order so GUI slot->quest mapping stays stable
        this.quests = new LinkedHashMap<>();
    }

    public void loadQuests() {
        quests.clear();

        ConfigurationSection questSection = plugin.getConfigManager().getConfig().getConfigurationSection("quests");
        if (questSection == null) {
            plugin.getLogger().warning("No quest configuration found!");
            return;
        }

        for (String questId : questSection.getKeys(false)) {
            ConfigurationSection questConfig = questSection.getConfigurationSection(questId);
            if (questConfig == null) continue;

            try {
                String displayName = questConfig.getString("display-name", questId);
                String description = questConfig.getString("description", "");
                Quest.QuestType type = Quest.QuestType.valueOf(questConfig.getString("type", "CATCH_TOTAL").toUpperCase());
                Quest.QuestTarget target = new Quest.QuestTarget(questConfig.getString("target", ""));
                int requiredAmount = questConfig.getInt("required-amount", 1);
                List<String> rewards = questConfig.getStringList("rewards");
                List<String> rewardDisplay = questConfig.getStringList("reward-display");
                String rewardMessage = questConfig.getString("reward-message", "&aQuest completed!");
                Material guiMaterial = Material.valueOf(questConfig.getString("gui-material", "PAPER").toUpperCase());
                String guiColor = questConfig.getString("gui-color", "&f");
                int cooldownSeconds = questConfig.getInt("cooldown-seconds", 0);

                Quest quest = new Quest(questId, displayName, description, type, target,
                                      requiredAmount, rewards, rewardDisplay, rewardMessage, guiMaterial, guiColor,
                                      cooldownSeconds);

                quests.put(questId, quest);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load quest '" + questId + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + quests.size() + " quests.");
    }

    public Quest getQuest(String id) {
        return quests.get(id);
    }

    public Collection<Quest> getAllQuests() {
        return quests.values();
    }

    /**
     * Reset any repeatable quest whose cooldown has elapsed since it was claimed, both in the cache
     * and in the database. Called lazily (on each catch and when the quest GUI is opened) so it stays
     * consistent with the cache-first design without needing a scheduled task.
     */
    public void applyQuestCooldowns(UUID playerUUID, PlayerData data) {
        if (data == null) {
            return;
        }
        long now = System.currentTimeMillis() / 1000L;
        for (Quest quest : quests.values()) {
            if (!quest.isRepeatable() || !data.hasClaimedQuest(quest.getId())) {
                continue;
            }
            long claimedAt = data.getQuestClaimedAt(quest.getId());
            if (claimedAt > 0 && now - claimedAt >= quest.getCooldownSeconds()) {
                data.resetQuest(quest.getId());
                plugin.getDatabaseManager().resetQuest(playerUUID, quest.getId());
            }
        }
    }

    /**
     * Remaining cooldown in seconds before a claimed repeatable quest becomes available again,
     * or 0 if it is ready / not on cooldown.
     */
    public long getRemainingCooldown(PlayerData data, Quest quest) {
        if (data == null || !quest.isRepeatable() || !data.hasClaimedQuest(quest.getId())) {
            return 0;
        }
        long claimedAt = data.getQuestClaimedAt(quest.getId());
        if (claimedAt <= 0) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() / 1000L - claimedAt;
        long remaining = quest.getCooldownSeconds() - elapsed;
        return Math.max(0, remaining);
    }

    public void checkQuestCompletion(Player player, Fish caughtFish) {
        UUID playerUUID = player.getUniqueId();
        PlayerData data = plugin.getPlayerDataManager().get(playerUUID);
        if (data == null) {
            return;
        }

        // Reset any repeatable quests whose cooldown has elapsed before evaluating this catch.
        applyQuestCooldowns(playerUUID, data);

        for (Quest quest : quests.values()) {
            if (data.hasCompletedQuest(quest.getId())) {
                continue;
            }

            boolean shouldComplete = false;

            switch (quest.getType()) {
                case CATCH_TOTAL:
                    if (quest.isRepeatable()) {
                        // Repeatable: count catches since the last reset so each cycle needs fresh catches,
                        // otherwise the lifetime total would instantly re-satisfy the quest after cooldown.
                        int totalProgress = data.incrementQuestProgress(quest.getId());
                        plugin.getDatabaseManager().updateQuestProgress(playerUUID, quest.getId(), 1);
                        shouldComplete = totalProgress >= quest.getRequiredAmount();
                    } else {
                        // One-time: lifetime total catches (repeats included), tracked in the cache
                        shouldComplete = data.getTotalCatches() >= quest.getRequiredAmount();
                    }
                    break;

                case CATCH_SPECIFIC:
                    if (caughtFish.getId().equals(quest.getTarget().getValue())) {
                        int specificCount = data.incrementQuestProgress(quest.getId());
                        plugin.getDatabaseManager().updateQuestProgress(playerUUID, quest.getId(), 1);
                        shouldComplete = specificCount >= quest.getRequiredAmount();
                    }
                    break;

                case CATCH_RARITY:
                    try {
                        FishRarity targetRarity = FishRarity.valueOf(quest.getTarget().getValue().toUpperCase());
                        if (caughtFish.getRarity() == targetRarity) {
                            int rarityProgress = data.incrementQuestProgress(quest.getId());
                            plugin.getDatabaseManager().updateQuestProgress(playerUUID, quest.getId(), 1);
                            shouldComplete = rarityProgress >= quest.getRequiredAmount();
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid rarity in quest " + quest.getId() + ": " + quest.getTarget().getValue());
                    }
                    break;
            }

            if (shouldComplete && !data.hasCompletedQuest(quest.getId())) {
                data.markQuestCompleted(quest.getId());
                plugin.getDatabaseManager().markQuestCompleted(playerUUID, quest.getId());

                String message = plugin.getMessagesManager().getMessage("quest-completed",
                        "{quest}", quest.getDisplayName());
                player.sendMessage(message);
            }
        }
    }

    public void giveQuestRewards(Player player, Quest quest) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null || !data.hasCompletedQuest(quest.getId())) {
            return;
        }

        if (data.hasClaimedQuest(quest.getId())) {
            player.sendMessage(plugin.getMessagesManager().getMessage("quest-already-claimed"));
            return;
        }

        for (String rewardCommand : quest.getRewards()) {
            String command = rewardCommand.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        data.markQuestClaimed(quest.getId());
        plugin.getDatabaseManager().setQuestClaimed(player.getUniqueId(), quest.getId());

        // Send custom reward message instead of generic one
        String rewardMessage = quest.getRewardMessage();
        if (rewardMessage != null && !rewardMessage.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', rewardMessage));
        } else {
            player.sendMessage(plugin.getMessagesManager().getMessage("quest-rewards-claimed",
                    "{quest}", quest.getDisplayName()));
        }
    }
}