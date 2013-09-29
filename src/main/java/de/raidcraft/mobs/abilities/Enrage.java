package de.raidcraft.mobs.abilities;

import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.MobAbility;
import de.raidcraft.skills.api.persistance.AbilityProperties;
import de.raidcraft.skills.api.trigger.TriggerHandler;
import de.raidcraft.skills.api.trigger.Triggered;
import de.raidcraft.skills.trigger.DamageTrigger;
import de.raidcraft.skills.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Silthus
 */
public class Enrage extends MobAbility implements Triggered {

    private double healthTreshhold;
    private ConfigurationSection damageIncrease;
    private ConfigurationSection attackIncrease;

    public Enrage(Mob holder, AbilityProperties data) {

        super(holder, data);
    }

    @Override
    public void load(ConfigurationSection data) {

        healthTreshhold = data.getDouble("treshhold", 0.5);
        damageIncrease = data.getConfigurationSection("damage-increase");
        attackIncrease = data.getConfigurationSection("attack-increase");
    }

    public double getDamageIncrease() {

        return ConfigUtil.getTotalValue(this, damageIncrease);
    }

    public double getAttackIncrease() {

        return ConfigUtil.getTotalValue(this, attackIncrease);
    }

    @TriggerHandler(ignoreCancelled = true)
    public void onDamage(DamageTrigger trigger) {

        int currentHealth = getHolder().getHealth();
        if (currentHealth - trigger.getAttack().getDamage() < healthTreshhold * getHolder().getMaxHealth()) {
            // trigger the enrage effect

        }
    }
}
