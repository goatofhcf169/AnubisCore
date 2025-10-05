package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.revive.ReviveModule;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("revive")
public class ReviveCommand extends BaseCommand {

    private final AnubisCore plugin;

    public ReviveCommand(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @Default
    @CommandPermission("anubiscore.admin")
    @CommandCompletion("@players")
    @Syntax("<player>")
    public void onRevive(Player sender, OnlinePlayer targetArg) {
        Player target = targetArg.getPlayer();

        ReviveModule module = plugin.getModuleManager().getReviveModule();

        if (module.getDeaths(target.getUniqueId()).isEmpty()) {
            sender.sendMessage(ColorUtil.color("&d&lRevive &fNo death history found for &d" + target.getName() + "&f."));
            return;
        }

        module.openDeathsMenu(sender, target);
    }
}

