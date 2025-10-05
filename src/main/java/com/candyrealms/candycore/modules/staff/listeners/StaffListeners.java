package com.candyrealms.candycore.modules.staff.listeners;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.modules.staff.modules.FreezeModule;
import com.candyrealms.candycore.modules.staff.modules.StaffModeModule;
import com.candyrealms.candycore.utils.ColorUtil;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;


public class StaffListeners implements Listener {

    private final CandyCore plugin;

    public StaffListeners(CandyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player clicker = event.getPlayer();

        if(clicker.getItemInHand() == null || clicker.getItemInHand().getType() == Material.AIR) return;

        NBTItem nbtItem = new NBTItem(clicker.getItemInHand());

        if(nbtItem.getCompound("CandyStaff") == null) return;

        event.setCancelled(true);

        NBTCompound compound = nbtItem.getCompound("CandyStaff");

        String type = compound.getString("type").toLowerCase();

        switch (type) {
            case "vanish":
                clicker.performCommand("vanish");
                return;
            case "rtp":
                clicker.performCommand("rtp");
        }
    }

    @EventHandler
    public void onFreezeMove(PlayerMoveEvent event) {
        Block initialBlock = event.getFrom().clone().subtract(0, 1, 0).getBlock();
        Block toBlock = event.getTo().clone().subtract(0, 1, 0).getBlock();

        if(initialBlock.getX() == toBlock.getX() && initialBlock.getZ() == toBlock.getZ()) return;

        Player player = event.getPlayer();

        FreezeModule freezeModule = plugin.getModuleManager().getStaffModule().getFreezeModule();

        if(!freezeModule.isUUIDFrozen(player.getUniqueId())) return;

        FileConfiguration config = plugin.getStaffCFG().getConfig();

        List<String> messages = config.getStringList("messages.frozen-msg")
                .stream()
                .map(ColorUtil::color)
                .collect(Collectors.toList());

        messages.forEach(player::sendMessage);

        event.setCancelled(true);
    }

    @EventHandler
    public void onStaffLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        StaffModeModule module = plugin.getModuleManager().getStaffModule().getStaffModeModule();

        module.leaveStaffMode(player);
    }

    @EventHandler
    public void onStaffKick(PlayerKickEvent event) {
        Player player = event.getPlayer();

        StaffModeModule module = plugin.getModuleManager().getStaffModule().getStaffModeModule();

        module.leaveStaffMode(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        StaffModeModule module = plugin.getModuleManager().getStaffModule().getStaffModeModule();

        module.leaveStaffMode(player);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if(!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        StaffModeModule module = plugin.getModuleManager().getStaffModule().getStaffModeModule();

        if(!module.inStaffMode(player)) return;

        event.setCancelled(true);
    }


    @EventHandler
    public void onFreezeLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        FreezeModule freezeModule = plugin.getModuleManager().getStaffModule().getFreezeModule();

        if(!freezeModule.isUUIDFrozen(player.getUniqueId())) return;

        FileConfiguration config = plugin.getStaffCFG().getConfig();

        List<String> messages = config.getStringList("messages.leave-frozen-msg")
                .stream()
                .map(s -> s.replace("%player%", player.getName()))
                .map(ColorUtil::color)
                .collect(Collectors.toList());

        Bukkit.getOnlinePlayers().forEach(p -> {
            p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 6, 6);

            messages.forEach(s -> p.sendMessage(s));
        });
    }

    @EventHandler
    public void onCommandWhenFrozen(PlayerCommandPreprocessEvent event) {
        if(event.getPlayer().hasPermission("candycore.staffmode")) return;

        Player player = event.getPlayer();

        FreezeModule freezeModule = plugin.getModuleManager().getStaffModule().getFreezeModule();

        if(!freezeModule.isUUIDFrozen(player.getUniqueId())) return;

        String command = event.getMessage().toLowerCase();

        if(command.startsWith("/msg") || command.startsWith("/r")) return;

        player.sendMessage(ColorUtil.color("&4&lFREEZE &cYou cannot do commands when frozen!"));

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player clicker = event.getPlayer();

        if(clicker.getItemInHand() == null || clicker.getItemInHand().getType() == Material.AIR) return;

        NBTItem nbtItem = new NBTItem(clicker.getItemInHand());

        if(nbtItem.getCompound("CandyStaff") == null) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if(!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        StaffModeModule module = plugin.getModuleManager().getStaffModule().getStaffModeModule();

        if(!module.inStaffMode(player)) return;

        if(event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        ItemStack itemStack = event.getCurrentItem();

        NBTItem nbtItem = new NBTItem(itemStack);

        if(nbtItem.getCompound("CandyStaff") == null) return;

        event.setCancelled(true);

        if(player.getGameMode() != GameMode.CREATIVE) return;

        player.closeInventory();
    }

    @EventHandler
    public void dropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        StaffModeModule module = plugin.getModuleManager().getStaffModule().getStaffModeModule();

        if(!module.inStaffMode(player)) return;

        ItemStack itemStack = event.getItemDrop().getItemStack();

        if(itemStack == null || itemStack.getType() == Material.AIR) return;

        NBTItem nbtItem = new NBTItem(itemStack);

        if(nbtItem.getCompound("CandyStaff") == null) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if(!(event.getRightClicked() instanceof Player)) return;

        Player clicker = event.getPlayer();
        Player clicked = (Player) event.getRightClicked();

        if(clicker.getItemInHand() == null || clicker.getItemInHand().getType() == Material.AIR) return;

        NBTItem nbtItem = new NBTItem(clicker.getItemInHand());

        if(nbtItem.getCompound("CandyStaff") == null) return;

        NBTCompound compound = nbtItem.getCompound("CandyStaff");

        String type = compound.getString("type").toLowerCase();

        switch (type) {
            case "invsee":
                clicker.performCommand("invsee " + clicked.getName());
                event.setCancelled(true);
                return;
            case "freeze":
                clicker.performCommand("freeze " + clicked.getName());
                event.setCancelled(true);
        }
    }
}
