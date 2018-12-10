package de.raidcraft.mobs.api;

import de.raidcraft.skills.api.character.CharacterTemplate;
import org.bukkit.Location;

import java.util.List;

/**
 * @author Silthus
 */
public interface Spawnable {

    List<CharacterTemplate> spawn(Location location) throws SpawnMobException;

    default List<CharacterTemplate> spawn(Location location, SpawnReason reason) throws SpawnMobException {

        return spawn(location);
    }

    default List<CharacterTemplate> spawn(String source, Location location) throws SpawnMobException {

        return spawn(location);
    }

    default void remove(String source) {

        throw new UnsupportedOperationException();
    }
}
