package de.raidcraft.mobs.creatures;

import com.comphenix.packetwrapper.Packet3ENamedSoundEffect;
import com.comphenix.protocol.ProtocolLibrary;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.items.CustomItemException;
import de.raidcraft.loot.LootPlugin;
import de.raidcraft.loot.api.table.LootTable;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.api.AbstractMob;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.effects.AbilityUser;
import de.raidcraft.skills.AbilityManager;
import de.raidcraft.skills.api.ability.Ability;
import de.raidcraft.skills.api.effect.common.Combat;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.skills.api.exceptions.UnknownSkillException;
import de.raidcraft.util.EntityUtil;
import de.raidcraft.util.LocationUtil;
import de.raidcraft.util.MathUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * @author Silthus
 */
public class ConfigurableCreature extends AbstractMob {

    private final int minDamage;
    private final int maxDamage;
    private final boolean resetHealth;
    private final boolean elite;
    private final boolean rare;
    private final LootTable lootTable;
    private final Location spawnLocation;
    private final String hurtSound;
    private final float hurtSoundPitch;
    private final String deathSound;
    private final float deathSoundPitch;

    public ConfigurableCreature(LivingEntity entity, ConfigurationSection config) {

        super(entity);
        this.spawnLocation = entity.getLocation();
        this.minDamage = config.getInt("min-damage");
        this.maxDamage = config.getInt("max-damage", minDamage);
        int minHealth = config.getInt("min-health", 20);
        int maxHealth = config.getInt("max-health", minHealth);
        this.resetHealth = config.getBoolean("reset-health", true);
        this.elite = config.getBoolean("elite", false);
        this.rare = config.getBoolean("rare", false);
        this.hurtSound = config.getString("sound.hurt", "random.classic_hurt");
        this.hurtSoundPitch = (float) config.getDouble("sound.hurt-pitch", 1.0);
        this.deathSound = config.getString("sound.death", "random.classic_hurt");
        this.deathSoundPitch = (float) config.getDouble("sound.death-pitch", 0.5);
        int minLevel = config.getInt("min-level", 1);
        int maxLevel = config.getInt("max-level", minLevel);
        getAttachedLevel().setLevel(MathUtil.RANDOM.nextInt(maxLevel) + minLevel);

        String tableName = config.getString("loot-table");
        if (tableName == null || tableName.equals("")) {
            this.lootTable = RaidCraft.getComponent(LootPlugin.class).getLootTableManager().getLevelDependantLootTable(getAttachedLevel().getLevel());
        } else {
            this.lootTable = RaidCraft.getComponent(LootPlugin.class).getLootTableManager().getTable(tableName);
        }

        if (config.getBoolean("baby")) {
            if (getEntity() instanceof Ageable) {
                ((Ageable) getEntity()).setBaby();
                ((Ageable) getEntity()).setAgeLock(true);
            } else if (getEntity() instanceof Zombie) {
                ((Zombie) getEntity()).setBaby(true);
            }
        }
        setMaxHealth(MathUtil.RANDOM.nextInt(maxHealth) + minHealth);
        setHealth(getMaxHealth());
        setName(config.getString("name"));
        loadAbilities(config.getConfigurationSection("abilities"));
        equipItems(config.getConfigurationSection("equipment"));
        if (config.getBoolean("ranged", false)) {
            // EntityUtil.setRangedMode(getEntity());
        }
        // set custom meta data to identify our mob
        MobsPlugin plugin = RaidCraft.getComponent(MobsPlugin.class);
        getEntity().setMetadata("RC_CUSTOM_MOB", new FixedMetadataValue(plugin, true));
        if (elite) getEntity().setMetadata("ELITE", new FixedMetadataValue(plugin, true));
        if (rare) getEntity().setMetadata("RARE", new FixedMetadataValue(plugin, true));
    }

    private void loadAbilities(ConfigurationSection config) {

        if (config == null) return;
        for (String key : config.getKeys(false)) {
            try {
                Ability<Mob> ability = RaidCraft.getComponent(AbilityManager.class).getAbility((Mob) this, key, config.getConfigurationSection(key));
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
            equipment.setItemInHand(RaidCraft.getItem(config.getString("hand", "AIR")));
            equipment.setHelmet(RaidCraft.getItem(config.getString("head", "AIR")));
            equipment.setChestplate(RaidCraft.getItem(config.getString("chest", "AIR")));
            equipment.setLeggings(RaidCraft.getItem(config.getString("legs", "AIR")));
            equipment.setBoots(RaidCraft.getItem(config.getString("boots", "AIR")));

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
    public void setHealth(double health) {

        // lets play the hurt sound if the new health is below our current
        if (health < getHealth()) {
            playSound(hurtSound, hurtSoundPitch, 1.0F);
        }
        super.setHealth(health);
        updateHealthBar();
    }

    @Override
    public void setMaxHealth(double maxHealth) {

        super.setMaxHealth(maxHealth);
        updateHealthBar();
    }

    @Override
    public void setInCombat(boolean inCombat) {

        super.setInCombat(inCombat);
        updateHealthBar();
        // reset the health to max
        if (!inCombat && resetHealth) {
            setHealth(getMaxHealth());
        }
    }

    private void updateHealthBar() {

        if (isInCombat()) {
            getEntity().setCustomName(EntityUtil.drawHealthBar(getHealth(), getMaxHealth(), ChatColor.WHITE));
        } else {
            getEntity().setCustomName(getName());
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

    public void checkSpawnPoint() {

        if (LocationUtil.getBlockDistance(getEntity().getLocation(), spawnLocation) > RaidCraft.getComponent(MobsPlugin.class).getConfiguration().resetRange) {
            try {
                getEntity().teleport(spawnLocation);
                removeEffect(Combat.class);
            } catch (CombatException ignored) {
            }
        } else if (!isInCombat()) {
            EntityUtil.walkToLocation(getEntity(), spawnLocation, 1.15F);
        }
    }

    @Override
    public LootTable getLootTable() {

        return lootTable;
    }

    @Override
    public double getDamage() {

        return MathUtil.RANDOM.nextInt(maxDamage - minDamage) + minDamage;
    }

    @Override
    public void kill() {

        playSound(deathSound, deathSoundPitch, 1.0F);
        super.kill();
    }

    private void playSound(String name, float pitch, float volume) {

        Location location = getEntity().getLocation();
        Packet3ENamedSoundEffect effect = new Packet3ENamedSoundEffect();
        effect.setSoundName(name);
        effect.setPitch(pitch);
        effect.setEffectPositionX(location.getX());
        effect.setEffectPositionY(location.getY());
        effect.setEffectPositionZ(location.getZ());
        effect.setVolume(volume);
        ProtocolLibrary.getProtocolManager().broadcastServerPacket(effect.getHandle());
    }
}
