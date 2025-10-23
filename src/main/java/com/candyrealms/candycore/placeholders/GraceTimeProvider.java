package com.candyrealms.candycore.placeholders;

import com.candyrealms.candycore.AnubisCore;
import com.golfing8.kore.FactionsKore;
import com.golfing8.kore.feature.GracePeriodFeature;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GraceTimeProvider {

    private static final DateTimeFormatter CONFIG_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static long resolveRemainingSeconds(AnubisCore plugin, OfflinePlayer player) {
        String source = plugin.getConfig().getString("grace.source", "auto").toLowerCase(Locale.ROOT);

        if (source.equals("papi") || (source.equals("auto") && useExternalPapi(plugin))) {
            long s = fromExternalPapi(plugin, player);
            if (s >= 0) return s;
        }

        if (source.equals("kore") || (source.equals("auto") && Bukkit.getPluginManager().getPlugin("FactionsKore") != null)) {
            long s = fromFactionsKore();
            if (s >= 0) return s;
        }

        // Fallback to config
        return fromConfig(plugin);
    }

    private static boolean useExternalPapi(AnubisCore plugin) {
        String ph = plugin.getConfig().getString("grace.papi_placeholder", "");
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && ph != null && !ph.trim().isEmpty();
    }

    private static long fromExternalPapi(AnubisCore plugin, OfflinePlayer player) {
        try {
            String placeholder = plugin.getConfig().getString("grace.papi_placeholder", "");
            if (placeholder == null || placeholder.trim().isEmpty()) return -1;
            String token = placeholder.trim();
            if (!token.startsWith("%")) token = "%" + token + "%";
            String raw = PlaceholderAPI.setPlaceholders(player != null ? player : null, token);
            return parseToSeconds(raw);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static long fromFactionsKore() {
        try {
            GracePeriodFeature feature = FactionsKore.get().getFeature(GracePeriodFeature.class);
            if (feature == null) return -1;
            ZonedDateTime end = feature.getGraceEndDate();
            if (end == null) return -1;
            long seconds = Duration.between(ZonedDateTime.now(end.getZone()), end).getSeconds();
            return Math.max(0, seconds);
        } catch (Throwable t) {
            return -1;
        }
    }

    private static long fromConfig(AnubisCore plugin) {
        try {
            String endsAtStr = plugin.getConfig().getString("grace.ends_at", "");
            String zoneIdStr = plugin.getConfig().getString("grace.timezone", "America/New_York");
            if (endsAtStr == null || endsAtStr.trim().isEmpty()) return 0;
            ZoneId zone = ZoneId.of(zoneIdStr);
            LocalDateTime localEnd = LocalDateTime.parse(endsAtStr, CONFIG_FMT);
            ZonedDateTime end = localEnd.atZone(zone);
            long seconds = Duration.between(ZonedDateTime.now(zone), end).getSeconds();
            return Math.max(0, seconds);
        } catch (Exception e) {
            return 0;
        }
    }

    // Attempts to parse a variety of common time formats into seconds
    private static long parseToSeconds(String input) {
        if (input == null) return -1;
        String s = stripColors(input).trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return -1;

        // digits only -> seconds
        if (s.matches("^\\d+$")) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) { }
        }

        // HH:MM:SS
        if (s.matches("^\\d{1,2}:\\d{2}:\\d{2}$")) {
            String[] parts = s.split(":");
            long h = Long.parseLong(parts[0]);
            long m = Long.parseLong(parts[1]);
            long sec = Long.parseLong(parts[2]);
            return h * 3600 + m * 60 + sec;
        }

        // D day(s), HH:MM:SS (or "Xd, HH:MM:SS")
        Matcher dHms = Pattern.compile("^(\\d+)\\s*(day|days|d)[, ]+\\s*(\\d{1,2}):([0-5]\\d):([0-5]\\d)").matcher(s);
        if (dHms.find()) {
            long d = Long.parseLong(dHms.group(1));
            long h = Long.parseLong(dHms.group(3));
            long m = Long.parseLong(dHms.group(4));
            long sec = Long.parseLong(dHms.group(5));
            return d * 86400 + h * 3600 + m * 60 + sec;
        }

        // Tokens like 1d 2h 3m 4s or 2 days 5 hours ...
        long total = 0;
        boolean matched = false;
        Matcher token = Pattern.compile("(\\d+)\\s*(d|day|days|h|hour|hours|m|min|mins|minute|minutes|s|sec|secs|second|seconds)").matcher(s);
        while (token.find()) {
            matched = true;
            long val = Long.parseLong(token.group(1));
            String unit = token.group(2);
            switch (unit.charAt(0)) {
                case 'd': total += val * 86400; break;
                case 'h': total += val * 3600; break;
                case 'm': total += val * 60; break;
                case 's': total += val; break;
            }
        }
        if (matched) return total;

        return -1; // unknown format
    }

    private static String stripColors(String s) {
        // Remove common color codes
        return s.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }
}

