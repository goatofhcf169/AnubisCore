package com.candyrealms.candycore.modules.donator;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.utils.ColorUtil;
import com.candyrealms.candycore.utils.CompatUtil;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DonatorModule {

    private final AnubisCore plugin;

    private FileConfiguration config;

    private BukkitTask rankAnnTask;

    private BukkitTask donatorPerkTask;

    private String rankPermission;
    private String donatorReward;
    private String globalDonoReward;

    private int rankAnnInterval;
    private int perkInterval;

    private List<String> rankAnnMessages;
    private List<String> donationMessages;
    private List<String> donatorPerkMessages;
    private List<String> potionsList;

    public DonatorModule(AnubisCore plugin) {
        this.plugin = plugin;

        config = plugin.getDonatorCFG().getConfig();

        cacheValues();
        startTasks();
    }

    public void startTasks() {
        startRankTask();
        startPerkTask();
    }

    public void donate(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        Bukkit.getWorld(player.getWorld().getName()).spawnEntity(player.getLocation(), EntityType.FIREWORK);

        // Do not announce donations for OPs
        if (!player.isOp()) {
            donationMessages.forEach(string -> Bukkit.broadcastMessage(ColorUtil.color(string
                    .replace("%player%", player.getName()))));
        }

        List<UUID> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(rankPermission))
                .map(Player::getUniqueId)
                .collect(Collectors.toList());

        for(UUID playerUUID : onlinePlayers) {
            Player onlinePlayer = Bukkit.getPlayer(playerUUID);

            CompatUtil.play(onlinePlayer, 7f, 7f, "LEVEL_UP", "ENTITY_PLAYER_LEVELUP");

            givePotions(onlinePlayer, potionsList);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), globalDonoReward
                    .replace("%player%", onlinePlayer.getName()));
        }
    }

    private void givePotions(Player player, List<String> potionList) {
        for(String string : potionList) {
            String[] splitPotString = string.split(":");

            String potionName = splitPotString[0];
            int amplifier = (Integer.parseInt(splitPotString[1]) - 1);
            int length = Integer.parseInt(splitPotString[2]);

            player.addPotionEffect(new PotionEffect(PotionEffectType.getByName(potionName), 20 * length, amplifier));
        }
    }

    private void startPerkTask() {
        if(isRunning(donatorPerkTask) || !config.getBoolean("donator-perks.enabled")) return;

        donatorPerkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<UUID> rankedPlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission(rankPermission))
                    .map(Player::getUniqueId)
                    .collect(Collectors.toList());

            rankedPlayers.forEach(uuid -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), donatorReward
                    .replace("%player%", Bukkit.getPlayer(uuid).getName())));

            donatorPerkMessages.forEach(string -> Bukkit.broadcastMessage(ColorUtil.color(string)));

        }, 20L * 5, 20L * perkInterval);
    }

    private void startRankTask() {
        if(isRunning(rankAnnTask) || !config.getBoolean("announce-rank.enabled")) return;

        rankAnnTask = Bukkit.getScheduler().runTaskTimer(plugin, ()-> {
            List<String> rankedPlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission(rankPermission))
                    .filter(p -> !p.isOp())
                    .map(Player::getName)
                    .map(s -> ColorUtil.color("&6" + s))
                    .collect(Collectors.toList());

            rankAnnMessages.forEach(string -> {
                Bukkit.broadcastMessage(ColorUtil.color(string
                        .replace("%players%", StringUtils.join(rankedPlayers, ", "))
                ));
            });

        }, 20L * 5, 20L * rankAnnInterval);
    }

    private void cacheValues() {
        rankPermission = config.getString("announce-rank.permission");
        rankAnnInterval = config.getInt("announce-rank.interval");
        rankAnnMessages = config.getStringList("announce-rank.message");
        donatorPerkMessages = config.getStringList("donator-perks.message");
        perkInterval = config.getInt("donator-perks.interval");
        donatorReward = config.getString("donator-perks.command");
        donationMessages = config.getStringList("announce-donation.message");
        potionsList = config.getStringList("announce-donation.potion-perks");
        globalDonoReward = config.getString("announce-donation.command");
    }

    private void cancelAllTasks() {
        if(rankAnnTask != null) {
            rankAnnTask.cancel();
        }

        if(donatorPerkTask == null) return;
        donatorPerkTask.cancel();
    }

    public void reload() {
        cancelAllTasks();
        config = plugin.getDonatorCFG().getConfig();

        cacheValues();
        startTasks();
    }

    private boolean isRunning(BukkitTask task) {
        if(task == null) return false;

        return (Bukkit.getScheduler().isCurrentlyRunning(task.getTaskId()) || Bukkit.getScheduler().isQueued(task.getTaskId()));
    }
}
