package com.candyrealms.candycore.modules.masks;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MaskModule {

    private final ConfigManager configManager;

    public MaskModule(CandyCore plugin) {
        configManager = plugin.getConfigManager();
    }

    public boolean hasMask(Player player) {
        if(player.getInventory().getHelmet() == null) return false;

        ItemStack helmet = player.getInventory().getHelmet();

        if(helmet.getType() == Material.AIR || !helmet.hasItemMeta()
                || !helmet.getItemMeta().hasLore()) return false;

        return helmet.getItemMeta().getLore().contains(convertCode(configManager.getMasksAttachedLore()));
    }

    public boolean hasHydraMask(Player player) {
        if(!hasMask(player)) return false;
        if(hasFounderMask(player)) return true;

        ItemStack helmet = player.getInventory().getHelmet();

        return helmet.getItemMeta().getLore().contains(convertCode(configManager.getHydraLore()));
    }

    public boolean hasSlayerMask(Player player) {
        if(!hasMask(player)) return false;
        if(hasFounderMask(player)) return true;

        ItemStack helmet = player.getInventory().getHelmet();

        return helmet.getItemMeta().getLore().contains(convertCode(configManager.getSlayerLore()));
    }

    public boolean hasComboMask(Player player) {
        if(!hasMask(player)) return false;
        if(hasFounderMask(player)) return true;

        ItemStack helmet = player.getInventory().getHelmet();

        return helmet.getItemMeta().getLore().contains(convertCode(configManager.getComboLore()));
    }

    private boolean hasFounderMask(Player player) {
        if(!hasMask(player)) return false;

        ItemStack helmet = player.getInventory().getHelmet();

        return helmet.getItemMeta().getLore().contains(convertCode(configManager.getFounderLore()));
    }

    private String convertCode(String string) {
        return string.replace("&", "§");
    }
}
