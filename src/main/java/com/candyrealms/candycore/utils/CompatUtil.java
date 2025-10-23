package com.candyrealms.candycore.utils;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CompatUtil {

    private CompatUtil() {}

    public static Material mat(String... candidates) {
        if (candidates == null) return Material.STONE;
        for (String name : candidates) {
            try {
                Material m = Material.matchMaterial(name);
                if (m != null) return m;
            } catch (Throwable ignored) {}
        }
        return Material.STONE;
    }

    public static ItemStack glassPaneBlack() {
        return new ItemStack(mat("STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE"), 1, (short) 15);
    }

    public static void play(Player player, float volume, float pitch, String... candidates) {
        if (player == null || candidates == null) return;
        Sound sound = null;
        for (String name : candidates) {
            try {
                sound = Sound.valueOf(name);
                break;
            } catch (IllegalArgumentException ignored) {}
        }
        if (sound == null) return;
        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Throwable ignored) {}
    }

    public static boolean isSkull(Material material) {
        if (material == null) return false;
        String n = material.name();
        return n.equals("SKULL_ITEM") || n.equals("PLAYER_HEAD") || n.endsWith("_HEAD") || n.contains("SKULL");
    }
}

