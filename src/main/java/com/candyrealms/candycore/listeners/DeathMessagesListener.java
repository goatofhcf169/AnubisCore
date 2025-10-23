package com.candyrealms.candycore.listeners;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class DeathMessagesListener implements Listener {

    private final AnubisCore plugin;

    public DeathMessagesListener(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        FileConfiguration cfg = plugin.getDeathMessagesCFG().getConfig();
        if (!cfg.getBoolean("enabled", true)) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        String message;
        if (killer != null && cfg.getBoolean("pvp.enabled", true)) {
            String base = cfg.getString("pvp.message", "&c%victim% &fwas slain by &c%killer%&f%with%.");
            String withFmt = cfg.getString("pvp.with-weapon-format", " &7using &e%weapon%");

            String with = "";
            ItemStack hand = killer.getItemInHand();
            if (hand != null && hand.getType() != Material.AIR) {
                String weapon = (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName())
                        ? hand.getItemMeta().getDisplayName()
                        : prettify(hand.getType().name());
                with = withFmt.replace("%weapon%", weapon);
            }

            message = base
                    .replace("%victim%", victim.getName())
                    .replace("%killer%", killer.getName())
                    .replace("%with%", with)
                    .replace("%prefix%", plugin.getConfigManager().getPrefix());
        } else {
            String causeKey = "DEFAULT";
            if (victim.getLastDamageCause() != null && victim.getLastDamageCause().getCause() != null) {
                causeKey = victim.getLastDamageCause().getCause().name();
            }

            String path = "environment." + causeKey;
            String base = cfg.getString(path);
            if (base == null) base = cfg.getString("environment.DEFAULT", "&c%victim% &fdied.");

            message = base
                    .replace("%victim%", victim.getName())
                    .replace("%prefix%", plugin.getConfigManager().getPrefix());
        }

        // Visibility controls
        boolean opsOnly = cfg.getBoolean("visibility.ops-only", false);
        String viewPerm = cfg.getString("visibility.permission", "");

        if (opsOnly) {
            event.setDeathMessage(null);
            final String colored = ColorUtil.color(message);
            Bukkit.getOnlinePlayers().stream().filter(Player::isOp).forEach(p -> p.sendMessage(colored));
            return;
        }

        if (viewPerm != null && !viewPerm.isEmpty()) {
            event.setDeathMessage(null);
            final String colored = ColorUtil.color(message);
            Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission(viewPerm)).forEach(p -> p.sendMessage(colored));
            return;
        }

        event.setDeathMessage(ColorUtil.color(message));
    }

    private String prettify(String key) {
        String lower = key.toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
