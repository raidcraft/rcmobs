package de.raidcraft.mobs.api;

import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.character.SkilledCharacter;
import de.raidcraft.skills.api.exceptions.CombatException;

import java.util.List;

/**
 * @author Silthus
 */
public interface Mob extends SkilledCharacter<Mob> {

    public CharacterTemplate getHighestThreat() throws CombatException;

    public List<CharacterTemplate> getInvolvedTargets();

    public CharacterTemplate getRandomTarget();

    public CharacterTemplate getNearestTarget();

    public CharacterTemplate getFarthestTarget();
}
