package com.candyrealms.candycore.modules.heads;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.heads.currencies.CrystalsManager;
import com.candyrealms.candycore.modules.heads.currencies.MoneyManager;
import com.candyrealms.candycore.modules.heads.currencies.ShardsManager;
import com.candyrealms.candycore.modules.heads.currencies.TokenManager;
import com.candyrealms.candycore.utils.ItemCreator;
import com.earth2me.essentials.api.Economy;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.Getter;
import org.bukkit.Bukkit;
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

    private final AnubisCore plugin;

    @Getter
    private FileConfiguration config;

    private MoneyManager moneyManager;
    private CrystalsManager crystalsManager;
    private ShardsManager shardsManager;
    private TokenManager tokenManager;

    public HeadsModule(AnubisCore plugin) {
        this.plugin = plugin;

        config = plugin.getHeadsCFG().getConfig();

        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            moneyManager = new MoneyManager(plugin);
        }
        // Crystals and shards integration removed
        if (Bukkit.getPluginManager().getPlugin("ManticHoes") != null) {
            tokenManager = new TokenManager(plugin);
        }
    }

    public void addMoney(Player player, long money) {
        if (moneyManager != null)
            moneyManager.addMoney(player, money);
    }

    public void addTokens(Player player, double tokens) {
        if (tokenManager != null)
            tokenManager.addTokens(player, tokens);
    }

    public void addShards(Player player, long shards) {
        if (shardsManager != null)
            shardsManager.addShards(player, shards);
    }

    public void addCrystals(Player player, long crystals) {
        if (crystalsManager != null)
            crystalsManager.addCrystals(player, crystals);
    }

    public ItemStack getPlayerHead(Player player) {

        long takenMoney = moneyManager != null ? moneyManager.getDeductedMoney(player) : 0;
        long takenCrystals = 0;
        long takenShards = 0;
        double takenTokens = tokenManager != null ? tokenManager.getDeductedTokens(player) : 0;

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        String formattedMoney = (plugin.getEssentials() != null) ? Economy.format(new BigDecimal(takenMoney))
                : formatter.format(takenMoney);
        String formattedCrystals = formatter.format(takenCrystals);
        String formattedShards = formatter.format(takenShards);
        String formattedTokens = formatter.format(takenTokens);

        String name = config.getString("head-item.name", "&dHead of %player%").replace("%player%", player.getName());
        String texture = config.getString("head-item.texture", "");

        String matKey = config.getString("head-item.material", "SKULL_ITEM");
        Material material = Material.matchMaterial(matKey);
        if (material == null)
            material = Material.matchMaterial("PLAYER_HEAD");
        if (material == null)
            material = Material.matchMaterial("SKULL_ITEM");
        int data = config.getInt("head-item.data", 3);

        List<String> lore = config.getStringList("head-item.lore")
                .stream()
                .map(s -> s.replace("%money%", formattedMoney))
                .map(s -> s.replace("%tokens%", formattedTokens))
                .map(s -> s.replace("%mantichoes_tokens%", formattedTokens))
                .map(s -> s.replace("%shards%", formattedShards))
                .map(s -> s.replace("%crystals%", formattedCrystals))
                .collect(Collectors.toList());

        ItemStack itemStack = new ItemCreator(material, name, 1, data, texture, lore).getItem();

        NBTItem nbtItem = new NBTItem(itemStack);

        NBTCompound compound = nbtItem.getOrCreateCompound("CandyHeads");

        compound.setLong("money", takenMoney);
        compound.setDouble("tokens", takenTokens);
        return nbtItem.getItem();
    }

    public void reloadConfig() {
        config = plugin.getHeadsCFG().getConfig();
        if (moneyManager != null)
            moneyManager.reloadConfig();
        if (crystalsManager != null)
            crystalsManager.reloadConfig();
        if (shardsManager != null)
            shardsManager.reloadConfig();
        if (tokenManager != null)
            tokenManager.reloadConfig();
    }
}
