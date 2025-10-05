package com.candyrealms.candycore.listeners;

import com.golfing8.kore.FactionsKore;
import com.golfing8.kore.event.GenBlockUseEvent;
import com.golfing8.kore.event.roam.PlayerRoamExitEvent;
import com.golfing8.kore.feature.RaidingOutpostFeature;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class FactionListeners implements Listener {

    /**
     * Fixes an issue where roaming in enemy land will disable
     * your flight when you exit roam in areas you can normally fly in.
     * @param event
     */
    @EventHandler
    public void onRoamExit(PlayerRoamExitEvent event) {
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(event.getPlayer());

        if(!fPlayer.canFlyAtLocation()) return;

        event.getPlayer().setAllowFlight(true);

        event.getPlayer().setFlying(true);
        fPlayer.setFlying(true);
    }

//    /**
//     * Fixes an issue where flight is disabled when spectating a raid
//     * through /f spectate <raid>
//     * @param event
//     */
//    @EventHandler
//    public void onTryFly(PlayerMoveEvent event) {
//        Block initialBlock = event.getFrom().clone().subtract(0, 1, 0).getBlock();
//        Block toBlock = event.getTo().clone().subtract(0, 1, 0).getBlock();
//
//        if(initialBlock.equals(toBlock)) return;
//
//        RaidSpectateFeature feature = FactionsKore.get().getFeature(RaidSpectateFeature.class);
//
//        if(!feature.isActivelySpectating(event.getPlayer())) return;
//
//        Player spectator = event.getPlayer();
//        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(spectator);
//
//        if(spectator.isFlying()) return;
//
//        spectator.setAllowFlight(true);
//        fPlayer.setFlying(true);
//        spectator.setFlying(true);
//    }

    @EventHandler
    public void onRpostGen(GenBlockUseEvent event) {
        Location placedLocation = event.getUsedAt();

        RaidingOutpostFeature feature = FactionsKore.get().getFeature(RaidingOutpostFeature.class);

        if(feature == null) return;
        if(feature.getOutpost().getWorld().getName() == null) return;
        
        String rpostWorldName = feature.getOutpost().getWorld().getName();
        String placedWorldName = placedLocation.getWorld().getName();

        if(!rpostWorldName.equalsIgnoreCase(placedWorldName)) return;

    }
}
