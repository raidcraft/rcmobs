package de.raidcraft.mobs.api;

import de.raidcraft.skills.api.character.CharacterTemplate;
import org.bukkit.Location;

import java.util.List;

/**
 * @author Silthus
 */
public interface Spawnable {

    public List<CharacterTemplate> spawn(Location location);

    public default List<CharacterTemplate> spawn(Location location, SpawnReason reason) {

        return spawn(location);
    }

    public default List<CharacterTemplate> spawn(String source, Location location) {

        return spawn(location);
    }

    public default void remove(String source) {

        throw new UnsupportedOperationException();
    }
}
