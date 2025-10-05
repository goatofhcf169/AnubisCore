package com.candyrealms.candycore.modules.shards.listeners;

import com.candyrealms.candycore.CandyCore;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;


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

        if(heldItem.getAmount() > 1) {
            heldItem.setAmount(heldItem.getAmount() - 1);
            player.setItemOnCursor(heldItem);
        } else {
            player.setItemOnCursor(new ItemStack(Material.AIR));
        }

        event.setCancelled(true);
    }
}
