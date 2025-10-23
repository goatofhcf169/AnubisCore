package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.candyrealms.candycore.AnubisCore;

@CommandAlias("clearchat|chatclear")
public class ClearChatCommand extends BaseCommand {

    private final AnubisCore plugin;

    public ClearChatCommand(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @Default
    @CommandPermission("anubiscore.chatclear")
    public void onClear() {
        plugin.getModuleManager().getChatModerationModule().clearChat();
    }
}

