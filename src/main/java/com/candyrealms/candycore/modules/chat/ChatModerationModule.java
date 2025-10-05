package com.candyrealms.candycore.modules.chat;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.utils.ColorUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatModerationModule implements Listener {

    private final AnubisCore plugin;

    @Getter
    private volatile boolean muted = false;

    @Getter
    private volatile boolean locked = false;

    @Getter
    private volatile long slowDelaySeconds = 0L; // 0 = off

    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();

    public ChatModerationModule(AnubisCore plugin) {
        this.plugin = plugin;
    }

    // Enforcement
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (locked && !player.hasPermission("anubiscore.admin")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&fChat is currently &c&nlocked&f."));
            return;
        }

        if (muted && !player.hasPermission("anubiscore.chat.bypass")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&fChat is currently &c&nmuted&f."));
            return;
        }

        long delay = slowDelaySeconds;
        if (delay > 0 && !player.hasPermission("anubiscore.chat.bypass")) {
            long now = System.currentTimeMillis();
            long last = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);
            long waitMillis = delay * 1000L;
            if (now - last < waitMillis) {
                long remainingSec = Math.max(0L, (waitMillis - (now - last) + 999) / 1000);
                event.setCancelled(true);
                player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&fChat is in slow mode. Please wait &d" + remainingSec + "s&f."));
                return;
            }
            lastMessageTime.put(player.getUniqueId(), now);
        }
    }

    // Controls
    public void toggleMute() {
        muted = !muted;
        if (muted) plugin.getConfigManager().announceChatMute(); else plugin.getConfigManager().announceChatUnmute();
    }

    public void toggleLock() {
        locked = !locked;
        if (locked) {
            Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&fChat has been &c&nlocked&f."));
        } else {
            Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&fChat has been &a&nunlocked&f."));
        }
    }

    public void setSlowDelay(long seconds) {
        this.slowDelaySeconds = Math.max(0L, seconds);
        if (seconds <= 0) {
            Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&fChat slow mode has been &c&ndisabled&f."));
        } else {
            Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&fChat is now in slow mode: &d" + seconds + "s&f."));
        }
    }
}

