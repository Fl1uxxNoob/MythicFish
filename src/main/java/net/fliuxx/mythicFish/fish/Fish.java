package net.fliuxx.mythicFish.fish;

import org.bukkit.Material;
import org.bukkit.block.Biome;

import java.util.List;

public class Fish {
    
    private final String id;
    private final String displayName;
    private final String color;
    private final FishRarity rarity;
    private final double catchChance;
    private final List<Biome> allowedBiomes;
    private final List<Biome> restrictedBiomes;
    private final Material material;
    private final String description;
    
    public Fish(String id, String displayName, String color, FishRarity rarity, 
               double catchChance, List<Biome> allowedBiomes, List<Biome> restrictedBiomes,
               Material material, String description) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.rarity = rarity;
        this.catchChance = catchChance;
        this.allowedBiomes = allowedBiomes;
        this.restrictedBiomes = restrictedBiomes;
        this.material = material;
        this.description = description;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColor() {
        return color;
    }
    
    public FishRarity getRarity() {
        return rarity;
    }
    
    public double getCatchChance() {
        return catchChance;
    }
    
    public List<Biome> getAllowedBiomes() {
        return allowedBiomes;
    }
    
    public List<Biome> getRestrictedBiomes() {
        return restrictedBiomes;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean canBeCaughtInBiome(Biome biome) {
        if (restrictedBiomes != null && restrictedBiomes.contains(biome)) {
            return false;
        }
        
        if (allowedBiomes != null && !allowedBiomes.isEmpty()) {
            return allowedBiomes.contains(biome);
        }
        
        return true;
    }
}
