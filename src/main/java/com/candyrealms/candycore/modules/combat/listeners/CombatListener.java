package com.candyrealms.candycore.modules.combat.listeners;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.combat.CombatModule;
import com.candyrealms.candycore.modules.combat.PrinterAbuseManager;
import com.candyrealms.candycore.modules.combat.events.CombatTagExpireEvent;
import com.candyrealms.candycore.utils.ActionBarUtil;
import com.candyrealms.candycore.utils.ColorUtil;
import com.golfing8.kore.event.PlayerPrinterEnterEvent;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import net.minelink.ctplus.event.PlayerCombatTagEvent;
import net.minelink.ctplus.TagManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

public class CombatListener implements Listener {

    private final PrinterAbuseManager printerAbuseManager;

    private final CombatModule combatModule;
    private final AnubisCore plugin;
    private final TagManager tagManager;

    private final java.util.Map<java.util.UUID, BukkitTask> actionBarTasks = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Long> tagExpireAt = new java.util.HashMap<>();
    private final int defaultTagSeconds;

    public CombatListener(AnubisCore plugin) {
        this.plugin = plugin;
        combatModule = plugin.getModuleManager().getCombatModule();
        printerAbuseManager = combatModule.getPrinterAbuseManager();
        tagManager = plugin.getCombatTagPlus().getTagManager();
        this.defaultTagSeconds = resolveTagDuration();
    }

    @EventHandler
    public void onCombat(PlayerCombatTagEvent event) {
        combatModule.addTaggedPlayers(event.getVictim(), event.getAttacker());

        if (!plugin.getConfigManager().isCombatBarEnabled()) return;
        int baseDur = resolveTagDuration();
        long until = System.currentTimeMillis() + (baseDur * 1000L);
        if (event.getVictim() != null && plugin.getConfigManager().isCombatBarSendVictim()) {
            tagExpireAt.put(event.getVictim().getUniqueId(), until);
            startActionBar(event.getVictim());
        }
        if (event.getAttacker() != null && plugin.getConfigManager().isCombatBarSendAttacker()) {
            tagExpireAt.put(event.getAttacker().getUniqueId(), until);
            startActionBar(event.getAttacker());
        }
    }

    @EventHandler
    public void onExpire(CombatTagExpireEvent event) {
        printerAbuseManager.addCooldownPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPrinter(PlayerPrinterEnterEvent event) {
        if(event.getPlayer().hasPermission("anubiscore.admin")) return;

        if(!printerAbuseManager.hasCooldown(event.getPlayer())) return;

        boolean nearbyEnemy = false;

        for(Entity entity : event.getPlayer().getNearbyEntities(20, 5, 20)) {
            if(!(entity instanceof Player)) continue;
            Player nearbyPlayer = (Player) entity;

            FPlayer nearbyFPlayer = FPlayers.getInstance().getByPlayer(nearbyPlayer);

            if(!nearbyFPlayer.hasFaction()) continue;
            if(nearbyPlayer.hasPermission("anubiscore.admin")) continue;

            nearbyEnemy = true;
        }

        if(!nearbyEnemy) return;

        event.getPlayer().sendMessage(ColorUtil.color("&d&lAnubis&5&lCore &fYou &c&nCANNOT&f enter printer quickly after leaving combat!"));
        event.setCancelled(true);
    }

    private void startActionBar(Player player) {
        if (player == null) return;
        java.util.UUID id = player.getUniqueId();
        if (actionBarTasks.containsKey(id)) return;

        final int segments = Math.max(1, plugin.getConfigManager().getCombatBarSegments());
        final int updateTicks = Math.max(1, plugin.getConfigManager().getCombatBarUpdateTicks());
        BukkitTask task = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelTask(id);
                return;
            }

            // Visibility gating (ops-only or permission-based)
            if (plugin.getConfigManager().isCombatBarOpsOnly() && !player.isOp()) {
                return; // do not send to non-ops
            }
            String viewPerm = plugin.getConfigManager().getCombatBarViewPermission();
            if (viewPerm != null && !viewPerm.isEmpty() && !player.hasPermission(viewPerm)) {
                return; // requires permission
            }

            if (!tagManager.isTagged(id)) {
                // allow countdown to continue based on our expire time if present
                Long exp = tagExpireAt.get(id);
                if (exp == null || System.currentTimeMillis() >= exp) {
                    cancelTask(id);
                    return;
                }
            }

            int total = segments; // configurable bar segments
            long now = System.currentTimeMillis();
            long expAt = tagExpireAt.getOrDefault(id, now);
            long remainMs = Math.max(0L, expAt - now);
            int remainSec = (int) Math.ceil(remainMs / 1000.0);
            int tagSeconds = Math.max(1, resolveTagDuration());
            int fill = (int) Math.ceil((remainMs / (tagSeconds * 1000.0)) * total);
            if (fill < 0) fill = 0; if (fill > total) fill = total;

            String activeColor = plugin.getConfigManager().getCombatBarActiveColor();
            String inactiveColor = plugin.getConfigManager().getCombatBarInactiveColor();
            String activeChar = plugin.getConfigManager().getCombatBarActiveChar();
            String inactiveChar = plugin.getConfigManager().getCombatBarInactiveChar();

            StringBuilder bar = new StringBuilder();
            bar.append(activeColor);
            for (int i = 0; i < fill; i++) bar.append(activeChar);
            bar.append(inactiveColor);
            for (int i = fill; i < total; i++) bar.append(inactiveChar);

            String fmt = plugin.getConfigManager().getCombatBarFormat();
            String msg = fmt.replace("%bar%", bar.toString()).replace("%seconds%", String.valueOf(remainSec));
            ActionBarUtil.send(player, msg);
        }, 0L, updateTicks);

        actionBarTasks.put(id, task);
    }

    private void cancelTask(java.util.UUID id) {
        BukkitTask t = actionBarTasks.remove(id);
        tagExpireAt.remove(id);
        if (t != null) t.cancel();
    }

    private int resolveTagDuration() {
        try {
            if (plugin.getConfigManager().isCombatBarUseCTPDuration()) {
                int v = plugin.getCombatTagPlus().getConfig().getInt("tag-duration");
                if (v > 0) return v;
            }
        } catch (Throwable ignored) {}
        return Math.max(1, plugin.getConfigManager().getCombatBarDefaultDuration());
    }
}
