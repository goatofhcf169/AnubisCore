package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.command.CommandSender;

@CommandAlias("slowchat")
public class SlowChatCommand extends BaseCommand {

    private final AnubisCore plugin;

    public SlowChatCommand(AnubisCore plugin) { this.plugin = plugin; }

    @Default
    @Syntax("<duration|off> e.g. 10s, 2m, 1h, 1d, 1y or off")
    @CommandPermission("anubiscore.slowchat")
    public void onSet(CommandSender sender, String duration) {
        long seconds = parseDuration(duration);
        if (seconds < 0) {
            sender.sendMessage(ColorUtil.color("&d&lChat &fInvalid duration. Use &d10s&f/&d2m&f/&d1h&f/&d1d&f/&d1y&f or &doff&f."));
            return;
        }
        plugin.getModuleManager().getChatModerationModule().setSlowDelay(seconds);
    }

    private long parseDuration(String input) {
        if (input == null) return -1;
        input = input.trim().toLowerCase();
        if (input.equals("off") || input.equals("0")) return 0;
        try {
            // number without suffix defaults to seconds
            if (input.matches("^\\d+$")) return Long.parseLong(input);

            long factor;
            char unit = input.charAt(input.length() - 1);
            String numPart = input.substring(0, input.length() - 1);
            long value = Long.parseLong(numPart);
            switch (unit) {
                case 's': factor = 1; break;
                case 'm': factor = 60; break;
                case 'h': factor = 3600; break;
                case 'd': factor = 86400; break;
                case 'y': factor = 86400L * 365L; break;
                default: return -1;
            }
            return Math.max(0, value * factor);
        } catch (Exception e) {
            return -1;
        }
    }
}

