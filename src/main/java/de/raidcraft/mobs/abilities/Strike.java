package de.raidcraft.mobs.abilities;

import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.MobAbility;
import de.raidcraft.skills.api.ability.Useable;
import de.raidcraft.skills.api.combat.EffectType;
import de.raidcraft.skills.api.combat.action.EntityAttack;
import de.raidcraft.skills.api.combat.callback.EntityAttackCallback;
import de.raidcraft.skills.api.effect.common.SunderingArmor;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.persistance.AbilityProperties;
import de.raidcraft.skills.effects.Bleed;
import de.raidcraft.skills.effects.Burn;
import de.raidcraft.skills.effects.Slow;
import de.raidcraft.skills.effects.Weakness;
import de.raidcraft.skills.effects.disabling.Disarm;
import de.raidcraft.skills.effects.disabling.Interrupt;
import de.raidcraft.skills.effects.disabling.KnockBack;
import de.raidcraft.skills.effects.disabling.Silence;
import de.raidcraft.skills.effects.disabling.Stun;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Silthus
 */
public class Strike extends MobAbility implements Useable {

    private boolean knockBack = false;
    private boolean bleed = false;
    private boolean stun = false;
    private boolean sunderArmor = false;
    private boolean disarm = false;
    private boolean ignoreArmor = false;
    private boolean slow = false;
    private boolean weaken = false;
    private boolean burn = false;
    private boolean interrupt = false;
    private boolean silence = false;

    public Strike(Mob holder, AbilityProperties data) {

        super(holder, data);
    }

    @Override
    public void load(ConfigurationSection data) {

        knockBack = data.getBoolean("knockback", false);
        bleed = data.getBoolean("bleed", false);
        stun = data.getBoolean("stun", false);
        sunderArmor = data.getBoolean("sunder-armor", false);
        disarm = data.getBoolean("disarm", false);
        ignoreArmor = data.getBoolean("ignore-armor", false);
        slow = data.getBoolean("slow", false);
        weaken = data.getBoolean("weaken", false);
        burn = data.getBoolean("burn", false);
        interrupt = data.getBoolean("interrupt", false);
        silence = data.getBoolean("silence", false);
    }

    @Override
    public void use() throws CombatException {

        attack(getTarget(), new EntityAttackCallback() {
            @Override
            public void run(EntityAttack attack) throws CombatException {

                if (knockBack) addEffect(getHolder().getEntity().getLocation(), attack.getTarget(), KnockBack.class);
                if (bleed) addEffect(attack.getTarget(), Bleed.class);
                if (stun) addEffect(attack.getTarget(), Stun.class);
                if (sunderArmor) addEffect(attack.getTarget(), SunderingArmor.class);
                if (disarm) addEffect(attack.getTarget(), Disarm.class);
                if (ignoreArmor)attack.addAttackTypes(EffectType.IGNORE_ARMOR);
                if (slow) addEffect(attack.getTarget(), Slow.class);
                if (weaken)addEffect(attack.getTarget(), Weakness.class);
                if (burn) addEffect(attack.getTarget(), Burn.class);
                if (silence || interrupt) addEffect(attack.getTarget(), Interrupt.class);
                if (silence) addEffect(attack.getTarget(), Silence.class);
            }
        });
    }
}
