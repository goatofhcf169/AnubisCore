package com.candyrealms.candycore.modules.pvptop;

import com.candyrealms.candycore.AnubisCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PvPTopListener implements Listener {

    private final AnubisCore plugin;

    public PvPTopListener(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (plugin.getModuleManager().getPvPTopModule() == null) return;
        plugin.getModuleManager().getPvPTopModule().addKillFor(killer);
    }
}

