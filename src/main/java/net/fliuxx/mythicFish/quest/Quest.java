package net.fliuxx.mythicFish.quest;

import org.bukkit.Material;

import java.util.List;

public class Quest {
    
    private final String id;
    private final String displayName;
    private final String description;
    private final QuestType type;
    private final QuestTarget target;
    private final int requiredAmount;
    private final List<String> rewards;
    private final List<String> rewardDisplay;
    private final String rewardMessage;
    private final Material guiMaterial;
    private final String guiColor;
    
    public Quest(String id, String displayName, String description, QuestType type, 
                QuestTarget target, int requiredAmount, List<String> rewards, 
                List<String> rewardDisplay, String rewardMessage, Material guiMaterial, String guiColor) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.target = target;
        this.requiredAmount = requiredAmount;
        this.rewards = rewards;
        this.rewardDisplay = rewardDisplay;
        this.rewardMessage = rewardMessage;
        this.guiMaterial = guiMaterial;
        this.guiColor = guiColor;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public QuestType getType() {
        return type;
    }
    
    public QuestTarget getTarget() {
        return target;
    }
    
    public int getRequiredAmount() {
        return requiredAmount;
    }
    
    public List<String> getRewards() {
        return rewards;
    }
    
    public List<String> getRewardDisplay() {
        return rewardDisplay;
    }
    
    public String getRewardMessage() {
        return rewardMessage;
    }
    
    public Material getGuiMaterial() {
        return guiMaterial;
    }
    
    public String getGuiColor() {
        return guiColor;
    }
    
    public enum QuestType {
        CATCH_TOTAL,      // Catch X fish total
        CATCH_SPECIFIC,   // Catch specific fish
        CATCH_RARITY      // Catch X fish of specific rarity
    }
    
    public static class QuestTarget {
        private final String value;
        
        public QuestTarget(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
}