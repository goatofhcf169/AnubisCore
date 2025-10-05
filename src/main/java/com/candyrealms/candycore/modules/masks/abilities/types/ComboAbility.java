package com.candyrealms.candycore.modules.masks.abilities.types;

import com.candyrealms.candycore.CandyCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ComboAbility {

    private final CandyCore plugin;

    private final Map<UUID, Double> multipliers = new HashMap<>();

    private final double increase;

    private final double maxIncrease;

    public ComboAbility(CandyCore plugin) {
        this.plugin = plugin;

        increase = maskConfig.getDouble("combo-mask.increase-per-hit");
        maxIncrease = maskConfig.getDouble("combo-mask.max-increase");
    }

    /**
     * Puts a player into the multipliers map and adds their new multiplier.
     * If already inside map, it uses getNewCombo(value) method to compute a new value.
     * @param player
     */
    public void addCombo(Player player) {
        UUID playerUUID = player.getUniqueId();

        multipliers.compute(playerUUID, (k,v) -> (v != null ? getNewCombo(v) : increase));
    }

    /**
     * Resets a players combo multiplier to zero.
     * @param player
     */
    public void resetCombo(Player player) {
        UUID playerUUID = player.getUniqueId();
        if(!multipliers.containsKey(playerUUID)) return;

        multipliers.put(playerUUID, 0.0);
    }

    /**
     * Gets the players value or returns 0 if player isnt inside the map.
     * @param player
     * @return
     */
    public double getMultiplier(Player player) {
        UUID playerUUID = player.getUniqueId();

        return multipliers.getOrDefault(playerUUID, 0.0);
    }

    /**
     * Uses the value from the compute method if value doesnt equal null.
     * Adds the current value of a player's multiplier to the increase value.
     * If value is higher than the limit, then it sets the value to the limit.
     * @param value
     * @return
     */
    private double getNewCombo(double value) {
        return Math.min(maxIncrease, value + increase);
    }
}
