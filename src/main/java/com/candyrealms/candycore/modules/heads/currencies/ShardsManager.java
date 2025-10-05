package com.candyrealms.candycore.modules.heads.currencies;

import com.candyrealms.candycore.CandyCore;
import me.fullpage.manticrods.data.MPlayers;
import me.fullpage.manticrods.wrappers.MPlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ShardsManager {

    private final CandyCore plugin;

    private FileConfiguration config;

    public ShardsManager(CandyCore plugin) {
        this.plugin = plugin;

        config = plugin.getHeadsCFG().getConfig();
    }

    public long getDeductedShards(Player player) {
        MPlayer mPlayer = MPlayers.get(player.getUniqueId());

        if(mPlayer == null) return 0;

        long playerShards = mPlayer.getShards();

        if(playerShards < 1000) return 0;

        double deductionPercentage = ((double) config.getInt("shard-deduction")/100);

        long deduction = (long) (playerShards * deductionPercentage);

        mPlayer.setShards(Math.max(0, playerShards - deduction));

        return deduction;
    }

    public void addShards(Player player, long shards) {
        MPlayer mPlayer = MPlayers.get(player.getUniqueId());

        if(mPlayer == null) return;

        mPlayer.addShards(shards);
    }

    public void reloadConfig() {
        config = plugin.getHeadsCFG().getConfig();
    }
}
