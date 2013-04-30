package de.raidcraft.mobs.api;

import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Silthus
 */
public abstract class AbstractAbility implements Ability {

    private final Mob mob;

    public AbstractAbility(Mob mob, ConfigurationSection config) {

        this.mob = mob;
    }
}
