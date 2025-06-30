package net.fliuxx.mythicFish.fish;

import net.fliuxx.mythicFish.MythicFish;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.stream.Collectors;

public class FishManager {
    
    private final MythicFish plugin;
    private final Map<String, Fish> fishMap;
    private final List<Fish> allFish;
    
    public FishManager(MythicFish plugin) {
        this.plugin = plugin;
        this.fishMap = new HashMap<>();
        this.allFish = new ArrayList<>();
    }
    
    public void loadFish() {
        fishMap.clear();
        allFish.clear();
        
        ConfigurationSection fishSection = plugin.getConfigManager().getConfig().getConfigurationSection("fish");
        if (fishSection == null) {
            plugin.getLogger().warning("No fish configuration found!");
            return;
        }
        
        for (String fishId : fishSection.getKeys(false)) {
            ConfigurationSection fishConfig = fishSection.getConfigurationSection(fishId);
            if (fishConfig == null) continue;
            
            try {
                String displayName = fishConfig.getString("display-name", fishId);
                String color = fishConfig.getString("color", "&f");
                FishRarity rarity = FishRarity.valueOf(fishConfig.getString("rarity", "COMMON").toUpperCase());
                double catchChance = fishConfig.getDouble("catch-chance", 1.0);
                Material material = Material.valueOf(fishConfig.getString("material", "COD").toUpperCase());
                String description = fishConfig.getString("description", "");
                
                List<Biome> allowedBiomes = parseBiomes(fishConfig.getStringList("allowed-biomes"));
                List<Biome> restrictedBiomes = parseBiomes(fishConfig.getStringList("restricted-biomes"));
                
                Fish fish = new Fish(fishId, displayName, color, rarity, catchChance, 
                                   allowedBiomes, restrictedBiomes, material, description);
                
                fishMap.put(fishId, fish);
                allFish.add(fish);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load fish '" + fishId + "': " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + allFish.size() + " fish types.");
    }
    
    private List<Biome> parseBiomes(List<String> biomeNames) {
        return biomeNames.stream()
                .map(name -> {
                    try {
                        return Biome.valueOf(name.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid biome name: " + name);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    public Fish getFish(String id) {
        return fishMap.get(id);
    }
    
    public List<Fish> getAllFish() {
        return new ArrayList<>(allFish);
    }
    
    public List<Fish> getFishForBiome(Biome biome) {
        return allFish.stream()
                .filter(fish -> fish.canBeCaughtInBiome(biome))
                .collect(Collectors.toList());
    }
    
    public Fish getRandomFish(Biome biome) {
        List<Fish> availableFish = getFishForBiome(biome);
        if (availableFish.isEmpty()) {
            return null;
        }
        
        double totalWeight = availableFish.stream()
                .mapToDouble(fish -> fish.getCatchChance() * fish.getRarity().getBaseChance())
                .sum();
        
        double random = Math.random() * totalWeight;
        double currentWeight = 0;
        
        for (Fish fish : availableFish) {
            currentWeight += fish.getCatchChance() * fish.getRarity().getBaseChance();
            if (random <= currentWeight) {
                return fish;
            }
        }
        
        return availableFish.get(availableFish.size() - 1);
    }
    
    public Map<String, Fish> getFishMap() {
        return new HashMap<>(fishMap);
    }
}
