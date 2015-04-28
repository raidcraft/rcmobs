package de.raidcraft.mobs.api;

import de.raidcraft.skills.api.ability.AbstractAbility;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.persistance.AbilityProperties;

import java.util.Optional;

/**
 * @author Silthus
 */
public abstract class MobAbility extends AbstractAbility<Mob> {

    public MobAbility(Mob holder, AbilityProperties data) {

        super(holder, data);
    }

    @Override
    public CharacterTemplate getTarget() throws CombatException {

        Optional<CharacterTemplate> threat = getHolder().getHighestThreat();
        return threat.isPresent() ? threat.get() : null;
    }
}
