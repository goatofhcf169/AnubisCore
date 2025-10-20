package com.candyrealms.candycore.modules.chat;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.utils.ColorUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import java.util.Locale;

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
        // Try registering Paper's AsyncChatEvent dynamically if present (Paper 1.19+)
        tryRegisterPaperAsyncChat();
    }

    // Dynamically register Paper's AsyncChatEvent without hard dependency to maintain 1.8+ compatibility
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void tryRegisterPaperAsyncChat() {
        try {
            final Class<?> asyncChatClass = Class.forName("io.papermc.paper.event.player.AsyncChatEvent");

            EventExecutor executor = (listener, event) -> {
                if (!asyncChatClass.isInstance(event)) return;
                try {
                    Player player = (Player) asyncChatClass.getMethod("getPlayer").invoke(event);

                    // Lock enforcement
                    if (locked && !(player.hasPermission("anubiscore.lockchat.bypass") || player.hasPermission("anubiscore.admin"))) {
                        asyncChatClass.getMethod("setCancelled", boolean.class).invoke(event, true);
                        player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat is currently &c&nlocked&f."));
                        return;
                    }

                    // Mute enforcement
                    if (muted && !(player.hasPermission("anubiscore.chat.bypass") || player.hasPermission("anubiscore.admin"))) {
                        asyncChatClass.getMethod("setCancelled", boolean.class).invoke(event, true);
                        player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat is currently &c&nmuted&f."));
                        return;
                    }

                    // Slow mode enforcement
                    long delay = slowDelaySeconds;
                    if (delay > 0 && !(player.hasPermission("anubiscore.chat.bypass") || player.hasPermission("anubiscore.admin"))) {
                        long now = System.currentTimeMillis();
                        long last = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);
                        long waitMillis = delay * 1000L;
                        if (now - last < waitMillis) {
                            long remainingSec = Math.max(0L, (waitMillis - (now - last) + 999) / 1000);
                            asyncChatClass.getMethod("setCancelled", boolean.class).invoke(event, true);
                            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat is in slow mode. Please wait &d" + remainingSec + "s&f."));
                            return;
                        }
                        lastMessageTime.put(player.getUniqueId(), now);
                    }
                } catch (Throwable t) {
                    // If anything goes wrong, fail silently to avoid breaking chat on incompatible servers
                }
            };

            // Register at LOWEST so we can cancel before other chat formatters
            Bukkit.getPluginManager().registerEvent((Class) asyncChatClass, this, EventPriority.LOWEST, executor, plugin);
        } catch (ClassNotFoundException ignored) {
            // Not running on Paper with AsyncChatEvent; nothing to do.
        } catch (Throwable ignored) {
            // Any other issue: skip registration to remain safe on legacy servers
        }
    }

    // Enforcement
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (locked && !(player.hasPermission("anubiscore.lockchat.bypass") || player.hasPermission("anubiscore.admin"))) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat is currently &c&nlocked&f."));
            return;
        }

        if (muted && !(player.hasPermission("anubiscore.chat.bypass") || player.hasPermission("anubiscore.admin"))) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat is currently &c&nmuted&f."));
            return;
        }

        long delay = slowDelaySeconds;
        if (delay > 0 && !(player.hasPermission("anubiscore.chat.bypass") || player.hasPermission("anubiscore.admin"))) {
            long now = System.currentTimeMillis();
            long last = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);
            long waitMillis = delay * 1000L;
            if (now - last < waitMillis) {
                long remainingSec = Math.max(0L, (waitMillis - (now - last) + 999) / 1000);
                event.setCancelled(true);
                player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat is in slow mode. Please wait &d" + remainingSec + "s&f."));
                return;
            }
            lastMessageTime.put(player.getUniqueId(), now);
        }
    }

    // Prevent bypass via chat-like commands such as /me
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommandChat(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.isEmpty() || raw.charAt(0) != '/') return;

        String label = raw.substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1); // handle namespaced commands e.g. minecraft:me

        // Only target commands that behave like public chat broadcasts
        if (!label.equals("me")) return;

        Player player = event.getPlayer();

        if (locked && !(player.hasPermission("anubiscore.lockchat.bypass") || player.hasPermission("anubiscore.admin"))) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat is currently &c&nlocked&f."));
            return;
        }

        if (muted && !(player.hasPermission("anubiscore.chat.bypass") || player.hasPermission("anubiscore.admin"))) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat is currently &c&nmuted&f."));
            return;
        }

        long delay = slowDelaySeconds;
        if (delay > 0 && !(player.hasPermission("anubiscore.chat.bypass") || player.hasPermission("anubiscore.admin"))) {
            long now = System.currentTimeMillis();
            long last = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);
            long waitMillis = delay * 1000L;
            if (now - last < waitMillis) {
                long remainingSec = Math.max(0L, (waitMillis - (now - last) + 999) / 1000);
                event.setCancelled(true);
                player.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat is in slow mode. Please wait &d" + remainingSec + "s&f."));
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
            Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat has been &c&nlocked&f."));
        } else {
            Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat has been &a&nunlocked&f."));
        }
    }

    public void setSlowDelay(long seconds) {
        this.slowDelaySeconds = Math.max(0L, seconds);
        if (seconds <= 0) {
            Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat slow mode has been &c&ndisabled&f."));
        } else {
            Bukkit.broadcastMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &fChat is now in slow mode: &d" + seconds + "s&f."));
        }
    }

    public void clearChat() {
        // push old messages off client chat
        for (int i = 0; i < 100; i++) {
            Bukkit.broadcastMessage("");
        }
        plugin.getConfigManager().announceChatCleared();
    }
}
