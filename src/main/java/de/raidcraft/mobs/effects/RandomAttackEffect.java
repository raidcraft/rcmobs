package de.raidcraft.mobs.effects;

import de.raidcraft.mobs.api.Ability;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.effect.types.PeriodicEffect;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.persistance.EffectData;

/**
 * @author Silthus
 */
public class RandomAttackEffect extends PeriodicEffect<Ability> {

    public RandomAttackEffect(Ability source, CharacterTemplate target, EffectData data) {

        super(source, target, data);
    }

    @Override
    protected void tick(CharacterTemplate target) throws CombatException {
        //TODO: implement
    }

    @Override
    protected void apply(CharacterTemplate target) throws CombatException {
        //TODO: implement
    }

    @Override
    protected void remove(CharacterTemplate target) throws CombatException {
        //TODO: implement
    }

    @Override
    protected void renew(CharacterTemplate target) throws CombatException {
        //TODO: implement
    }
}
