package com.candyrealms.candycore.modules.staff.listeners;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import com.candyrealms.candycore.modules.staff.StaffModule;
import com.candyrealms.candycore.modules.staff.enums.ChatType;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListeners implements Listener {

    private final ConfigManager config;

    private final StaffModule module;

    public ChatListeners(CandyCore plugin) {
        module = plugin.getModuleManager().getStaffModule();
        config = plugin.getConfigManager();
    }

    @EventHandler
    public void onChatType(AsyncPlayerChatEvent event) {
        if(!module.isMuted() || event.getPlayer().hasPermission("candycore.admin")) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        player.sendMessage(ColorUtil.color(config.getPrefix() + " &fThe chat is currently muted."));
        player.playSound(player.getLocation(), Sound.NOTE_BASS, 7, 7);
    }

    @EventHandler
    public void onSpecialChat(AsyncPlayerChatEvent event) {
        if(!module.inSpecialChat(event.getPlayer())) return;

        module.sendSpecialMsg(event.getPlayer(), event.getMessage());

        event.setCancelled(true);
    }
}
