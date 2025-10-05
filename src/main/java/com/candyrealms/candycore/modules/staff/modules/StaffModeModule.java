package com.candyrealms.candycore.modules.staff.modules;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class StaffModeModule {

    private final CandyCore plugin;

    private final StaffItemsModule module;

    private final Map<UUID, ItemStack[]> staffContents = new HashMap<>();

    private final Set<UUID> staffUUIDS = new HashSet<>();

    public StaffModeModule(CandyCore plugin) {
        this.plugin = plugin;
        module = new StaffItemsModule(plugin);
    }

    public void toggleStaffMode(Player player) {
        FileConfiguration config = plugin.getStaffCFG().getConfig();

        if(inStaffMode(player)) {
            leaveStaffMode(player);

            List<String> staffDisabledMsg = config.getStringList("messages.staff-disabled").stream()
                    .map(ColorUtil::color)
                    .collect(Collectors.toList());

            staffDisabledMsg.forEach(player::sendMessage);
            player.playSound(player.getLocation(), Sound.ANVIL_USE, 7, 5);
            return;
        }

        List<String> staffEnabledMsg = config.getStringList("messages.staff-enabled").stream()
                .map(ColorUtil::color)
                .collect(Collectors.toList());

        addStaffMode(player);

        player.getInventory().clear();

        addStaffItems(player);

        player.updateInventory();

        staffEnabledMsg.forEach(player::sendMessage);

        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 7, 6);

        player.setAllowFlight(true);
        player.setFlying(true);
    }

    public void leaveStaffMode(Player player) {
        if(!inStaffMode(player)) return;

        player.getInventory().clear();
        player.getInventory().setContents(staffContents.get(player.getUniqueId()));
        player.updateInventory();
        removeStaffMode(player);
    }

    public void onRestart() {
        Iterator<Map.Entry<UUID, ItemStack[]>> entryIterator = staffContents.entrySet().iterator();

        while(entryIterator.hasNext()) {
            Map.Entry<UUID, ItemStack[]> entry = entryIterator.next();

            if(Bukkit.getPlayer(entry.getKey()) == null || !Bukkit.getPlayer(entry.getKey()).isOnline()) {
                entryIterator.remove();
                return;
            }

            Player player = Bukkit.getPlayer(entry.getKey());

            player.getInventory().clear();

            player.getInventory().setContents(entry.getValue());
            player.updateInventory();

            entryIterator.remove();
        }
    }

    public boolean inStaffMode(Player player) {
        return staffUUIDS.contains(player.getUniqueId());
    }

    private void removeStaffMode(Player player) {
        staffUUIDS.remove(player.getUniqueId());
        staffContents.remove(player.getUniqueId());
    }

    private void addStaffMode(Player player) {
        staffUUIDS.add(player.getUniqueId());

        staffContents.put(player.getUniqueId(), player.getInventory().getContents());
    }

    private void addStaffItems(Player player) {
        player.getInventory().setItem(1, module.getFreezeItem());
        player.getInventory().setItem(3, module.getInvseeItem());
        player.getInventory().setItem(5, module.getVanishItem());
        player.getInventory().setItem(7, module.getRTPItem());
    }

}
