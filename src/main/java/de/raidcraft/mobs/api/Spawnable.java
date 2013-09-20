package de.raidcraft.mobs.api;

import org.bukkit.Location;

/**
 * @author Silthus
 */
public interface Spawnable<T> {

    public void spawn(Location location);
}
