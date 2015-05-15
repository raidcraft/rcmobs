package de.raidcraft.mobs.api;

import org.bukkit.configuration.ConfigurationSection;

/**
 * @author mdoering
 */
public interface NmsEntityManager {

    void loadEntity(CustomNmsEntity entity, ConfigurationSection config);
}
