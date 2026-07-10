package net.fliuxx.mythicFish.player;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, authoritative snapshot of a single online player's progression.
 * <p>
 * Reads on the main thread are served from this object (fast, no SQL); writes update it
 * synchronously and are persisted to the database asynchronously. It is populated from the
 * database when the player connects and discarded when they quit.
 * <p>
 * The collections are concurrent because the initial load happens on the async database thread
 * while gameplay reads happen on the main thread.
 */
public class PlayerData {

    private final Set<String> caughtFish = ConcurrentHashMap.newKeySet();
    private final Set<String> completedQuests = ConcurrentHashMap.newKeySet();
    private final Set<String> claimedQuests = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> questProgress = new ConcurrentHashMap<>();
    // Epoch seconds at which each quest was claimed; anchors the repeatable-quest cooldown.
    private final Map<String, Long> questClaimedAt = new ConcurrentHashMap<>();
    private volatile int totalCatches;
    private volatile String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getCaughtFish() {
        return caughtFish;
    }

    public boolean hasCaughtFish(String fishId) {
        return caughtFish.contains(fishId);
    }

    public void addCaughtFish(String fishId) {
        caughtFish.add(fishId);
    }

    public void removeCaughtFish(String fishId) {
        caughtFish.remove(fishId);
    }

    public int getUniqueFishCount() {
        return caughtFish.size();
    }

    public int getTotalCatches() {
        return totalCatches;
    }

    public void setTotalCatches(int totalCatches) {
        this.totalCatches = totalCatches;
    }

    public void incrementTotalCatches() {
        this.totalCatches++;
    }

    public Set<String> getCompletedQuests() {
        return completedQuests;
    }

    public boolean hasCompletedQuest(String questId) {
        return completedQuests.contains(questId);
    }

    public void markQuestCompleted(String questId) {
        completedQuests.add(questId);
    }

    public int getCompletedQuestCount() {
        return completedQuests.size();
    }

    public Set<String> getClaimedQuests() {
        return claimedQuests;
    }

    public boolean hasClaimedQuest(String questId) {
        return claimedQuests.contains(questId);
    }

    public void markQuestClaimed(String questId) {
        claimedQuests.add(questId);
        questClaimedAt.put(questId, System.currentTimeMillis() / 1000L);
    }

    public int getClaimedQuestCount() {
        return claimedQuests.size();
    }

    /** Epoch seconds when the quest was claimed, or 0 if unknown/never claimed. */
    public long getQuestClaimedAt(String questId) {
        return questClaimedAt.getOrDefault(questId, 0L);
    }

    public void setQuestClaimedAt(String questId, long epochSeconds) {
        questClaimedAt.put(questId, epochSeconds);
    }

    /**
     * Reset a single quest's progression so it can be completed again (used by the repeatable-quest
     * cooldown and by the admin reset command).
     */
    public void resetQuest(String questId) {
        completedQuests.remove(questId);
        claimedQuests.remove(questId);
        questProgress.remove(questId);
        questClaimedAt.remove(questId);
    }

    public int getQuestProgress(String questId) {
        return questProgress.getOrDefault(questId, 0);
    }

    public int incrementQuestProgress(String questId) {
        return questProgress.merge(questId, 1, Integer::sum);
    }

    public void setQuestProgress(String questId, int progress) {
        questProgress.put(questId, progress);
    }

    /**
     * Reset all cached progression to empty (used when an admin resets a player's collection).
     */
    public void clear() {
        caughtFish.clear();
        completedQuests.clear();
        claimedQuests.clear();
        questProgress.clear();
        questClaimedAt.clear();
        totalCatches = 0;
    }
}
