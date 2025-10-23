package com.candyrealms.candycore.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class ActionBarUtil {

    private ActionBarUtil() {}

    public static void send(Player player, String message) {
        if (player == null || message == null) return;

        String colored = ColorUtil.color(message);

        // 1) Paper Adventure API (1.17+), if available
        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            // Try Component.text(CharSequence) first, then Component.text(String)
            Object component;
            try {
                java.lang.reflect.Method textMethod = componentClass.getMethod("text", CharSequence.class);
                component = textMethod.invoke(null, colored);
            } catch (NoSuchMethodException ignored) {
                java.lang.reflect.Method textMethod = componentClass.getMethod("text", String.class);
                component = textMethod.invoke(null, colored);
            }
            java.lang.reflect.Method sendActionBar = player.getClass().getMethod("sendActionBar", componentClass);
            sendActionBar.invoke(player, component);
            return;
        } catch (Throwable ignored) { }

        // Try common ActionBarAPI plugins via reflection
        String[] apiClasses = new String[] {
                "me.connorlinfoot.actionbarapi.ActionBarAPI",
                "com.connorlinfoot.actionbarapi.ActionBarAPI",
                "me.rayzr522.actionbarapi.ActionBarAPI"
        };
        for (String cls : apiClasses) {
            try {
                Class<?> c = Class.forName(cls);
                Method m = c.getMethod("sendActionBar", Player.class, String.class);
                m.invoke(null, player, colored);
                return;
            } catch (Throwable ignored) { }
        }

        // Try Spigot API (available on newer builds)
        try {
            Class<?> chatMsgType = Class.forName("net.md_5.bungee.api.ChatMessageType");
            // Prefer ACTION_BAR when present; otherwise fall back to GAME_INFO
            Object typeEnum;
            try {
                typeEnum = Enum.valueOf((Class<Enum>) chatMsgType, "ACTION_BAR");
            } catch (IllegalArgumentException e) {
                typeEnum = Enum.valueOf((Class<Enum>) chatMsgType, "GAME_INFO");
            }
            Class<?> baseComp = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
            Class<?> textComp = Class.forName("net.md_5.bungee.api.chat.TextComponent");
            Method fromLegacy = textComp.getMethod("fromLegacyText", String.class);
            Object comps = fromLegacy.invoke(null, colored);
            Method spigot = player.getClass().getMethod("spigot");
            Object spigotInst = spigot.invoke(player);
            Class<?> baseCompArray = java.lang.reflect.Array.newInstance(baseComp, 0).getClass();
            Method sendMsg = spigotInst.getClass().getMethod("sendMessage", chatMsgType, baseCompArray);
            sendMsg.invoke(spigotInst, typeEnum, comps);
            return;
        } catch (Throwable ignored) { }

        // Fallback to NMS (1.8+)
        try {
            String ver = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit." + ver + ".entity.CraftPlayer");
            Class<?> iChatBase = Class.forName("net.minecraft.server." + ver + ".IChatBaseComponent");
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + ver + ".IChatBaseComponent$ChatSerializer");
            Class<?> packetPlayOutChat = Class.forName("net.minecraft.server." + ver + ".PacketPlayOutChat");
            Object ichat = chatSerializer.getMethod("a", String.class)
                    .invoke(null, "{\"text\":\"" + colored.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}");
            Object packet = packetPlayOutChat.getConstructor(iChatBase, byte.class).newInstance(ichat, (byte) 2);
            Object handle = craftPlayer.getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            Class<?> packetClass = Class.forName("net.minecraft.server." + ver + ".Packet");
            connection.getClass().getMethod("sendPacket", packetClass).invoke(connection, packet);
        } catch (Throwable ignored) { }
    }
}
