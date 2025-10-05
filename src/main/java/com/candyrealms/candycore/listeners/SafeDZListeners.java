package com.candyrealms.candycore.listeners;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import me.fullpage.manticrods.api.events.RodUseEvent;
import me.fullpage.manticsword.api.events.SwordUseEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SafeDZListeners implements Listener {

    private final ConfigManager configManager;

    public SafeDZListeners(CandyCore plugin) {
        configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onSwordSell(SwordUseEvent event) {
        Player player = event.getPlayer();

        String world = player.getWorld().getName();

        if(!world.equalsIgnoreCase(configManager.getSafeDZWorld())) return;

        int reductionPercentage = configManager.getSafeDZReduction();

        double money = event.getMoney();

        event.setMoney(Math.max(0, money * (1 - ((double) reductionPercentage/100))));
    }

    @EventHandler
    public void onRodSell(RodUseEvent event) {
        Player player = event.getPlayer();

        String world = player.getWorld().getName();

        if(!world.equalsIgnoreCase(configManager.getSafeDZWorld())) return;

        int reductionPercentage = configManager.getSafeDZReduction();

        double money = event.getMoney();

        event.setMoney(Math.max(0, money * (1 - ((double) reductionPercentage/100))));
    }
}
