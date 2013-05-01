package de.raidcraft.mobs.creatures;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.AbilityManager;
import de.raidcraft.mobs.api.Ability;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.UnknownAbilityException;
import de.raidcraft.skills.api.character.AbstractCharacterTemplate;
import de.raidcraft.skills.api.trigger.Triggered;
import de.raidcraft.util.MathUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
public class ConfigurableCreature extends AbstractCharacterTemplate implements Mob, Triggered {

    private final List<Ability> abilities = new ArrayList<>();
    private final int minDamage;
    private final int maxDamage;
    private final int maxHealth;

    public ConfigurableCreature(LivingEntity entity, ConfigurationSection config) {

        super(entity);
        this.minDamage = config.getInt("min-damage");
        this.maxDamage = config.getInt("max-damage");
        this.maxHealth = config.getInt("health");
        loadAbilities(config.getConfigurationSection("abilities"));
    }

    private void loadAbilities(ConfigurationSection config) {

        if (config == null) {
            return;
        }
        for (String key : config.getKeys(false)) {
            try {
                abilities.add(RaidCraft.getComponent(AbilityManager.class).getAbility(key, this, config.getConfigurationSection(key)));
            } catch (UnknownAbilityException e) {
                RaidCraft.LOGGER.warning(e.getMessage());
            }
        }
    }

    @Override
    public List<Ability> getAbilities() {

        return abilities;
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
