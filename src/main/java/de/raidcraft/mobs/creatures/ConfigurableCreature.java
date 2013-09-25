package de.raidcraft.mobs.creatures;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.items.CustomItemException;
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
import org.bukkit.inventory.EntityEquipment;

/**
 * @author Silthus
 */
public class ConfigurableCreature extends AbstractMob {

    private final MobType type;
    private final int minDamage;
    private final int maxDamage;

    public ConfigurableCreature(LivingEntity entity, ConfigurationSection config) {

        super(entity);
        this.type = MobType.fromString(config.getString("strength"));
        this.minDamage = config.getInt("min-damage");
        this.maxDamage = config.getInt("max-damage", minDamage);

        int minHealth = config.getInt("min-health", 20);
        int maxHealth = config.getInt("max-health", minHealth);
        int health = config.getInt("health", 0);
        setMaxHealth(health > 0 ? health : MathUtil.RANDOM.nextInt(maxHealth) + minHealth);
        setHealth(getMaxHealth());
        getAttachedLevel().setLevel(config.getInt("level", 1));
        getEntity().setCustomNameVisible(true);
        setName(getType().getNameColor() + config.getString("name"));
        loadAbilities(config.getConfigurationSection("abilities"));
        equipItems(config.getConfigurationSection("equipment"));
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

    private void equipItems(ConfigurationSection config) {

        try {
            if (config == null) return;
            EntityEquipment equipment = getEntity().getEquipment();
            equipment.setItemInHand(RaidCraft.getItem(config.getString("hand")));
            equipment.setHelmet(RaidCraft.getItem(config.getString("head")));
            equipment.setChestplate(RaidCraft.getItem(config.getString("chest")));
            equipment.setLeggings(RaidCraft.getItem(config.getString("legs")));
            equipment.setBoots(RaidCraft.getItem(config.getString("boots")));

            equipment.setItemInHandDropChance(config.getInt("hand-drop-chance", 0));
            equipment.setHelmetDropChance(config.getInt("head-drop-chance", 0));
            equipment.setChestplateDropChance(config.getInt("chest-drop-chance", 0));
            equipment.setLeggingsDropChance(config.getInt("legs-drop-chance", 0));
            equipment.setBootsDropChance(config.getInt("boots-drop-chance", 0));
        } catch (CustomItemException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
        }
    }

    @Override
    public void setHealth(int health) {

        super.setHealth(health);
        updateHealthBar();
    }

    @Override
    public void setMaxHealth(int maxHealth) {

        super.setMaxHealth(maxHealth);
        updateHealthBar();
    }

    @Override
    public void setInCombat(boolean inCombat) {

        super.setInCombat(inCombat);
        updateHealthBar();
    }

    private void updateHealthBar() {

        if (isInCombat()) {
            getEntity().setCustomName(EntityUtil.drawHealthBar(getHealth(), getMaxHealth(), getType().getNameColor()));
        } else {
            getEntity().setCustomName(getType().getNameColor() + getName());
        }
    }

    @Override
    public MobType getType() {

        return type == null ? MobType.COMMON : type;
    }

    @Override
    public double getDamage() {

        return MathUtil.RANDOM.nextInt(maxDamage - minDamage) + minDamage;
    }
}
