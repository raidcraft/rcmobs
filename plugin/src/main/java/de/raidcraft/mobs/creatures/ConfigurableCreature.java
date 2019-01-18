package de.raidcraft.mobs.creatures;

import com.comphenix.packetwrapper.WrapperPlayServerNamedSoundEffect;
import com.comphenix.protocol.ProtocolLibrary;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.random.RDSRandom;
import de.raidcraft.api.random.RDSTable;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.api.AbstractMob;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.MobConfig;
import de.raidcraft.mobs.effects.AbilityUser;
import de.raidcraft.skills.AbilityManager;
import de.raidcraft.skills.api.ability.Ability;
import de.raidcraft.skills.api.effect.common.Combat;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.exceptions.UnknownSkillException;
import de.raidcraft.util.EntityMetaData;
import de.raidcraft.util.EntityUtil;
import de.raidcraft.util.EnumUtils;
import de.raidcraft.util.MathUtil;
import lombok.Data;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.metadata.FixedMetadataValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Silthus
 */
@Data
public class ConfigurableCreature extends AbstractMob {

    private final MobConfig config;
    private final Location spawnLocation;
    private final boolean hidingName;
    private double damage = 0;

    public ConfigurableCreature(LivingEntity entity, MobConfig config) {

        super(entity);
        this.config = config;
        this.spawnLocation = entity.getLocation();
        this.hidingName = config.isHidingName();

        int diffLevel = config.getMaxLevel() - config.getMinlevel();
        if (diffLevel <= 0) {
            getAttachedLevel().setLevel(config.getMinlevel());
        } else {
            getAttachedLevel().setLevel(MathUtil.RANDOM.nextInt(diffLevel) + config.getMinlevel());
        }
        if (getAttachedLevel().getLevel() <= 0) {
            RaidCraft.getComponent(MobsPlugin.class).getLogger().info(getId() + ":" + getName()
                    + " has level: " + getAttachedLevel().getLevel());
        }

        if (config.isBaby()) {
            if (getEntity() instanceof Ageable) {
                ((Ageable) getEntity()).setBaby();
                ((Ageable) getEntity()).setAgeLock(true);
            } else if (getEntity() instanceof Zombie) {
                ((Zombie) getEntity()).setBaby(true);
            }
        }

        if (config.isAggro()) {
            if (getEntity() instanceof PigZombie) {
                ((PigZombie) getEntity()).setAngry(true);
            } else if (getEntity() instanceof Wolf) {
                ((Wolf) getEntity()).setAngry(true);
            }
        }

        config.getDisguise().ifPresent(disguise -> {
            disguise.setEntity(getEntity());
            disguise.startDisguise();
        });

        final BigDecimal level = new BigDecimal(getAttachedLevel().getLevel());

        if (config.getMinHealth() < 0 && config.getMaxHealth() < 0) {
            config.getHealthExpression().ifPresent(expression -> {
                expression = expression.with("level", level).setPrecision(0);
                setMaxHealth(expression.eval().doubleValue());
                setHealth(getMaxHealth());
            });
        } else {
            setMaxHealth(MathUtil.RANDOM.nextInt(config.getMaxHealth()) + config.getMinHealth());
            setHealth(getMaxHealth());
        }

        if (config.getMinDamage() < 0 && config.getMaxDamage() < 0) {
            config.geDamageExpression().ifPresent(expression -> {
                expression = expression.with("level", level).setPrecision(2);
                setDamage(expression.eval().doubleValue());
            });
        } else {
            setDamage(RDSRandom.getDoubleValue(config.getMinDamage(), config.getMaxDamage()));
        }

        setName(config.getName());
        getEntity().setCanPickupItems(config.isItemPickup());
        loadAbilities(config.getAbilities());
        equipItems(config.getEquipment());
        if (config.isRanged()) {
            // TODO: investigate and fix
            // EntityUtil.setRangedMode(getEntity());
        }
        // set custom meta data to identify our mob
        MobsPlugin plugin = RaidCraft.getComponent(MobsPlugin.class);
        getEntity().setMetadata(EntityMetaData.RCMOBS_MOB_ID, new FixedMetadataValue(plugin, getId()));
        getEntity().setMetadata(EntityMetaData.RCMOBS_CUSTOM_MOB, new FixedMetadataValue(plugin, true));
        if (config.isGiveExp()) getEntity().setMetadata(EntityMetaData.RCMOBS_AWARD_EXP, new FixedMetadataValue(plugin, true));
        if (config.isElite()) getEntity().setMetadata(EntityMetaData.RCMOBS_ELITE, new FixedMetadataValue(plugin, true));
        if (config.isRare()) getEntity().setMetadata(EntityMetaData.RCMOBS_RARE, new FixedMetadataValue(plugin, true));
    }

