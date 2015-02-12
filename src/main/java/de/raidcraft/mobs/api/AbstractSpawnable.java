package de.raidcraft.mobs.api;

import de.raidcraft.RaidCraft;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mdoering
 */
public abstract class AbstractSpawnable implements Spawnable {

    private final Map<String, List<CharacterTemplate>> sourcedSpawns = new HashMap<>();

    @Override
    public List<CharacterTemplate> spawn(String source, Location location) {

        remove(source);
        List<CharacterTemplate> entities = spawn(location);
        sourcedSpawns.put(source, entities);
        return entities;
    }

    @Override
    public void remove(String source) {

        if (sourcedSpawns.containsKey(source)) {
            sourcedSpawns.remove(source).forEach(CharacterTemplate::remove);
        }
    }
}
