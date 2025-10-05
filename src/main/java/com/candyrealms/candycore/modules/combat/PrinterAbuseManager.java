package com.candyrealms.candycore.modules.combat;

import com.candyrealms.candycore.CandyCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class PrinterAbuseManager {

    private final CandyCore plugin;

    private final Map<UUID, Instant> cooldownPlayers = new HashMap<>();

    private BukkitTask cooldownTask;

    public PrinterAbuseManager(CandyCore plugin) {
        this.plugin = plugin;
    }

    public void addCooldownPlayer(Player player) {
        cooldownPlayers.put(player.getUniqueId(), Instant.now().plusSeconds(15));

        startTask();
    }

    public boolean hasCooldown(Player player) {
        return cooldownPlayers.containsKey(player.getUniqueId());
    }

    private void startTask() {
        if(isTaskActive()) return;

        cooldownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if(cooldownPlayers.isEmpty()) {
                cooldownTask.cancel();
                System.out.println("[CandyCore] Cancelling Printer Cooldown!");
                return;
            }

            Iterator<UUID> playerIterator = cooldownPlayers.keySet().iterator();

            while(playerIterator.hasNext()) {
                UUID playerUUID = playerIterator.next();

                if(Instant.now().isBefore(cooldownPlayers.get(playerUUID))) continue;

                playerIterator.remove();
            }
        }, 20L, 20L);
    }

    private boolean isTaskActive() {
        return cooldownTask != null && (Bukkit.getScheduler().isCurrentlyRunning(cooldownTask.getTaskId())
                || Bukkit.getScheduler().isQueued(cooldownTask.getTaskId()));
    }
}
