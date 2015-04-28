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

    public String getId();

    public void setId(String id);

    public Location getSpawnLocation();

    public boolean isRare();

    public boolean isElite();

    public boolean isSpawningNaturally();

    public boolean isWaterMob();

    public Optional<RDSTable> getLootTable();

    public Optional<CharacterTemplate> getHighestThreat();

    public List<CharacterTemplate> getInvolvedTargets();

    public CharacterTemplate getRandomTarget();

    public CharacterTemplate getNearestTarget();

    public CharacterTemplate getFarthestTarget();
}
