package de.raidcraft.mobs.abilities;

import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.api.ability.AbstractAbility;
import de.raidcraft.skills.api.persistance.AbilityProperties;

/**
 * @author Silthus
 */
public abstract class MobAbility extends AbstractAbility<Mob> {

    public MobAbility(Mob holder, AbilityProperties data) {

        super(holder, data);
    }
}
