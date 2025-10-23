package com.candyrealms.candycore.modules.pvptop;

import com.candyrealms.candycore.AnubisCore;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.golfing8.kore.event.KothCaptureEvent;

public class PvPTopKothListener implements Listener {

    private final AnubisCore plugin;

    public PvPTopKothListener(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKothCapture(KothCaptureEvent event) {
        if (plugin.getModuleManager().getPvPTopModule() == null) return;

        // Prefer resolving via the player capper's faction for reliability
        String playerName = null;
        try { playerName = (event.getPlayerCapper() != null) ? event.getPlayerCapper().getName() : null; } catch (Throwable ignored) {}

        String factionId = null;
        if (event.getPlayerCapper() != null) {
            try {
                // Use module helper to respect Wilderness exclusion
                factionId = plugin.getModuleManager().getPvPTopModule().getFactionId(event.getPlayerCapper());
            } catch (Throwable ignored) {}
        }

        // Fallback to using the capper string (tag or id) from the event
        if ((factionId == null || factionId.isEmpty())) {
            String capperStr = null;
            try { capperStr = event.getFactionCapper(); } catch (Throwable ignored) {}
            if (capperStr != null && !capperStr.trim().isEmpty()) {
                try {
                    Faction f = Factions.getInstance().getByTag(capperStr);
                    if (f == null) f = Factions.getInstance().getFactionById(capperStr);
                    if (f != null) {
                        boolean excluded = false;
                        // Try explicit API if available
                        try {
                            java.lang.reflect.Method m = f.getClass().getMethod("isWilderness");
                            Object v = m.invoke(f);
                            excluded = (v instanceof Boolean) && ((Boolean) v);
                        } catch (NoSuchMethodException ignored) {
                            // Fallback to tag check
                            try {
                                String tag = f.getTag();
                                if (tag != null) {
                                    if (tag.equalsIgnoreCase("wilderness") || tag.equalsIgnoreCase("RaidOutpost")) excluded = true;
                                }
                            } catch (Throwable ignored2) {}
                        } catch (Throwable ignored) {}
                        if (!excluded) {
                            factionId = f.getId();
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        if (factionId == null || factionId.isEmpty()) return;
        plugin.getModuleManager().getPvPTopModule().addKoTHCapture(factionId, 1, playerName == null ? "" : playerName);
    }
}
