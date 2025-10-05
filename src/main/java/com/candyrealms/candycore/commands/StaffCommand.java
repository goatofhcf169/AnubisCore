package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.modules.staff.StaffModule;
import com.candyrealms.candycore.modules.staff.modules.FreezeModule;
import com.candyrealms.candycore.modules.staff.modules.RandomTPModule;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

@CommandAlias("staff|candystaff")
public class StaffCommand extends BaseCommand {

    private final CandyCore plugin;

    private final StaffModule staffModule;

    public StaffCommand(CandyCore plugin) {
        this.plugin = plugin;

        staffModule = plugin.getModuleManager().getStaffModule();
    }

    @Default
    @CommandPermission("candycore.staffmode")
    @CommandAlias("staffmode")
    public void onStaff(Player sender) {
        staffModule.getStaffModeModule().toggleStaffMode(sender);
    }

    @Subcommand("rtp")
    @CommandPermission("candycore.staffmode")
    @CommandAlias("rtp")
    public void onRTP(Player sender) {
        RandomTPModule randomTPModule = staffModule.getRandomTPModule();

        randomTPModule.randomTeleportPlayer(sender);
    }

    @Subcommand("freeze")
    @CommandPermission("candycore.staffmode")
    @CommandAlias("freeze")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void onFreeze(Player sender, OnlinePlayer receiver) {
        Player player = receiver.getPlayer();

        FreezeModule freezeModule = staffModule.getFreezeModule();

        FileConfiguration config = plugin.getStaffCFG().getConfig();

        if(freezeModule.isUUIDFrozen(player.getUniqueId())) {
            freezeModule.removeFrozenUUID(player.getUniqueId());

            List<String> messages = config.getStringList("messages.unfreeze-msg")
                    .stream()
                    .map(ColorUtil::color)
                    .collect(Collectors.toList());

            messages.forEach(player::sendMessage);

            player.playSound(player.getLocation(), Sound.NOTE_BASS, 7, 7);

            sender.sendMessage(ColorUtil.color("&5&lSTAFF &fYou have unfroze a player."));
            return;
        }

        freezeModule.addFrozenUUID(player.getUniqueId());

        List<String> messages = config.getStringList("messages.frozen-msg")
                .stream()
                .map(ColorUtil::color)
                .collect(Collectors.toList());

        messages.forEach(player::sendMessage);

        player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 7, 6);
        sender.sendMessage(ColorUtil.color("&5&lSTAFF &fYou have froze a player."));
    }
}
