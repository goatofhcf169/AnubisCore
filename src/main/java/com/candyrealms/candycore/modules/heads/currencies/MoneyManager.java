package com.candyrealms.candycore.modules.heads.currencies;

import com.candyrealms.candycore.CandyCore;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.math.BigDecimal;


public class MoneyManager {

    private final CandyCore plugin;

    private final Essentials essentials;

    private FileConfiguration config;

    public MoneyManager(CandyCore plugin) {
        this.plugin = plugin;

        essentials = plugin.getEssentials();
        config = plugin.getHeadsCFG().getConfig();
    }

    public long getDeductedMoney(Player player) {
        if(essentials == null) return 0;

        User user = essentials.getUser(player.getUniqueId());

        if(user == null) return 0;

        long money = user.getMoney().longValue();

        if(money < 1000) return 0;

        double percentage = ((double) config.getInt("money-deduction") / 100);

        long deducted = (long) (money * percentage);

        try {
            user.setMoney(new BigDecimal(money-deducted));
        } catch (Exception exception) {
            return 0;
        }

        return deducted;
    }

    public void addMoney(Player player, long money) {
        User user = essentials.getUser(player.getUniqueId());

        try {
            user.giveMoney(new BigDecimal(money));
        } catch (Exception exception) {
            return;
        }
    }

    public void reloadConfig() {
        config = plugin.getHeadsCFG().getConfig();
    }
}
