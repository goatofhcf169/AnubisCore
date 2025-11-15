package com.candyrealms.candycore.modules.heads;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.heads.currencies.MoneyManager;
import com.candyrealms.candycore.modules.heads.currencies.VulcanTokenManager;
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
    // Use only the Vulcan token manager. Keep it nullable if Vulcan isn't present.
    private VulcanTokenManager tokenManager;

    public HeadsModule(AnubisCore plugin) {
        this.plugin = plugin;
        this.config = plugin.getHeadsCFG().getConfig();

        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            moneyManager = new MoneyManager(plugin);
        }
        ensureTokenManager();
    }

    /** Detect and lazily initialize the Vulcan token manager if available. */
    private void ensureTokenManager() {
        boolean debug = config.getBoolean("debug-vulcan-tokens", false);
        boolean loaderPresent = Bukkit.getPluginManager().getPlugin("VulcanLoader") != null;
        boolean apiPluginPresent = Bukkit.getPluginManager().getPlugin("VulcanAPI") != null;

        boolean vulcanClassPresent = false;
        try {
            Class.forName("net.vulcandev.vulcanapi.vulcantools.VulcanToolsAPI");
            vulcanClassPresent = true;
        } catch (Throwable ignored) {}

        if (debug) {
            Bukkit.getConsoleSender().sendMessage(
                    "[AnubisCore-Heads] Vulcan detection: loader=" + loaderPresent +
                            ", apiPlugin=" + apiPluginPresent + ", classPresent=" + vulcanClassPresent
            );
        }

        if (vulcanClassPresent && tokenManager == null) {
            tokenManager = new VulcanTokenManager(plugin);
            if (debug) {
                Bukkit.getConsoleSender().sendMessage("[AnubisCore-Heads] Using VulcanTokenManager (VulcanToolsAPI detected)");
            }
        }
    }

    public void addMoney(Player player, long money) {
        if (moneyManager != null) {
            moneyManager.addMoney(player, money);
        }
    }

    public void addTokens(Player player, double tokens) {
        ensureTokenManager();
        if (tokenManager == null) return;
        try {
            tokenManager.addTokens(player, tokens);
        } catch (Throwable ignored) {}
    }

    public ItemStack getPlayerHead(Player player) {
        long takenMoney   = moneyManager != null ? moneyManager.getDeductedMoney(player) : 0L;
        long takenCrystals = 0L;
        long takenShards   = 0L;
        double takenTokens = 0.0;

        ensureTokenManager();
        if (tokenManager != null) {
            try {
                takenTokens = tokenManager.getDeductedTokens(player);
                if (config.getBoolean("debug-vulcan-tokens", false)) {
                    plugin.getLogger().info("[Heads] Deducted tokens via Vulcan for " + player.getName() + ": " + takenTokens);
                    Bukkit.getConsoleSender().sendMessage("[AnubisCore-Heads] Deducted tokens via Vulcan for " + player.getName() + ": " + takenTokens);
                }
            } catch (Throwable t) {
                if (config.getBoolean("debug-vulcan-tokens", false)) {
                    Bukkit.getConsoleSender().sendMessage("[AnubisCore-Heads] Token deduction error: " + t.getClass().getSimpleName());
                }
            }
        }

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        String formattedMoney = (plugin.getEssentials() != null)
                ? Economy.format(new BigDecimal(takenMoney))
                : formatter.format(takenMoney);
        String formattedCrystals = formatter.format(takenCrystals);
        String formattedShards   = formatter.format(takenShards);
        String formattedTokens   = formatter.format(takenTokens);

        String name    = config.getString("head-item.name", "&dHead of %player%").replace("%player%", player.getName());
        String texture = config.getString("head-item.texture", "");

        String matKey  = config.getString("head-item.material", "SKULL_ITEM");
        Material material = Material.matchMaterial(matKey);
        if (material == null) material = Material.matchMaterial("PLAYER_HEAD");
        if (material == null) material = Material.matchMaterial("SKULL_ITEM");
        int data = config.getInt("head-item.data", 3);

        List<String> lore = config.getStringList("head-item.lore").stream()
                .map(s -> s.replace("%money%",   formattedMoney))
                .map(s -> s.replace("%tokens%",  formattedTokens))
                .map(s -> s.replace("%upgradetools_tokens%", formattedTokens))
                .map(s -> s.replace("%mantichoes_tokens%",   formattedTokens))
                .map(s -> s.replace("%shards%",  formattedShards))
                .map(s -> s.replace("%crystals%",formattedCrystals))
                .collect(Collectors.toList());

        ItemStack itemStack = new ItemCreator(material, name, 1, data, texture, lore).getItem();

        NBTItem nbtItem = new NBTItem(itemStack);
        NBTCompound compound = nbtItem.getOrCreateCompound("CandyHeads");
        compound.setLong("money", takenMoney);
        compound.setDouble("tokens", takenTokens);
        return nbtItem.getItem();
    }

    public void reloadConfig() {
        this.config = plugin.getHeadsCFG().getConfig();
        if (moneyManager != null) {
            moneyManager.reloadConfig();
        }
        // Re-evaluate token manager on reload (Vulcan may be available now)
        ensureTokenManager();
        if (tokenManager != null) {
            try {
                tokenManager.reloadConfig();
            } catch (Throwable ignored) {}
        }
    }
}
