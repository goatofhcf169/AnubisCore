package com.candyrealms.candycore.modules.heads.currencies;


import com.candyrealms.candycore.AnubisCore;
import me.fullpage.manticsword.data.MPlayers;
import me.fullpage.manticsword.wrappers.MPlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class CrystalsManager {

    private final AnubisCore plugin;

    private FileConfiguration config;

    public CrystalsManager(AnubisCore plugin) {
        this.plugin = plugin;

        config = plugin.getHeadsCFG().getConfig();
    }

    public long getDeductedCrystals(Player player) {
        MPlayer mPlayer = MPlayers.get(player.getUniqueId());

        if(mPlayer == null) return 0;

        long playerCrystals = mPlayer.getCrystals();

        if(playerCrystals < 1000) return 0;

        double deductionPercent = ((double) config.getInt("crystals-deduction")/100);

        long deduction = (long) (playerCrystals * deductionPercent);

        mPlayer.setCrystals(Math.max(0, playerCrystals - deduction));

        return deduction;
    }

    public void addCrystals(Player player, long crystals) {
        MPlayer mPlayer = MPlayers.get(player.getUniqueId());

        if(mPlayer == null) return;

        mPlayer.addCrystals(crystals);
    }

    public void reloadConfig() {
        config = plugin.getHeadsCFG().getConfig();
    }
}
