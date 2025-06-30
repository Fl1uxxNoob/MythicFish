package net.fliuxx.mythicFish.fish;

import org.bukkit.ChatColor;

public enum FishRarity {
    COMMON("Common", ChatColor.WHITE, 1.0),
    UNCOMMON("Uncommon", ChatColor.GREEN, 0.7),
    RARE("Rare", ChatColor.BLUE, 0.4),
    EPIC("Epic", ChatColor.DARK_PURPLE, 0.15),
    LEGENDARY("Legendary", ChatColor.GOLD, 0.05);
    
    private final String displayName;
    private final ChatColor color;
    private final double baseChance;
    
    FishRarity(String displayName, ChatColor color, double baseChance) {
        this.displayName = displayName;
        this.color = color;
        this.baseChance = baseChance;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    public double getBaseChance() {
        return baseChance;
    }
    
    public String getColoredDisplayName() {
        return color + displayName;
    }
}
