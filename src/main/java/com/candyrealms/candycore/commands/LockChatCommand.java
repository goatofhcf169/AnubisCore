package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.candyrealms.candycore.AnubisCore;

@CommandAlias("lockchat")
public class LockChatCommand extends BaseCommand {

    private final AnubisCore plugin;

    public LockChatCommand(AnubisCore plugin) { this.plugin = plugin; }

    @Default
    @CommandPermission("anubiscore.lockchat")
    public void onToggle() {
        plugin.getModuleManager().getChatModerationModule().toggleLock();
    }
}

