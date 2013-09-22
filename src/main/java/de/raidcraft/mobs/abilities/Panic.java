package de.raidcraft.mobs.abilities;

import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.MobAbility;
import de.raidcraft.skills.api.ability.AbilityInformation;
import de.raidcraft.skills.api.persistance.AbilityProperties;
import de.raidcraft.skills.api.trigger.TriggerHandler;
import de.raidcraft.skills.api.trigger.TriggerPriority;
import de.raidcraft.skills.api.trigger.Triggered;
import de.raidcraft.skills.trigger.DamageTrigger;
import de.raidcraft.util.EntityUtil;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Silthus
 */
@AbilityInformation(
        name = "Panic",
        description = "Sets the mob into a panic mode, running aways like a chicken."
)
public class Panic extends MobAbility implements Triggered {

    private double panicThreshhold;
    private boolean paniced = false;

    public Panic(Mob holder, AbilityProperties data) {

        super(holder, data);
    }

    @Override
    public void load(ConfigurationSection data) {

        panicThreshhold = data.getDouble("panic-treshhold");
    }

    @TriggerHandler(ignoreCancelled = true, priority = TriggerPriority.MONITOR)
    public void onDamage(DamageTrigger trigger) {

        if (paniced) {
            return;
        }
        double newHealth = trigger.getAttack().getTarget().getHealth() - trigger.getAttack().getDamage();
        if (newHealth / getHolder().getMaxHealth() < panicThreshhold) {
            EntityUtil.addPanicMode(getHolder().getEntity());
            paniced = true;
        }
    }
}
