package com.candyrealms.candycore.placeholders;

import com.candyrealms.candycore.AnubisCore;
import com.golfing8.kore.FactionsKore;
import com.golfing8.kore.feature.GracePeriodFeature;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class AnubisExpansion extends PlaceholderExpansion {

    private final AnubisCore plugin;
    private final DateTimeFormatter configTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AnubisExpansion(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        // Alias to match usage: %anubis_grace_time%
        return "anubis";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) return null;
        if (params.equalsIgnoreCase("grace_time")) {
            return formattedGraceTime();
        }
        return null;
    }

    private String formattedGraceTime() {
        String suffix = plugin.getConfig().getString("grace.display_label", "EST");
        long seconds = GraceTimeProvider.resolveRemainingSeconds(plugin, null);
        if (seconds < 0) seconds = 0;
        long days = seconds / 86_400; // 24 * 3600
        long remainder = seconds % 86_400;
        long hours = remainder / 3600;
        long minutes = (remainder % 3600) / 60;
        long secs = remainder % 60;
        String dayWord = (days == 1) ? "Day" : "Days";
        return String.format("%d %s, %02d:%02d:%02d (%s)", days, dayWord, hours, minutes, secs, suffix);
    }
}
