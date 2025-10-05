package com.candyrealms.candycore.modules.combat;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.combat.events.CombatTagExpireEvent;
import lombok.Getter;
import net.minelink.ctplus.TagManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class CombatModule {

    private final AnubisCore plugin;

    @Getter
    private final PrinterAbuseManager printerAbuseManager;

    private final TagManager tagManager;

    private final Set<UUID> taggedPlayers = new HashSet<>();

    private BukkitTask combatTask;

    public CombatModule(AnubisCore plugin) {
        this.plugin = plugin;

        tagManager = plugin.getCombatTagPlus().getTagManager();

        printerAbuseManager = new PrinterAbuseManager(plugin);
    }

    public void addTaggedPlayers(Player player1, Player player2) {
        if(player1 != null) {
            taggedPlayers.add(player1.getUniqueId());
        }

        if(player2 != null) {
            taggedPlayers.add(player2.getUniqueId());
        }

        startTask();
    }

    private boolean isTaskActive() {
        return combatTask != null && (Bukkit.getScheduler().isQueued(combatTask.getTaskId()) || Bukkit.getScheduler().isCurrentlyRunning(combatTask.getTaskId()));
    }

    private void startTask() {
        if(isTaskActive()) return;

        // Do the task to check if they are still tagged
        combatTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if(taggedPlayers.isEmpty()) {
                Bukkit.getScheduler().cancelTask(combatTask.getTaskId());
                combatTask.cancel();
                System.out.println("[AnubisCore] Stopping Combat Task!");
                return;
            }

            Iterator<UUID> playerIterator = taggedPlayers.iterator();

            while(playerIterator.hasNext()) {
                UUID taggedUUID = playerIterator.next();

                if(Bukkit.getPlayer(taggedUUID) == null) {
                    playerIterator.remove();
                    continue;
                }

                if(tagManager.isTagged(taggedUUID)) continue;

                Player player = Bukkit.getPlayer(taggedUUID);

                CombatTagExpireEvent event = new CombatTagExpireEvent(player);

                Bukkit.getPluginManager().callEvent(event);

                playerIterator.remove();
            }
        }, 20L, 1L);
    }
}
