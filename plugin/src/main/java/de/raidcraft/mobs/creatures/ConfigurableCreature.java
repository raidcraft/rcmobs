package de.raidcraft.mobs.creatures;

import com.comphenix.packetwrapper.WrapperPlayServerNamedSoundEffect;
import com.comphenix.protocol.ProtocolLibrary;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.random.RDSTable;
import de.raidcraft.loot.LootPlugin;
import de.raidcraft.loot.LootTableManager;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.api.AbstractMob;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.effects.AbilityUser;
import de.raidcraft.mobs.util.CustomMobUtil;
import de.raidcraft.skills.AbilityManager;
import de.raidcraft.skills.api.ability.Ability;
import de.raidcraft.skills.api.effect.common.Combat;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.exceptions.UnknownSkillException;
import de.raidcraft.util.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Optional;

/**
 * @author Silthus
 */
public class ConfigurableCreature extends AbstractMob {

    private final int minDamage;
    private final int maxDamage;
    private final boolean resetHealth;
    private final boolean elite;
    private final boolean rare;
    private final boolean spawnNaturally;
    private final boolean water;
    private final boolean passive;
    private final Location spawnLocation;
    private final String hurtSound;
    private final float hurtSoundPitch;
    private final String deathSound;
    private final float deathSoundPitch;
    private RDSTable lootTable;

    public ConfigurableCreature(LivingEntity entity, ConfigurationSection config) {

        super(entity);
        this.spawnLocation = entity.getLocation();
        this.minDamage = config.getInt("min-damage");
        this.maxDamage = config.getInt("max-damage", minDamage);
        this.resetHealth = config.getBoolean("reset-health", true);
        this.elite = config.getBoolean("elite", false);
        this.rare = config.getBoolean("rare", false);
        this.water = config.getBoolean("water", false);
        this.passive = config.getBoolean("passive", false);
        this.spawnNaturally = config.getBoolean("spawn-naturally", false);
        this.hurtSound = config.getString("sound.hurt", Sound.ENTITY_GENERIC_HURT.name());
        this.hurtSoundPitch = (float) config.getDouble("sound.hurt-pitch", 1.0);
        this.deathSound = config.getString("sound.death", Sound.ENTITY_GENERIC_DEATH.name());
        this.deathSoundPitch = (float) config.getDouble("sound.death-pitch", 0.5);
        int minLevel = config.getInt("min-level", 1);
        int maxLevel = config.getInt("max-level", minLevel);
        int diffLevel = maxLevel - minLevel;
        if (diffLevel <= 0) {
            getAttachedLevel().setLevel(minLevel);
        } else {
            getAttachedLevel().setLevel(MathUtil.RANDOM.nextInt(diffLevel) + minLevel);
        }
        if (getAttachedLevel().getLevel() <= 0) {
            RaidCraft.getComponent(MobsPlugin.class).getLogger().info(getId() + ":" + getName()
                    + " has level: " + getAttachedLevel().getLevel());
        }
        int minHealth = config.getInt("min-health", (int) CustomMobUtil.getMaxHealth(getAttachedLevel().getLevel()));
        int maxHealth = config.getInt("max-health", minHealth);

        LootTableManager tableManager = RaidCraft.getComponent(LootPlugin.class).getLootTableManager();
        if (tableManager != null) {
            RDSTable lootTable = tableManager.getLevelDependantLootTable(
                    config.getString("loot-table", RaidCraft.getComponent(MobsPlugin.class).getConfiguration().defaultLoottable),
                    getAttachedLevel().getLevel());
            if (lootTable == null) {
                RaidCraft.LOGGER.warning("Loot-Table " + config.getString("loot-table") + " defined in mob " + ConfigUtil.getFileName(config) + " does not exist!");
            }
            this.lootTable = lootTable;
        }

        if (config.getBoolean("baby")) {
            if (getEntity() instanceof Ageable) {
                ((Ageable) getEntity()).setBaby();
                ((Ageable) getEntity()).setAgeLock(true);
            } else if (getEntity() instanceof Zombie) {
                ((Zombie) getEntity()).setBaby(true);
            }
        }

        if (config.getBoolean("aggro", true)) {
            if (getEntity() instanceof PigZombie) {
                ((PigZombie) getEntity()).setAngry(config.getBoolean("aggro", true));
            } else if (getEntity() instanceof Wolf) {
                ((Wolf) getEntity()).setAngry(config.getBoolean("aggro", true));
            }
        }

        setMaxHealth(MathUtil.RANDOM.nextInt(maxHealth) + minHealth);
        setHealth(getMaxHealth());
        setName(config.getString("name"));
        getEntity().setCanPickupItems(config.getBoolean("item-pickup", false));
        loadAbilities(config.getConfigurationSection("abilities"));
        equipItems(config.getConfigurationSection("equipment"));
        if (config.getBoolean("ranged", false)) {
            // TODO: investigate and fix
            // EntityUtil.setRangedMode(getEntity());
        }
        // set custom meta data to identify our mob
        MobsPlugin plugin = RaidCraft.getComponent(MobsPlugin.class);
        getEntity().setMetadata(EntityMetaData.RCMOBS_MOB_ID, new FixedMetadataValue(plugin, getId()));
        getEntity().setMetadata(EntityMetaData.RCMOBS_CUSTOM_MOB, new FixedMetadataValue(plugin, true));
        if (config.getBoolean("give-exp", true)) getEntity().setMetadata(EntityMetaData.RCMOBS_AWARD_EXP, new FixedMetadataValue(plugin, true));
        if (elite) getEntity().setMetadata(EntityMetaData.RCMOBS_ELITE, new FixedMetadataValue(plugin, true));
        if (rare) getEntity().setMetadata(EntityMetaData.RCMOBS_RARE, new FixedMetadataValue(plugin, true));
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
            playSound(hurtSound, hurtSoundPitch, 1.0F);
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
        if (!inCombat && resetHealth) {
            setHealth(getMaxHealth());
        }
    }

    @Override
    public void updateNameDisplay() {

        if (isInCombat()) {
            getEntity().setCustomName(EntityUtil.drawHealthBar(getHealth(), getMaxHealth(), ChatColor.WHITE));
        } else {
            getEntity().setCustomName(EntityUtil.drawMobName(
                    getName(),
                    getAttachedLevel().getLevel(),
                    ChatColor.YELLOW,
                    isElite(),
                    isRare()));
        }
    }

    @Override
    public boolean isRare() {

        return rare;
    }

    @Override
    public boolean isElite() {

        return elite;
    }

    @Override
    public boolean isSpawningNaturally() {

        return spawnNaturally;
    }

    @Override
    public boolean isWaterMob() {

        return water;
    }

    @Override
    public boolean isPassive() {

        return passive;
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
    public Optional<RDSTable> getLootTable() {

        return Optional.ofNullable(lootTable);
    }

    @Override
    public double getDamage() {

        int maxDmg = maxDamage;
        if (maxDamage <= minDamage) maxDmg = minDamage + 1;
        return MathUtil.RANDOM.nextInt(maxDmg - minDamage) + minDamage;
    }

    @Override
    public boolean kill() {

        playSound(deathSound, deathSoundPitch, 1.0F);
        boolean result = super.kill();
        getEntity().setPassenger(null);
        return result;
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
