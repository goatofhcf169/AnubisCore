package com.candyrealms.candycore.configuration;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.utils.ColorUtil;
import com.candyrealms.candycore.utils.CompatUtil;
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

    private final AnubisCore plugin;

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

    // Combat Actionbar settings
    @Getter private boolean combatBarEnabled;
    @Getter private boolean combatBarSendVictim;
    @Getter private boolean combatBarSendAttacker;
    @Getter private int combatBarSegments;
    @Getter private int combatBarUpdateTicks;
    @Getter private boolean combatBarUseCTPDuration;
    @Getter private int combatBarDefaultDuration;
    @Getter private String combatBarFormat;
    @Getter private String combatBarActiveColor;
    @Getter private String combatBarInactiveColor;
    @Getter private String combatBarActiveChar;
    @Getter private String combatBarInactiveChar;

    // Combat Actionbar visibility
    @Getter private boolean combatBarOpsOnly;
    @Getter private String combatBarViewPermission;

    public ConfigManager(AnubisCore plugin) {
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

        boolean usePrefix = config.getBoolean("use-prefix", true);
        String cfgPrefix = config.getString("prefix", "");
        prefix = usePrefix ? cfgPrefix : "";
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

        // Combat actionbar
        combatBarEnabled = config.getBoolean("combat-actionbar.enabled", true);
        combatBarSendVictim = config.getBoolean("combat-actionbar.send-to-victim", true);
        combatBarSendAttacker = config.getBoolean("combat-actionbar.send-to-attacker", true);
        combatBarSegments = config.getInt("combat-actionbar.segments", 20);
        combatBarUpdateTicks = config.getInt("combat-actionbar.update-ticks", 10);
        combatBarUseCTPDuration = config.getBoolean("combat-actionbar.use-combattagplus-duration", true);
        combatBarDefaultDuration = config.getInt("combat-actionbar.default-duration", 15);
        combatBarFormat = config.getString("combat-actionbar.format", "&c&lCombat &7» <&a&l%bar%&7> (&f%seconds%s&7)");
        combatBarActiveColor = config.getString("combat-actionbar.active-color", "&a&l");
        combatBarInactiveColor = config.getString("combat-actionbar.inactive-color", "&c&l");
        combatBarActiveChar = config.getString("combat-actionbar.active-char", ":");
        combatBarInactiveChar = config.getString("combat-actionbar.inactive-char", ":");

        // Visibility
        combatBarOpsOnly = config.getBoolean("combat-actionbar.visibility.ops-only", false);
        combatBarViewPermission = config.getString("combat-actionbar.visibility.permission", "");
    }

    public void sendReloadMessage(Player player) {
        player.sendMessage(ColorUtil.color(getReloadMessage()
                .replace("%prefix%", getPrefix())));

        CompatUtil.play(player, 7f, 6f, "LEVEL_UP", "ENTITY_PLAYER_LEVELUP");
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
