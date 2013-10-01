package de.raidcraft.mobs.effects;

import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.api.ability.Ability;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.combat.action.AbilityAction;
import de.raidcraft.skills.api.effect.EffectInformation;
import de.raidcraft.skills.api.effect.types.PeriodicEffect;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.persistance.EffectData;
import de.raidcraft.util.MathUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Silthus
 */
@EffectInformation(
        name = "Abilities",
        description = "Describes how to use abilities."
)
public class AbilityUser extends PeriodicEffect<Mob> {

    private final Set<Ability<Mob>> usedAbilities = new HashSet<>();
    private boolean random = true;
    private boolean resetAfterAllUsed = true;
    private boolean trackCastedAbilities = false;

    public AbilityUser(Mob source, CharacterTemplate target, EffectData data) {

        super(source, target, data);
    }

    @Override
    public void load(ConfigurationSection data) {

        random = data.getBoolean("random", true);
        resetAfterAllUsed = data.getBoolean("reset-all", true);
        trackCastedAbilities = data.getBoolean("track-casted", false);
    }

    @Override
    protected void tick(CharacterTemplate target) throws CombatException {

        List<Ability<Mob>> abilities = getSource().getUseableAbilities();
        if (trackCastedAbilities) {
            abilities.removeAll(usedAbilities);
            if (abilities.isEmpty()) {
                if (!usedAbilities.isEmpty() && resetAfterAllUsed) {
                    abilities.addAll(usedAbilities);
                    usedAbilities.clear();
                } else {
                    return;
                }
            }
        }

        Ability<Mob> abilitiy = getAbilitiy(abilities, 0);
        if (trackCastedAbilities) {
            usedAbilities.add(abilitiy);
        }
        new AbilityAction<>(abilitiy).run();
    }

    private Ability<Mob> getAbilitiy(List<Ability<Mob>> abilities, int initialIndex) {

        Ability<Mob> ability = abilities.get(initialIndex);
        if (!trackCastedAbilities || random) {
            ability = abilities.get(MathUtil.RANDOM.nextInt(abilities.size()));
        }
        if (ability.isOnCooldown() && initialIndex + 1 < abilities.size()) {
            return getAbilitiy(abilities, initialIndex + 1);
        }
        return ability;
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
}
