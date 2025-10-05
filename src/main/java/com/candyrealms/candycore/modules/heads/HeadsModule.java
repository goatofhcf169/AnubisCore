package com.candyrealms.candycore.modules.heads;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.modules.heads.currencies.CrystalsManager;
import com.candyrealms.candycore.modules.heads.currencies.MoneyManager;
import com.candyrealms.candycore.modules.heads.currencies.ShardsManager;
import com.candyrealms.candycore.modules.heads.currencies.TokenManager;
import com.candyrealms.candycore.utils.ItemCreator;
import com.earth2me.essentials.api.Economy;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HeadsModule {

    private final CandyCore plugin;

    @Getter
    private FileConfiguration config;

    private final MoneyManager moneyManager;
    private final CrystalsManager crystalsManager;
    private final ShardsManager shardsManager;
    private final TokenManager tokenManager;

    public HeadsModule(CandyCore plugin) {
        this.plugin = plugin;

        config = plugin.getHeadsCFG().getConfig();

        moneyManager = new MoneyManager(plugin);
        crystalsManager = new CrystalsManager(plugin);
        shardsManager = new ShardsManager(plugin);
        tokenManager = new TokenManager(plugin);
    }

    public void addMoney(Player player, long money) {
        moneyManager.addMoney(player, money);
    }

    public void addTokens(Player player, double tokens) {
        tokenManager.addTokens(player, tokens);
    }

    public void addShards(Player player, long shards) {
        shardsManager.addShards(player, shards);
    }

    public void addCrystals(Player player, long crystals) {
        crystalsManager.addCrystals(player, crystals);
    }

    public ItemStack getPlayerHead(Player player) {

        long takenMoney = moneyManager.getDeductedMoney(player);
        long takenCrystals = crystalsManager.getDeductedCrystals(player);
        long takenShards = shardsManager.getDeductedShards(player);
        double takenTokens = tokenManager.getDeductedTokens(player);

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        String formattedMoney = Economy.format(new BigDecimal(takenMoney));
        String formattedCrystals = formatter.format(takenCrystals);
        String formattedShards = formatter.format(takenShards);
        String formattedTokens = formatter.format(takenTokens);

        String name = config.getString("head-item.name").replace("%player%", player.getName());
        String texture = config.getString("head-item.texture");

        Material material = Material.valueOf(config.getString("head-item.material"));
        int data = config.getInt("head-item.data");

        List<String> lore = config.getStringList("head-item.lore")
                .stream()
                .map(s -> s.replace("%money%", formattedMoney))
                .map(s -> s.replace("%tokens%", formattedTokens))
                .map(s -> s.replace("%shards%", formattedShards))
                .map(s -> s.replace("%crystals%", formattedCrystals))
                .collect(Collectors.toList());

        ItemStack itemStack = new ItemCreator(material, name, 1, data, texture, lore).getItem();

        NBTItem nbtItem = new NBTItem(itemStack);

        NBTCompound compound = nbtItem.getOrCreateCompound("CandyHeads");

        compound.setLong("money", takenMoney);
        compound.setLong("shards", takenShards);
        compound.setLong("crystals", takenCrystals);
        compound.setDouble("tokens", takenTokens);
        return nbtItem.getItem();
    }

    public void reloadConfig() {
        config = plugin.getConfig();
        moneyManager.reloadConfig();
        crystalsManager.reloadConfig();
        shardsManager.reloadConfig();
        tokenManager.reloadConfig();
    }
}
