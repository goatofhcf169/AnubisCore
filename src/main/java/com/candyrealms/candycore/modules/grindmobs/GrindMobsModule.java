package com.candyrealms.candycore.modules.grindmobs;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.configuration.GrindMobsCFG;
import com.candyrealms.candycore.utils.ColorUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class GrindMobsModule implements Listener {

    private final AnubisCore plugin;

    @Getter
    private final GrindMobsCFG cfg;

    private FileConfiguration config;

    // Settings
    private double chancePercent; // 0-100
    private String rewardCommand; // e.g. "voyage give %player% 1"
    private boolean broadcastStart;
    private boolean broadcastEnd;
    private List<String> startMessages;
    private List<String> endMessages;
    private String rewardMessage;
    private boolean rewardUsePrefix;

    // Runtime state
    private long eventEndMillis = 0L;
    private BukkitTask endTask;

    public GrindMobsModule(AnubisCore plugin) {
        this.plugin = plugin;
        this.cfg = new GrindMobsCFG(plugin);
        this.config = cfg.getConfig();
        cache();
    }

    public void reload() {
        cfg.reloadConfig();
        this.config = cfg.getConfig();
        cache();
    }

    private void cache() {
        this.chancePercent = Math.max(0.0, Math.min(100.0, config.getDouble("chance-percent", 5.0)));
        String cmd = config.getString("reward-command", "voyage give %player% 1");
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        this.rewardCommand = cmd;
        this.broadcastStart = config.getBoolean("messages.broadcast-start", true);
        this.broadcastEnd = config.getBoolean("messages.broadcast-end", true);
        this.startMessages = config.getStringList("messages.start");
        this.endMessages = config.getStringList("messages.end");
        this.rewardMessage = config.getString("messages.reward", "&aYou found a &dVoyage Portal&a!");
        this.rewardUsePrefix = config.getBoolean("messages.use-prefix", true);
    }

    public boolean isActive() {
        return System.currentTimeMillis() < eventEndMillis;
    }

    public long getRemainingMillis() {
        long rem = eventEndMillis - System.currentTimeMillis();
        return Math.max(0L, rem);
    }

    public void startForMillis(long durationMillis) {
        if (durationMillis <= 0) durationMillis = 1000L;
        // Replace existing
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }
        eventEndMillis = System.currentTimeMillis() + durationMillis;

        if (broadcastStart && startMessages != null && !startMessages.isEmpty()) {
            String nice = formatDuration(durationMillis);
            for (String line : startMessages) {
                Bukkit.broadcastMessage(ColorUtil.color(line.replace("%time%", nice)));
            }
        }

        final long endAt = eventEndMillis;
        endTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() >= endAt) {
                    stop();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stop() {
        eventEndMillis = 0L;
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }
        if (broadcastEnd && endMessages != null && !endMessages.isEmpty()) {
            for (String line : endMessages) {
                Bukkit.broadcastMessage(ColorUtil.color(line));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isActive()) return;
        LivingEntity entity = event.getEntity();
        if (entity == null) return;
        if (entity instanceof Player) return; // exclude player deaths
        Player killer = entity.getKiller();
        if (killer == null) return;

        double roll = ThreadLocalRandom.current().nextDouble(0.0, 100.0);
        if (roll < chancePercent) {
            // Reward
            String cmd = rewardCommand.replace("%player%", killer.getName());
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } catch (Throwable ignored) {}
            if (rewardMessage != null && !rewardMessage.isEmpty()) {
                String base = rewardMessage.replace("%player%", killer.getName());
                String msg = (rewardUsePrefix ? plugin.getConfigManager().getPrefix() + " " : "") + base;
                killer.sendMessage(ColorUtil.color(msg));
            }
        }
    }

    public static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long days = totalSeconds / 86400L; totalSeconds %= 86400L;
        long hours = totalSeconds / 3600L; totalSeconds %= 3600L;
        long minutes = totalSeconds / 60L; long seconds = totalSeconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    public static long parseDurationToMillis(String input) {
        if (input == null || input.trim().isEmpty()) return -1L;
        String s = input.trim().toLowerCase(Locale.ROOT);
        try {
            // Accept plain number as seconds
            if (s.matches("^\\d+$")) {
                long val = Long.parseLong(s);
                return Math.max(0L, val) * 1000L;
            }
            long mul = 1000L; // seconds default
            if (s.endsWith("s") || s.endsWith("sec") || s.endsWith("secs") || s.endsWith("second") || s.endsWith("seconds")) {
                mul = 1000L;
                s = s.replaceAll("(seconds|second|secs|sec|s)$", "");
            } else if (s.endsWith("m") || s.endsWith("min") || s.endsWith("mins") || s.endsWith("minute") || s.endsWith("minutes")) {
                mul = 60_000L;
                s = s.replaceAll("(minutes|minute|mins|min|m)$", "");
            } else if (s.endsWith("h") || s.endsWith("hr") || s.endsWith("hour") || s.endsWith("hours")) {
                mul = 3_600_000L;
                s = s.replaceAll("(hours|hour|hr|h)$", "");
            } else if (s.endsWith("d") || s.endsWith("day") || s.endsWith("days")) {
                mul = 86_400_000L;
                s = s.replaceAll("(days|day|d)$", "");
            }
            long val = Long.parseLong(s.trim());
            return Math.max(0L, val) * mul;
        } catch (Exception ex) {
            return -1L;
        }
    }
}
