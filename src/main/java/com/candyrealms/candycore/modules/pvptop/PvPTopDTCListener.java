package com.candyrealms.candycore.modules.pvptop;

import com.candyrealms.candycore.AnubisCore;
import me.dreewww.destroythecore.DestroyTheCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class PvPTopDTCListener implements Listener {

    private final AnubisCore plugin;

    // Simple de-dupe so we don't award multiple times for the same core break
    private String lastAwardKey = ""; // world:x:y:z
    private long lastAwardAtMs = 0L;

    public PvPTopDTCListener(AnubisCore plugin) {
        this.plugin = plugin;
    }

    // Do not ignore cancelled: many DTC plugins cancel the actual break
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCoreBreak(BlockBreakEvent event) {
        if (plugin.getModuleManager().getPvPTopModule() == null) return;
        if (Bukkit.getPluginManager().getPlugin("DestroyTheCore") == null) return;

        DestroyTheCore dtc = (DestroyTheCore) Bukkit.getPluginManager().getPlugin("DestroyTheCore");
        if (dtc == null) return;

        String coords = dtc.getCoreUtils().getRunningCoreCoords();
        if (coords == null || coords.trim().isEmpty()) return;

        int[] xyz = parseCoords(coords);
        if (xyz == null) return;

        Location b = event.getBlock().getLocation();
        if (b.getBlockX() == xyz[0] && b.getBlockY() == xyz[1] && b.getBlockZ() == xyz[2]) {
            final String key = b.getWorld().getName() + ":" + xyz[0] + ":" + xyz[1] + ":" + xyz[2];
            final Player breaker = event.getPlayer();
            final java.util.UUID breakerUuid = (breaker != null) ? breaker.getUniqueId() : null;
            final String breakerName = (breaker != null) ? breaker.getName() : null;
            final String runningCoreName = safeRunningCoreName(dtc);

            // Delay a bit and only award if the core actually ended after this break
            new BukkitRunnable() {
                @Override public void run() {
                    String afterName = safeRunningCoreName(dtc);
                    // If the running core name changed or is now null, we consider the core ended
                    boolean ended = (runningCoreName != null && (afterName == null || !runningCoreName.equalsIgnoreCase(afterName)));
                    if (!ended) return;

                    // De-dupe within a short window for same location/core
                    long now = System.currentTimeMillis();
                    if (key.equals(lastAwardKey) && (now - lastAwardAtMs) < 5000L) return;
                    lastAwardKey = key;
                    lastAwardAtMs = now;
                    // Award strictly to the player who broke the core
                    String factionId = null;
                    String playerName = breakerName;
                    if (breakerUuid != null) {
                        factionId = plugin.getModuleManager().getPvPTopModule().getFactionId(breakerUuid);
                        if (playerName == null) {
                            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(breakerUuid);
                            playerName = (op != null) ? op.getName() : null;
                        }
                    }
                    if (factionId == null && breaker != null) {
                        factionId = plugin.getModuleManager().getPvPTopModule().getFactionId(breaker);
                        if (playerName == null) playerName = breaker.getName();
                    }
                    if (factionId != null) {
                        plugin.getModuleManager().getPvPTopModule().addDTCWin(factionId, 1, playerName == null ? "" : playerName);
                    }
                }
            }.runTaskLater(plugin, 20L); // ~1s delay to let DTC stop
        }
    }

    // Attempts to extract x,y,z integers from the provided coordinates string
    private int[] parseCoords(String s) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\d+").matcher(s);
            int[] vals = new int[3];
            int i = 0;
            while (m.find() && i < 3) {
                vals[i++] = Integer.parseInt(m.group());
            }
            return (i == 3) ? vals : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private String safeRunningCoreName(DestroyTheCore dtc) {
        try {
            String name = dtc.getCoreUtils().getRunningCoreName();
            if (name == null || name.trim().isEmpty()) return null;
            return name;
        } catch (Throwable t) {
            return null;
        }
    }

    // No longer used: awarding goes to the breaker, not top contributor
}
