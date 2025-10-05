package com.candyrealms.candycore.modules.elitearmor;

import me.fullpage.mantichoes.api.events.HoeUseEvent;
import net.splodgebox.elitearmor.EliteArmorAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.stream.Collectors;

public class ArmorListeners implements Listener {

    @EventHandler
    public void onManticSell(HoeUseEvent event) {
        Player player = event.getPlayer();
        if(player == null) return;

        if(EliteArmorAPI.getArmorAPI() == null) return;
        if(EliteArmorAPI.getArmorAPI().getActiveArmorSets(player) == null) return;
        if(EliteArmorAPI.getArmorAPI().getActiveArmorSets(player).isEmpty()) return;

        List<String> playerSets = EliteArmorAPI.getArmorAPI().getActiveArmorSets(player)
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if(!playerSets.contains("starter")) return;

        event.setAutosellMoney(event.getAutosellMoney() * 1.15);
    }

}
