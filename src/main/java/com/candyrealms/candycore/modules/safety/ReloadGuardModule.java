package com.candyrealms.candycore.modules.safety;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ReloadGuardModule implements Listener {

    private final AnubisCore plugin;

    private final Set<String> blockedExact = new HashSet<>(Arrays.asList(
            "/reload",
            "/rl",
            "/bukkit:reload"
    ));

    private final Set<String> blockedPrefixes = new HashSet<>(Arrays.asList(
            "/plugman reload",
            "/plugman rel",
            "/plm reload",
            "/tab reload"
    ));

    public ReloadGuardModule(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCmd(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase().trim();

        if (!shouldBlock(msg)) return;
        if (event.getPlayer().hasPermission("anubiscore.reload.bypass")) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&fLive plugin/server reloads are &cdisabled&f. Please &arestart&f the server."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsoleCmd(ServerCommandEvent event) {
        String msg = ("/" + event.getCommand()).toLowerCase().trim();
        if (!shouldBlock(msg)) return;

        event.setCancelled(true);
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("[AnubisCore] Live plugin/server reloads are disabled. Please restart the server."));
    }

    private boolean shouldBlock(String cmd) {
        if (blockedExact.contains(cmd)) return true;
        for (String prefix : blockedPrefixes) {
            if (cmd.startsWith(prefix)) return true;
        }
        return false;
    }
}

