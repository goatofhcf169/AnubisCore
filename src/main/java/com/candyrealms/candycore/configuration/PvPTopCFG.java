package com.candyrealms.candycore.configuration;

import com.candyrealms.candycore.AnubisCore;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class PvPTopCFG {

    private final AnubisCore plugin;

    @Getter
    private FileConfiguration config;

    @Getter
    private File file;

    public PvPTopCFG(AnubisCore plugin) {
        this.plugin = plugin;

        initializeConfig();
    }

    private void initializeConfig() {
        file = new File(plugin.getDataFolder(), "pvptop.yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource("pvptop.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reloadConfig() {
        // Preserve runtime data section across reloads without flattening keys
        FileConfiguration old = this.config;
        FileConfiguration fresh = YamlConfiguration.loadConfiguration(file);

        if (old != null && old.getConfigurationSection("data") != null) {
            org.bukkit.configuration.ConfigurationSection oldData = old.getConfigurationSection("data");
            org.bukkit.configuration.ConfigurationSection freshData = fresh.getConfigurationSection("data");
            if (freshData == null) freshData = fresh.createSection("data");
            deepCopySection(oldData, freshData);
        }

        this.config = fresh;
        // Do not save here; reload should not rewrite disk
    }

    private void deepCopySection(org.bukkit.configuration.ConfigurationSection from, org.bukkit.configuration.ConfigurationSection to) {
        for (String key : from.getKeys(false)) {
            Object val = from.get(key);
            org.bukkit.configuration.ConfigurationSection child = from.getConfigurationSection(key);
            if (child != null) {
                org.bukkit.configuration.ConfigurationSection dest = to.getConfigurationSection(key);
                if (dest == null) dest = to.createSection(key);
                deepCopySection(child, dest);
            } else {
                to.set(key, val);
            }
        }
    }
}
