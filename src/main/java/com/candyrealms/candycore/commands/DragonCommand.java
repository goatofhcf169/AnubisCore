package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.dragon.DragonModule;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("dragon|dragonevent")
@Description("Manage the Dragon Event")
public class DragonCommand extends BaseCommand {

    private final AnubisCore plugin;

    public DragonCommand(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @Subcommand("spawn")
    @CommandPermission("anubiscore.admin")
    @Description("Spawn the event Ender Dragon at your location")
    public void onSpawn(Player sender) {
        DragonModule module = plugin.getModuleManager().getDragonModule();
        module.spawnDragon(sender);
    }

    @Subcommand("despawn")
    @CommandPermission("anubiscore.admin")
    @Description("Force-despawn the active event dragon")
    public void onDespawn(CommandSender sender) {
        DragonModule module = plugin.getModuleManager().getDragonModule();
        if (!module.hasActiveDragon()) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &7No active dragon to despawn."));
            return;
        }
        module.despawnActiveDragon();
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + " &aDespawned the active dragon."));
    }
}

