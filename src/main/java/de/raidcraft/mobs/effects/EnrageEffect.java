package de.raidcraft.mobs.effects;

import de.raidcraft.mobs.abilities.Enrage;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.combat.EffectType;
import de.raidcraft.skills.api.effect.EffectInformation;
import de.raidcraft.skills.api.effect.types.PeriodicExpirableEffect;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.persistance.EffectData;
import de.raidcraft.skills.api.trigger.TriggerHandler;
import de.raidcraft.skills.api.trigger.TriggerPriority;
import de.raidcraft.skills.api.trigger.Triggered;
import de.raidcraft.skills.trigger.AttackTrigger;
import de.raidcraft.skills.trigger.DamageTrigger;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Silthus
 */
@EffectInformation(
        name = "Enrage",
        description = "Deals and takes more damage",
        types = {EffectType.HARMFUL, EffectType.HELPFUL}
)
public class EnrageEffect extends PeriodicExpirableEffect<Enrage> implements Triggered {

    private double damageIncrease;
    private double attackIncrease;

    public EnrageEffect(Enrage source, CharacterTemplate target, EffectData data) {

        super(source, target, data);
    }

    @Override
    public void load(ConfigurationSection data) {

        damageIncrease = getSource().getDamageIncrease();
        attackIncrease = getSource().getAttackIncrease();
    }

    @TriggerHandler(ignoreCancelled = true, priority = TriggerPriority.HIGHEST)
    public void onDamage(DamageTrigger trigger) {

        double damage = trigger.getAttack().getDamage();
        double newDamage = damage + (damage * damageIncrease);
        trigger.getAttack().setDamage(newDamage);
    }

    @TriggerHandler(ignoreCancelled = true, priority = TriggerPriority.HIGHEST)
    public void onAttack(AttackTrigger trigger) {

        double damage = trigger.getAttack().getDamage();
        double newDamage = damage + (damage * attackIncrease);
        trigger.getAttack().setDamage(newDamage);
    }

    @Override
    protected void apply(CharacterTemplate target) throws CombatException {

    }

    @Override
    protected void remove(CharacterTemplate target) throws CombatException {


    }

    @Override
    protected void renew(CharacterTemplate target) throws CombatException {


    }

    @Override
    protected void tick(CharacterTemplate target) throws CombatException {


    }
}
