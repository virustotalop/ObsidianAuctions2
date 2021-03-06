package com.gmail.virustotalop.obsidianauctions.language;

import com.clubobsidian.wrappy.Configuration;
import com.gmail.virustotalop.obsidianauctions.ObsidianAuctions;
import com.gmail.virustotalop.obsidianauctions.inject.annotation.I18nItemConfig;
import com.gmail.virustotalop.obsidianauctions.nbt.NBTCompound;
import com.gmail.virustotalop.obsidianauctions.util.MaterialUtil;
import com.google.inject.Inject;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class I18nTranslationFactory implements TranslationFactory {

    private final Map<Material, Collection<LanguageItem>> items;

    @Inject
    private I18nTranslationFactory(@I18nItemConfig Configuration config) {
        this.items = this.loadItems(config);
    }

    @Override
    public String getTranslation(ItemStack itemStack) {
        if(itemStack == null) {
            return null;
        }
        Material type = itemStack.getType();
        Collection<LanguageItem> items = this.items.get(type);
        if(items != null) {
            for(LanguageItem item : items) {
                if(item.matches(itemStack)) {
                    return item.getTranslation();
                }
            }
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        if(!itemMeta.hasLocalizedName()) {
            return MaterialUtil.formatName(type.name());
        }
        return itemMeta.getLocalizedName();
    }

    private Map<Material, Collection<LanguageItem>> loadItems(Configuration config) {
        Map<Material, Collection<LanguageItem>> map = new HashMap<>();
        for(String key : config.getKeys()) {
            String translation = config.getString(key);
            LanguageItem item = this.parseItem(key, translation);
            if(item != null) {
                Material material = item.getType();
                Collection<LanguageItem> items = map.computeIfAbsent(material, (col) -> new ArrayList<>());
                items.add(item);
            }
        }
        return map;
    }

    private LanguageItem parseItem(String key, String translation) {
        boolean hasSeparator = key.contains("<sep>");
        Material material;
        short durability = 0;
        NBTCompound compound = null;
        try {
            if(hasSeparator) {
                String[] split = key.split("<sep>");
                if(split.length != 2) {
                    ObsidianAuctions.get().getLogger().log(Level.SEVERE, "Invalid length for: " + key);
                    return null;
                }

                String first = split[0];
                String second = split[1];
                if(this.invalidMaterial(first)) {
                    return null;
                }
                material = Material.getMaterial(first);
                if(this.isShort(second)) {
                    durability = Short.parseShort(second);
                } else {
                    try {
                        compound = new NBTCompound(second);
                    } catch(Exception ex) {
                        ObsidianAuctions.get().getLogger().log(Level.SEVERE, "Invalid nbt: " + second);
                        ex.printStackTrace();
                    }
                }
            } else {
                if(this.invalidMaterial(key)) {
                    return null;
                }
                material = Material.valueOf(key);
            }
            if(material == null) {
                ObsidianAuctions.get().getLogger().log(Level.SEVERE, "No material found for: " + key);
                return null;
            }
            return new LanguageItem(material, durability, compound, translation);
        } catch(Exception ex) {
            ObsidianAuctions.get().getLogger().log(Level.SEVERE, "Invalid item: " + key);
            ex.printStackTrace();
            return null;
        }
    }

    private boolean invalidMaterial(String material) {
        return Material.getMaterial(material) == null;
    }

    private boolean isShort(String parse) {
        try {
            Short.parseShort(parse);
            return true;
        } catch(NumberFormatException ex) {
            return false;
        }
    }
}
