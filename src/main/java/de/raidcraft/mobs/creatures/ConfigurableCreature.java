package de.raidcraft.mobs.creatures;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.AbstractMob;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.MobType;
import de.raidcraft.mobs.effects.AbilityUser;
import de.raidcraft.skills.AbilityManager;
import de.raidcraft.skills.api.ability.Ability;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.exceptions.UnknownSkillException;
import de.raidcraft.util.EntityUtil;
import de.raidcraft.util.MathUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

/**
 * @author Silthus
 */
public class ConfigurableCreature extends AbstractMob {

    private final MobType type;
    private final int minDamage;
    private final int maxDamage;

    public ConfigurableCreature(LivingEntity entity, ConfigurationSection config) {

        super(entity);
        this.type = MobType.fromString(config.getString("type"));
        this.minDamage = config.getInt("min-damage");
        this.maxDamage = config.getInt("max-damage", minDamage);

        setMaxHealth(config.getInt("health"));
        getAttachedLevel().setLevel(config.getInt("level"));
        getEntity().setCustomNameVisible(true);
        setName(getType().getNameColor() + config.getString("name"));
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
    public void setInCombat(boolean inCombat) {

        super.setInCombat(inCombat);
        if (inCombat) {
            getEntity().setCustomName(EntityUtil.drawHealthBar(getHealth(), getMaxHealth()));
        } else {
            getEntity().setCustomName(getType().getNameColor() + getName());
        }
    }

    @Override
    public MobType getType() {

        return type;
    }

    @Override
    public int getDamage() {

        return MathUtil.RANDOM.nextInt(maxDamage - minDamage) + minDamage;
    }
}
