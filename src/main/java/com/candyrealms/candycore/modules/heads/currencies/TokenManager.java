package com.candyrealms.candycore.modules.heads.currencies;

import com.candyrealms.candycore.CandyCore;
import me.fullpage.mantichoes.data.MPlayers;
import me.fullpage.mantichoes.wrappers.MPlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class TokenManager {

    private final CandyCore plugin;

    private FileConfiguration config;

    public TokenManager(CandyCore plugin) {
        this.plugin = plugin;

        config = plugin.getHeadsCFG().getConfig();
    }

    public double getDeductedTokens(Player player) {
        MPlayer mPlayer = MPlayers.get(player.getUniqueId());

        if(mPlayer == null) return 0;

        double playerTokens = mPlayer.getAccurateTokens();

        if(playerTokens < 1000) return 0;

        double deductionPercent = ((double) config.getInt("token-deduction")/100);

        double deduction = (playerTokens * deductionPercent);

        mPlayer.setTokens(Math.max(0, playerTokens - deduction));

        return deduction;
    }

    public void addTokens(Player player, double tokens) {
        MPlayer mPlayer = MPlayers.get(player.getUniqueId());

        if(mPlayer == null) return;

        mPlayer.addTokens(tokens);
    }

    public void reloadConfig() {
        config = plugin.getHeadsCFG().getConfig();
    }
}