    private void loadAbilities(ConfigurationSection config) {

        if (config == null) return;
        for (String key : config.getKeys(false)) {
            try {
                Ability<Mob> ability = RaidCraft.getComponent(AbilityManager.class).getAbility(this, key, config.getConfigurationSection(key));
                addAbility(ability);
            } catch (UnknownSkillException e) {
                RaidCraft.LOGGER.warning(e.getMessage());
            }
        }
        if (!getAbilties().isEmpty()) {
            try {
                addEffect(this, AbilityUser.class);
            } catch (CombatException e) {
                RaidCraft.LOGGER.warning(e.getMessage());
            }
        }
    }

    private void equipItems(ConfigurationSection config) {

        if (config == null) return;
        EntityEquipment equipment = getEntity().getEquipment();
        RaidCraft.getItem(config.getString("hand")).ifPresent(equipment::setItemInMainHand);
        RaidCraft.getItem(config.getString("head")).ifPresent(equipment::setHelmet);
        RaidCraft.getItem(config.getString("chest")).ifPresent(equipment::setChestplate);
        RaidCraft.getItem(config.getString("legs")).ifPresent(equipment::setLeggings);
        RaidCraft.getItem(config.getString("boots")).ifPresent(equipment::setBoots);

        equipment.setItemInHandDropChance(config.getInt("hand-drop-chance", 0));
        equipment.setHelmetDropChance(config.getInt("head-drop-chance", 0));
        equipment.setChestplateDropChance(config.getInt("chest-drop-chance", 0));
        equipment.setLeggingsDropChance(config.getInt("legs-drop-chance", 0));
        equipment.setBootsDropChance(config.getInt("boots-drop-chance", 0));
    }

    @Override
    public Location getSpawnLocation() {

        return spawnLocation;
    }

    @Override
    public void setHealth(double health) {

        // lets play the hurt sound if the new health is below our current
        if (health < getHealth()) {
            playSound(getConfig().getHurtSound(), getConfig().getHurtSoundPitch(), 1.0F);
        }
        super.setHealth(health);
        updateNameDisplay();
    }

    @Override
    public void setMaxHealth(double maxHealth) {

        super.setMaxHealth(maxHealth);
        updateNameDisplay();
    }

    @Override
    public void setInCombat(boolean inCombat) {

        super.setInCombat(inCombat);
        updateNameDisplay();
        // reset the health to max
        if (!inCombat && getConfig().isResetHealth()) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    public boolean isRare() {
        return getConfig().isRare();
    }

    @Override
    public boolean isElite() {
        return getConfig().isElite();
    }

    @Override
    public boolean isSpawningNaturally() {
        return getConfig().isSpawningNaturally();
    }

    @Override
    public boolean isWaterMob() {
        return getConfig().isWaterMob();
    }

    @Override
    public boolean isPassive() {
        return getConfig().isPassive();
    }

    @Override
    public void updateNameDisplay() {

        if (isInCombat()) {
            getEntity().setCustomName(EntityUtil.drawHealthBar(getHealth(), getMaxHealth(), ChatColor.WHITE));
        } else {
            getEntity().setCustomName(EntityUtil.drawMobName(
                    getName(),
                    getAttachedLevel().getLevel(),
                    getAttachedLevel().getLevel() > 0 ? ChatColor.YELLOW : ChatColor.GRAY,
                    isElite(),
                    isRare()));
        }

        if (getConfig().isHidingName()) {
            getEntity().setCustomNameVisible(false);
        }
    }

    @Override
    public void reset() {

        try {
            getEntity().teleport(spawnLocation);
            removeEffect(Combat.class);
        } catch (CombatException ignored) {
        }
    }

    @Override
    public boolean kill() {

        playSound(getConfig().getDeathSound(), getConfig().getDeathSoundPitch(), 1.0F);
        boolean result = super.kill();
        getEntity().getPassengers().forEach(Entity::remove);
        return result;
    }

    public List<RDSTable> getLootTables() {
        return new ArrayList<>(this.getConfig().getLootTables()).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void playSound(String name, float pitch, float volume) {

        Sound sound = EnumUtils.getEnumFromString(Sound.class, name);
        if (sound == null) {
            RaidCraft.LOGGER.warning("Tried to play invalid sound: " + name);
            return;
        }
        Location location = getEntity().getLocation();
        WrapperPlayServerNamedSoundEffect soundEffect = new WrapperPlayServerNamedSoundEffect();
        soundEffect.setSoundEffect(Sound.valueOf(name));
        soundEffect.setPitch((byte) pitch);
        soundEffect.setVolume(volume);
        soundEffect.setEffectPositionX(location.getBlockX());
        soundEffect.setEffectPositionY(location.getBlockY());
        soundEffect.setEffectPositionZ(location.getBlockZ());
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(soundEffect.getHandle());
    }
}
