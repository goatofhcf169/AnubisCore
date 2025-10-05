package com.candyrealms.candycore.modules.shards;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.utils.ItemCreator;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShardsModule {

    private final CandyCore plugin;

    private FileConfiguration config;

    public ShardsModule(CandyCore plugin) {
        this.plugin = plugin;

        config = plugin.getShardsCFG().getConfig();
    }

    public void giveShard(Player player, String name) {
        ConfigurationSection section = config.getConfigurationSection("shards." + name);

        List<String> itemLore = section.getStringList("lore");
        String enchantString = section.getString("enchantment");
        String itemName = section.getString("name");
        Material material = Material.valueOf(section.getString("material"));
        int data = section.getInt("data");

        ItemStack itemStack = new ItemCreator(material, itemName, 1, data, "", itemLore).getItem();

        NBTItem nbtItem = new NBTItem(itemStack);

        NBTCompound nbtCompound = nbtItem.addCompound("CandyShards");

        nbtCompound.setString("Enchantment", enchantString);

        player.getInventory().addItem(nbtItem.getItem());
    }

    /**
     * Simply checks if the path exist and if the enchantment is valid.
     * @param name the identifier for the shard.
     * @return
     */
    public boolean isValidShard(String name) {
        if(config.getConfigurationSection("shards." + name) == null) return false;

        ConfigurationSection section = config.getConfigurationSection("shards." + name);

        String[] splitString = section.getString("enchantment").split(":");

        return Enchantment.getByName(splitString[0]) != null;
    }

    public void reload() {
        config = plugin.getShardsCFG().getConfig();
    }
}
