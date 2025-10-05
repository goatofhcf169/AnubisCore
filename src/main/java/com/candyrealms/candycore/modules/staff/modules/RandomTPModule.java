package com.candyrealms.candycore.modules.staff.modules;

import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomTPModule {

    public void randomTeleportPlayer(Player player) {
        List<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !player.equals(p))
                .collect(Collectors.toList());

        if(onlinePlayers.isEmpty()) {
            player.sendMessage(ColorUtil.color("&d&lCandy&5&lStaff &fThere are no available players."));
            return;
        }

        player.teleport(onlinePlayers.get(new Random().nextInt(onlinePlayers.size())));
    }
}
