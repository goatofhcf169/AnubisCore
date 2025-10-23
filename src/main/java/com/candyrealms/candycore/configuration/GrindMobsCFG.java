package com.candyrealms.candycore.configuration;

import com.candyrealms.candycore.AnubisCore;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class GrindMobsCFG {

    private final AnubisCore plugin;

    @Getter
    private FileConfiguration config;

    @Getter
    private File file;

    public GrindMobsCFG(AnubisCore plugin) {
        this.plugin = plugin;
        initializeConfig();
    }

    private void initializeConfig() {
        file = new File(plugin.getDataFolder(), "grindmobs.yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource("grindmobs.yml", false);
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
        config = YamlConfiguration.loadConfiguration(file);
        saveConfig();
    }
}

