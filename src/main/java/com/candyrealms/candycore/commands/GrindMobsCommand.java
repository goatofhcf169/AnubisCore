package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.grindmobs.GrindMobsModule;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.command.CommandSender;

@CommandAlias("grindmobs|grindmob|gmobs|gmob")
@Description("Manage the GrindMobs event")
public class GrindMobsCommand extends BaseCommand {

    private final AnubisCore plugin;

    public GrindMobsCommand(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @Subcommand("start")
    @CommandPermission("anubiscore.admin")
    @Syntax("<time>  e.g. 30s, 10m, 2h, 1d")
    @Description("Start GrindMobs for the specified duration")
    public void onStart(CommandSender sender, String time) {
        GrindMobsModule module = plugin.getModuleManager().getGrindMobsModule();
        if (module == null) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&cGrindMobs module is not available."));
            return;
        }
        long ms = GrindMobsModule.parseDurationToMillis(time);
        if (ms <= 0) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&cInvalid time. Use e.g. &e30s&c, &e10m&c, &e2h&c, &e1d&c."));
            return;
        }
        module.startForMillis(ms);
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&aStarted GrindMobs for &f" + GrindMobsModule.formatDuration(ms) + "&a."));
    }

    @Subcommand("stop")
    @CommandPermission("anubiscore.admin")
    @Description("Stop the active GrindMobs event")
    public void onStop(CommandSender sender) {
        GrindMobsModule module = plugin.getModuleManager().getGrindMobsModule();
        if (module == null) return;
        if (!module.isActive()) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&7No active GrindMobs event."));
            return;
        }
        module.stop();
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&aStopped GrindMobs."));
    }

    @Subcommand("status")
    @CommandPermission("anubiscore.admin")
    @Description("Show GrindMobs status")
    public void onStatus(CommandSender sender) {
        GrindMobsModule module = plugin.getModuleManager().getGrindMobsModule();
        if (module == null) return;
        if (!module.isActive()) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&7GrindMobs is &cINACTIVE&7."));
            return;
        }
        long rem = module.getRemainingMillis();
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&aGrindMobs is &aACTIVE&7. Remaining: &f" + GrindMobsModule.formatDuration(rem)));
    }
}

