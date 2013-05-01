package de.raidcraft.mobs.creatures;

import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.api.character.AbstractSkilledCharacter;
import de.raidcraft.skills.api.trigger.Triggered;
import de.raidcraft.util.MathUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

/**
 * @author Silthus
 */
public class ConfigurableCreature extends AbstractSkilledCharacter<Mob> implements Mob, Triggered {

    private final int minDamage;
    private final int maxDamage;
    private final int maxHealth;

    public ConfigurableCreature(LivingEntity entity, ConfigurationSection config) {

        super(entity);
        this.minDamage = config.getInt("min-damage");
        this.maxDamage = config.getInt("max-damage");
        this.maxHealth = config.getInt("health");
    }

    @Override
    public int getDamage() {

        return MathUtil.RANDOM.nextInt(maxDamage - minDamage) + minDamage;
    }

    @Override
    public int getDefaultHealth() {

        return maxHealth;
    }
}
