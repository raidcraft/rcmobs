package de.raidcraft.mobs.api;

import de.raidcraft.skills.api.character.CharacterTemplate;

import java.util.List;

/**
 * @author Silthus
 */
public interface Mob extends CharacterTemplate {

    public List<Ability> getAbilities();
}
