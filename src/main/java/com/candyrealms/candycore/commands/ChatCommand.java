package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.modules.staff.StaffModule;
import com.candyrealms.candycore.utils.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("chat|cchat|candychat")
public class ChatCommand extends BaseCommand {

    private final StaffModule module;

    public ChatCommand(CandyCore plugin) {
        module = plugin.getModuleManager().getStaffModule();
    }

    @Default
    @Subcommand("help")
    @CommandPermission("candycore.admin")
    public void onChat(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage(ColorUtil.color("&d&lCandy&5&lCore &f- &eChat Module"));
        sender.sendMessage(" ");
        sender.sendMessage(ColorUtil.color("&d- /chat clear &f- &eClears the chat"));
        sender.sendMessage(ColorUtil.color("&d- /chat toggle &f- &eMutes or Unmutes Chat"));
        sender.sendMessage(ColorUtil.color("&d- /chat staff &f- &eToggles the staff chat"));
        sender.sendMessage(ColorUtil.color("&d- /chat admin &f- &eToggles the admin chat"));
        sender.sendMessage(" ");
    }

    @Subcommand("clear|chatclear|clearchat")
    @CommandAlias("clearchat")
    @CommandPermission("candycore.chatclear")
    public void onClear(CommandSender sender) {
        module.clearChat();
    }

    @Subcommand("toggle|mute|unmute")
    @CommandAlias("mutechat")
    @CommandPermission("candycore.mutechat")
    public void onToggle(CommandSender sender) {
        module.toggleMute();
    }

    @Subcommand("staff")
    @CommandPermission("candycore.staffchat")
    @CommandAlias("staffchat|sc|chatstaff")
    public void onStaffChat(Player sender) {
        module.toggleStaffChat(sender);
    }

    @Subcommand("admin")
    @CommandPermission("candycore.adminchat")
    @CommandAlias("adminchat|ac|chatadmin")
    public void onAdminChat(Player sender) {
        module.toggleAdminChat(sender);
    }
}
