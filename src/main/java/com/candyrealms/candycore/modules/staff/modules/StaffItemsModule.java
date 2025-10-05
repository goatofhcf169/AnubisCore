package com.candyrealms.candycore.modules.staff.modules;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.utils.ItemCreator;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class StaffItemsModule {

    private final CandyCore plugin;

    public StaffItemsModule(CandyCore plugin) {
        this.plugin = plugin;
    }

    public ItemStack getFreezeItem() {
        return createStaffItem("freeze-item", "freeze");
    }

    public ItemStack getVanishItem() {
        return createStaffItem("vanish-item", "vanish");
    }

    public ItemStack getRTPItem() {
        return createStaffItem("rtp-item", "rtp");
    }

    public ItemStack getInvseeItem() {
        return createStaffItem("invsee-item", "invsee");
    }

    private ItemStack createStaffItem(String configPath, String type) {
        FileConfiguration config = plugin.getStaffCFG().getConfig();

        String name = config.getString(configPath + ".name");
        Material material = Material.valueOf(config.getString(configPath + ".material"));
        int data = config.getInt(configPath + ".data");
        List<String> lore = config.getStringList(configPath + ".lore");
        String texture = config.getString(configPath + ".texture") == null ? "" : config.getString(configPath + ".texture");

        ItemStack itemStack = new ItemCreator(material, name, 1, data, texture, lore).getItem();

        NBTItem nbtItem = new NBTItem(itemStack);
        NBTCompound compound = nbtItem.getOrCreateCompound("CandyStaff");
        compound.setString("type", type);

        return nbtItem.getItem();
    }
}
