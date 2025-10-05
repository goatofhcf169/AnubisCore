package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.donator.DonatorModule;
import org.bukkit.command.CommandSender;

@CommandAlias("donate|donated|donation|givedonation")
public class DonateCommand extends BaseCommand {

    private final DonatorModule module;

    public DonateCommand(AnubisCore plugin) {
        module = plugin.getModuleManager().getDonatorModule();
    }

    @Default
    @CommandPermission("anubiscore.admin")
    @CommandCompletion("@players")
    public void onDonate(CommandSender sender, OnlinePlayer player) {
        module.donate(player.getPlayer().getUniqueId());
    }
}
