package com.candyrealms.candycore.modules.debug;

import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DebugModule {

    private final Set<UUID> debuggedPlayers = new HashSet<>();

    public void addDebugPlayer(UUID playerUUID) {
        debuggedPlayers.add(playerUUID);
    }

    public void removeDebugPlayer(UUID playerUUID) {
        debuggedPlayers.remove(playerUUID);
    }

    public boolean hasDebug(UUID playerUUID) {
        return debuggedPlayers.contains(playerUUID);
    }

    public void sendDamageDebugMSG(Player player, String h1, String h2, String h3) {
        player.sendMessage(" ");
        player.sendMessage(ColorUtil.color("&d&lCandy&5&lRealms &e- &fDamage Debug"));
        player.sendMessage(" ");
        player.sendMessage(ColorUtil.color("&7Health before: &f" + h1));
        player.sendMessage(ColorUtil.color("&7Health after: &f" + h2));
        player.sendMessage(" ");
        player.sendMessage(ColorUtil.color("&7Hearts dealt: &f" + h3));
        player.sendMessage(" ");
    }
}
