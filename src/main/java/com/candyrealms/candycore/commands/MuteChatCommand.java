package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.candyrealms.candycore.AnubisCore;

@CommandAlias("mutechat")
public class MuteChatCommand extends BaseCommand {

    private final AnubisCore plugin;

    public MuteChatCommand(AnubisCore plugin) { this.plugin = plugin; }

    @Default
    @CommandPermission("anubiscore.mutechat")
    public void onToggle() {
        plugin.getModuleManager().getChatModerationModule().toggleMute();
    }
}

