package com.candyrealms.candycore.utils;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Getter
public class ItemCreator {

    private ItemStack item;

    public ItemCreator(Material material) {
        item = new ItemStack(material);
    }

    public ItemCreator(Material material, String name, String... lore) {
        this(material, name, 1, 0, null, lore);
    }

    public ItemCreator(Material material, String name, int amount, int data, String texture, String... lore) {
        item = new ItemStack(material, amount, (short) data);
        assignName(name);
        assignLore(Arrays.asList(lore));

        if(texture != null && !texture.isEmpty() && (data == 3 || data == 0) && CompatUtil.isSkull(material)) {
            assignTexture(texture);
        }
    }

    public ItemCreator(Material material, String name, int amount, int data, String texture, List<String> lore) {
        item = new ItemStack(material, amount, (short) data);
        assignName(name);
        assignLore(lore);

        if(texture != null && !texture.isEmpty() && (data == 3 || data == 0) && CompatUtil.isSkull(material)) {
            assignTexture(texture);
        }
    }

    public ItemStack setGlow() {
        item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);

        ItemMeta itemMeta = item.getItemMeta();

        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(itemMeta);

        return item;
    }

    private void assignLore(List<String> loreList) {
        List<String> updatedLore = new ArrayList<>();

        loreList.forEach(string -> updatedLore.add(ColorUtil.color(string)));

        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setLore(updatedLore);
        item.setItemMeta(itemMeta);
    }

    private void assignName(String name) {
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ColorUtil.color(name));

        item.setItemMeta(itemMeta);
    }

    public void updateLore(Function<String, String> replace) {
        ItemMeta itemMeta = getItem().getItemMeta();;

        List<String> updatedLore = new ArrayList<>();

        for(String string : itemMeta.getLore()) {
            updatedLore.add(replace.apply(string));
        }

        itemMeta.setLore(updatedLore);
        getItem().setItemMeta(itemMeta);
    }

    public void updateName(Function<String, String> replace) {
        ItemMeta itemMeta = getItem().getItemMeta();

        itemMeta.setDisplayName(replace.apply(itemMeta.getDisplayName()));

        getItem().setItemMeta(itemMeta);
    }

    public void assignTexture(String texture) {
        NBT.modify(item, nbt -> {
            final ReadWriteNBT skullOwnerCompound = nbt.getOrCreateCompound("SkullOwner");

            skullOwnerCompound.setUUID("Id", UUID.randomUUID());
            skullOwnerCompound.getOrCreateCompound("Properties")
                    .getCompoundList("textures")
                    .addCompound()
                    .setString("Value", texture);
        });
    }
}
