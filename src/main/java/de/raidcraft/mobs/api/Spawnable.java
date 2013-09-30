package de.raidcraft.mobs.api;

import de.raidcraft.skills.api.character.CharacterTemplate;
import org.bukkit.Location;

import java.util.List;

/**
 * @author Silthus
 */
public interface Spawnable {

    public List<CharacterTemplate> spawn(Location location);
}
