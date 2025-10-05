package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.modules.shards.ShardsModule;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

@CommandAlias("shards|shard|giveshard|giveshards")
public class ShardsCommand extends BaseCommand {

    private final ShardsModule module;

    public ShardsCommand(CandyCore plugin) {
        module = plugin.getModuleManager().getShardsModule();
    }

    @Default
    @Subcommand("give")
    @Description("gives player a shard based on config")
    @CommandPermission("candycore.admin")
    @CommandCompletion("@players @shards")
    @Syntax("<player> <shard>")
    public void onGive(CommandSender sender, OnlinePlayer player, String shardName) {
        if(!module.isValidShard(shardName)) {
            sender.sendMessage(ColorUtil.color("&5&lSHARDS &fThis is an invalid shard name!"));
            return;
        }

        module.giveShard(player.getPlayer(), shardName);

        player.getPlayer().sendMessage(ColorUtil.color("&5&lSHARDS &fYou have received a shard!"));
        player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.LEVEL_UP, 7, 6);
    }

}
