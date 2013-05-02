package de.raidcraft.mobs.api;

import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.character.SkilledCharacter;

import java.util.List;

/**
 * @author Silthus
 */
public interface Mob extends SkilledCharacter<Mob> {

    public CharacterTemplate getHighestThreat();

    public List<CharacterTemplate> getInvolvedTargets();

    public CharacterTemplate getRandomTarget();

    public CharacterTemplate getNearestTarget();

    public CharacterTemplate getFarthestTarget();
}
