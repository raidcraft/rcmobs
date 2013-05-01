package de.raidcraft.mobs.api;

import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.exceptions.CombatException;

/**
 * @author Silthus
 */
public interface Ability {

    public void run(CharacterTemplate target) throws CombatException;
}
