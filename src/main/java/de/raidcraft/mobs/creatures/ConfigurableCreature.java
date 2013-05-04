package de.raidcraft.mobs.creatures;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.AbstractMob;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.effects.AbilityUser;
import de.raidcraft.mobs.util.EntityUtil;
import de.raidcraft.skills.AbilityManager;
import de.raidcraft.skills.api.ability.Ability;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.exceptions.UnknownSkillException;
import de.raidcraft.util.MathUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

/**
 * @author Silthus
 */
public class ConfigurableCreature extends AbstractMob {

    private final int minDamage;
    private final int maxDamage;
    private final int panicThreshhold;

    public ConfigurableCreature(LivingEntity entity, ConfigurationSection config) {

        super(entity);
        this.minDamage = config.getInt("min-damage");
        this.maxDamage = config.getInt("max-damage");
        this.panicThreshhold = config.getInt("panic-treshhold", 0);

        setMaxHealth(config.getInt("health"));
        getEntity().setCustomNameVisible(true);
        setName(ChatColor.RED + config.getString("name"));
        loadAbilities(config.getConfigurationSection("abilities"));
    }

    private void loadAbilities(ConfigurationSection config) {

        if (config == null) return;
        for (String key : config.getKeys(false)) {
            try {
                Ability<Mob> ability = RaidCraft.getComponent(AbilityManager.class).getAbility((Mob) this, key);
                addAbility(ability);
            } catch (UnknownSkillException e) {
                RaidCraft.LOGGER.warning(e.getMessage());
            }
        }
        if (!getAbilties().isEmpty()) {
            try {
                addEffect((Mob) this, AbilityUser.class);
            } catch (CombatException e) {
                RaidCraft.LOGGER.warning(e.getMessage());
            }
        }
    }

    @Override
    public void setHealth(int health) {

        super.setHealth(health);
        if (panicThreshhold > 0 && getHealth() < panicThreshhold) {
            EntityUtil.addPanicMode(getEntity());
        }
    }

    @Override
    public int getDamage() {

        return MathUtil.RANDOM.nextInt(maxDamage - minDamage) + minDamage;
    }
}
