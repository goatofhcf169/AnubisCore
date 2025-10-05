package com.candyrealms.candycore.listeners;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import com.candyrealms.candycore.utils.ColorUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.stream.Collectors;

public class BalanceListeners implements Listener {

    private final ConfigManager configManager;

    public BalanceListeners(AnubisCore plugin) {
        configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();

        if(!command.equals("/balance") && !command.equals("/bal")) return;

        Player player = event.getPlayer();

        getBalanceMessage(player).forEach(player::sendMessage);

        player.playSound(player.getLocation(), Sound.LEVEL_UP, 6, 6);

        event.setCancelled(true);
    }

    private List<String> getBalanceMessage(Player player) {
        return configManager.getBalanceMsg().stream()
                .map(s -> PlaceholderAPI.setPlaceholders(player, s))
                .map(ColorUtil::color)
                .collect(Collectors.toList());
    }
}
