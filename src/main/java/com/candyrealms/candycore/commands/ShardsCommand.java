package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.shards.ShardsModule;
import com.candyrealms.candycore.utils.ColorUtil;
import com.candyrealms.candycore.utils.CompatUtil;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

@CommandAlias("shards|shard|giveshard|giveshards")
public class ShardsCommand extends BaseCommand {

    private final ShardsModule module;

    public ShardsCommand(AnubisCore plugin) {
        module = plugin.getModuleManager().getShardsModule();
    }

    @Default
    @Subcommand("give")
    @Description("gives player a shard based on config")
    @CommandPermission("anubiscore.admin")
    @CommandCompletion("@players @shards")
    @Syntax("<player> <shard>")
    public void onGive(CommandSender sender, OnlinePlayer player, String shardName) {
        if(!module.isValidShard(shardName)) {
            sender.sendMessage(ColorUtil.color("&5&lSHARDS &8» &fThis is an invalid shard name!"));
            return;
        }

        module.giveShard(player.getPlayer(), shardName);

        player.getPlayer().sendMessage(ColorUtil.color("&5&lSHARDS &8» &fYou have received a shard!"));
        CompatUtil.play(player.getPlayer(), 7f, 6f, "LEVEL_UP", "ENTITY_PLAYER_LEVELUP");
    }

}
