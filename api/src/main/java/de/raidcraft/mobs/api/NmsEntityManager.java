package de.raidcraft.mobs.api;

import org.bukkit.configuration.ConfigurationSection;

/**
 * @author mdoering
 */
public interface NmsEntityManager {

    public void loadEntity(CustomNmsEntity entity, ConfigurationSection config);
}
