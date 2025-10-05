package com.candyrealms.candycore.configuration;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.utils.ColorUtil;
import com.candyrealms.candycore.utils.ItemCreator;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ConfigManager {

    private final CandyCore plugin;

    private FileConfiguration config;

    @Getter
    private List<String> balanceMsg;

    @Getter
    private String prefix;

    @Getter
    private String muteMessage;

    @Getter
    private String unmuteMessage;

    @Getter
    private String clearMessage;

    @Getter
    private String reloadMessage;

    @Getter
    private String safeDZWorld;

    @Getter
    private String headBypassPerm;

    @Getter
    private String masksAttachedLore;

    @Getter
    private String hydraLore;

    @Getter
    private String slayerLore;

    @Getter
    private String comboLore;

    @Getter
    private String founderLore;

    @Getter
    private boolean nerfApples;

    @Getter
    private boolean alwaysSunny;

    @Getter
    private boolean balanceEnabled;

    @Getter
    private int resistanceTime;

    @Getter
    private double comboIncrease;

    @Getter
    private double comboMaxIncrease;

    @Getter
    private int safeDZReduction;

    @Getter
    private ItemStack helpItem;

    public ConfigManager(CandyCore plugin) {
        this.plugin = plugin;

        config = plugin.getConfig();
        cacheValues();
    }

    public void reloadConfig() {
        plugin.reloadConfig();

        config = plugin.getConfig();

        cacheValues();
    }

    public void cacheValues() {
        FileConfiguration masksConfig = plugin.getMasksCFG().getConfig();

        prefix = config.getString("prefix");
        muteMessage = config.getString("messages.chat-mute");
        unmuteMessage = config.getString("messages.chat-unmute");
        clearMessage = config.getString("messages.chat-cleared");
        reloadMessage = config.getString("messages.reload");
        safeDZWorld = config.getString("safe-dz-world");
        headBypassPerm = config.getString("heads-bypass-permission");
        masksAttachedLore = masksConfig.getString("attached-lore");
        hydraLore = masksConfig.getString("hydra-mask.attached-lore");
        slayerLore = masksConfig.getString("slayer-mask.attached-lore");
        comboLore = masksConfig.getString("combo-mask.attached-lore");
        founderLore = masksConfig.getString("founder-mask.attached-lore");

        nerfApples = config.getBoolean("nerf-god-apples.enabled");
        balanceEnabled = config.getBoolean("better-balance.enabled");
        alwaysSunny = config.getBoolean("always-sunny");

        resistanceTime = config.getInt("nerf-god-apples.resistance-time");
        safeDZReduction = config.getInt("safe-dz-amount");

        comboIncrease = masksConfig.getDouble("combo-mask.increase-per-hit");
        comboMaxIncrease = masksConfig.getDouble("combo-mask.max-increase");

        balanceMsg = config.getStringList("better-balance.message");

        // ItemStack Caching
        String helpItemName = config.getString("help-star.name");
        Material helpMaterial = Material.valueOf(config.getString("help-star.material"));
        int helpData = config.getInt("help-star.data");
        List<String> helpLore = config.getStringList("help-star.lore");

        helpItem = new ItemCreator(helpMaterial, helpItemName, 1, helpData, "", helpLore).getItem();
    }

    public void sendReloadMessage(Player player) {
        player.sendMessage(ColorUtil.color(getReloadMessage()
                .replace("%prefix%", getPrefix())));

        player.playSound(player.getLocation(), Sound.LEVEL_UP, 7, 6);
    }

    public void announceChatMute() {
        Bukkit.broadcastMessage(ColorUtil.color(getMuteMessage()
                .replace("%prefix%", getPrefix())));
    }

    public void announceChatUnmute() {
        Bukkit.broadcastMessage(ColorUtil.color(getUnmuteMessage()
                .replace("%prefix%", getPrefix())));
    }

    public void announceChatCleared() {
        Bukkit.broadcastMessage(ColorUtil.color(getClearMessage()
                .replace("%prefix%", getPrefix())));
    }
}
