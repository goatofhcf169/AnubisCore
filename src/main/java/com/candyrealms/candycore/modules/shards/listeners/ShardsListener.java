package com.candyrealms.candycore.modules.shards.listeners;

import com.candyrealms.candycore.AnubisCore;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

import static com.candyrealms.candycore.utils.ColorUtil.color;


public class ShardsListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if(event.getItemInHand() == null || event.getItemInHand().getType() == Material.AIR) return;

        NBTItem nbtItem = new NBTItem(event.getItemInHand());

        if(nbtItem.getCompound("CandyShards") == null) return;

        event.setCancelled(true);
    }
    @EventHandler
    public void onApply(InventoryClickEvent event) {
        if(!(event.getWhoClicked() instanceof Player)) return;

        if(event.getClick() == ClickType.CREATIVE) return;

        if(event.getCursor() == null || event.getCursor().getType() == Material.AIR) return;
        if(event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();

        ItemStack heldItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();

        if(heldItem.equals(clickedItem)) return;

        NBTItem nbtItem = new NBTItem(heldItem);

        if(nbtItem.getCompound("CandyShards") == null) return;

        NBTCompound nbtCompound = nbtItem.getCompound("CandyShards");

        String[] splitEnchantString = Objects.requireNonNull(nbtCompound.getString("Enchantment").split(":"));

        clickedItem.addUnsafeEnchantment(Enchantment.getByName(splitEnchantString[0]), Integer.valueOf(splitEnchantString[1]));

        event.setCurrentItem(clickedItem);

        if (splitEnchantString.length < 2) return;
        String enchName = splitEnchantString[0];
        int enchLevel = Integer.parseInt(splitEnchantString[1]);

        Material type = clickedItem.getType();
        if (enchName.equalsIgnoreCase("PROTECTION_ENVIRONMENTAL") && enchLevel == 5 && isPatchArmor(clickedItem)) {
            String message;

            if (type.name().endsWith("_SWORD")) {
                message = color("&5&lSHARDS &8» &cYou cannot apply a Protection V Shard to a Tank Sword!");
            } else if (type.name().endsWith("_PICKAXE")) {
                message = color("&5&lSHARDS &8» &cYou cannot apply a Protection V Shard to a Tank Pickaxe!");
            } else if (type.name().endsWith("_HELMET") || type.name().endsWith("_CHESTPLATE") ||
                    type.name().endsWith("_LEGGINGS") || type.name().endsWith("_BOOTS")) {
                message = color("&5&lSHARDS &8» &cYou cannot apply a Protection V Shard to a Tank Armor!");
            } else {
                message = color("&5&lSHARDS &8» &cYou cannot apply a Protection V Shard to a Tank Armor!");
            }
            player.sendMessage(message);
            event.setCancelled(true);
            return;
        }

        if (enchName.equalsIgnoreCase("DAMAGE_ALL") && enchLevel == 6) {
            if(!type.name().endsWith("_SWORD")) {
                player.sendMessage(color("&5&lSHARDS &8» &cSharpness VI Shard can only be applied to Swords!"));
                event.setCancelled(true);
                return;
            }
        }

        if (enchName.equalsIgnoreCase("DEPTH_STRIDER") && enchLevel == 3) {
            if(!type.name().endsWith("_BOOTS")) {
                player.sendMessage(color("&5&lSHARDS &8» &cDepth Strider III Shard can only be applied to Boots!"));
                event.setCancelled(true);
                return;
            }
        }

        clickedItem.addUnsafeEnchantment(Enchantment.getByName(enchName), enchLevel);
        event.setCurrentItem(clickedItem);

        if(heldItem.getAmount() > 1) {
            heldItem.setAmount(heldItem.getAmount() - 1);
            player.setItemOnCursor(heldItem);
        } else {
            player.setItemOnCursor(new ItemStack(Material.AIR));
        }

        event.setCancelled(true);
    }

    private boolean isPatchArmor(ItemStack item) {
        if (item == null) return false;

        NBTItem nbtItem = new NBTItem(item);
        String setName = nbtItem.getString("SetName");

        if ("Patch".equalsIgnoreCase(setName)) return true;
        NBTCompound vulcan = nbtItem.getCompound("VulcanEnchants");
        if (vulcan != null) {
            String armorSet = vulcan.getString("ArmorSet");
            if ("Patch".equalsIgnoreCase(armorSet)) return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                String name = ChatColor.stripColor(meta.getDisplayName());
                if (name != null && name.toLowerCase().contains("tank")) return true;
            }
            if (meta.hasLore()) {
                for (String line : meta.getLore()) {
                    String clean = ChatColor.stripColor(line);
                    if (clean != null && clean.toLowerCase().contains("Take 20% less damage from all enemies"))
                        return true;
                }
            }
        }
        return false;
    }
}
