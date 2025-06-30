package net.fliuxx.mythicFish.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {
    
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    
    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }
    
    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }
    
    public ItemBuilder setDisplayName(String displayName) {
        if (itemMeta != null) {
            itemMeta.setDisplayName(displayName);
        }
        return this;
    }
    
    public ItemBuilder setLore(List<String> lore) {
        if (itemMeta != null) {
            itemMeta.setLore(lore);
        }
        return this;
    }
    
    public ItemBuilder setLore(String... lore) {
        return setLore(Arrays.asList(lore));
    }
    
    public ItemBuilder addLoreLine(String line) {
        if (itemMeta != null) {
            List<String> lore = itemMeta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(line);
            itemMeta.setLore(lore);
        }
        return this;
    }
    
    public ItemBuilder addLoreLines(String... lines) {
        for (String line : lines) {
            addLoreLine(line);
        }
        return this;
    }
    
    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        if (itemMeta != null) {
            itemMeta.addEnchant(enchantment, level, true);
        }
        return this;
    }
    
    public ItemBuilder addItemFlags(ItemFlag... flags) {
        if (itemMeta != null) {
            itemMeta.addItemFlags(flags);
        }
        return this;
    }
    
    public ItemBuilder setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }
    
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
        }
        return this;
    }
    
    public ItemBuilder hideAttributes() {
        return addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }
    
    public ItemBuilder hideEnchants() {
        return addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
    
    public ItemBuilder hideAll() {
        return addItemFlags(ItemFlag.values());
    }
    
    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}
