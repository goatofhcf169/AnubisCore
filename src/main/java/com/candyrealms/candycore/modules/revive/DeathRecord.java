package com.candyrealms.candycore.modules.revive;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

@Getter
public class DeathRecord {

    private final long timestamp;
    private final String cause;
    private final String killer;
    private final Location location;
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final int level;

    public DeathRecord(long timestamp,
                       String cause,
                       String killer,
                       Location location,
                       ItemStack[] contents,
                       ItemStack[] armor,
                       int level) {
        this.timestamp = timestamp;
        this.cause = cause;
        this.killer = killer;
        this.location = location;
        this.contents = contents;
        this.armor = armor;
        this.level = level;
    }
}

