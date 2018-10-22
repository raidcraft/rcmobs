package de.raidcraft.mobs.api;

import de.raidcraft.api.random.RDSTable;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.character.SkilledCharacter;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;

/**
 * @author Silthus
 */
public interface Mob extends SkilledCharacter<Mob> {

    String getId();

    void setId(String id);

    Location getSpawnLocation();

    void updateNameDisplay();

    boolean isRare();

    boolean isElite();

    boolean isSpawningNaturally();

    boolean isWaterMob();

    boolean isPassive();

    List<RDSTable> getLootTables();

    Optional<CharacterTemplate> getHighestThreat();

    CharacterTemplate getRandomTarget();

    CharacterTemplate getNearestTarget();

    CharacterTemplate getFarthestTarget();
}
