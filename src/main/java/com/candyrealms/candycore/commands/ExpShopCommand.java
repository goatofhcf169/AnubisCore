package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.modules.expshop.ExpShopModule;
import org.bukkit.entity.Player;

@CommandAlias("expshop|xpshop")
public class ExpShopCommand extends BaseCommand {

    private final ExpShopModule module;

    public ExpShopCommand(CandyCore plugin) {
        module = plugin.getModuleManager().getExpShopModule();
    }

    @Default
    public void onShop(Player sender) {
        module.openMenu(sender);
    }
}
