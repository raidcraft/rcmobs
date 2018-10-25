package de.raidcraft.mobs.creatures;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.random.RDSTable;
import de.raidcraft.loot.LootPlugin;
import de.raidcraft.loot.LootTableManager;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.api.MobConfig;
import de.raidcraft.mobs.util.CustomMobUtil;
import de.raidcraft.util.ConfigUtil;
import lombok.Data;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class YamlMobConfig implements MobConfig {

    private final ConfigurationSection config;
    private String name;
    private String customEntityType;
    private int minDamage;
    private int maxDamage;
    private int minlevel;
    private int maxLevel;
    private int minHealth;
    private int maxHealth;
    private boolean resetHealth;
    private boolean elite;
    private boolean rare;
    private boolean spawningNaturally;
    private boolean waterMob;
    private boolean passive;
    private boolean baby;
    private boolean aggro;
    private boolean ranged;
    private boolean giveExp;
    private boolean itemPickup;
    private String hurtSound;
    private float hurtSoundPitch;
    private String deathSound;
    private float deathSoundPitch;
    private ConfigurationSection abilities;
    private ConfigurationSection equipment;
    private final List<RDSTable> lootTables = new ArrayList<>();

    public YamlMobConfig() {
        this.config = new MemoryConfiguration();
    }

    public YamlMobConfig(ConfigurationSection config) {

        this.config = config;
        this.name = config.getString("name");
        this.customEntityType = config.getString("custom-type");
        this.minDamage = config.getInt("min-damage");
        this.maxDamage = config.getInt("max-damage", minDamage);
        this.resetHealth = config.getBoolean("reset-health", true);
        this.elite = config.getBoolean("elite", false);
        this.rare = config.getBoolean("rare", false);
        this.waterMob = config.getBoolean("water", false);
        this.passive = config.getBoolean("passive", false);
        this.baby = config.getBoolean("baby", false);
        this.aggro = config.getBoolean("aggro", true);
        this.ranged = config.getBoolean("ranged", false);
        this.giveExp = config.getBoolean("give-exp", true);
        this.itemPickup = config.getBoolean("item-pickup", false);
        this.spawningNaturally = config.getBoolean("spawn-naturally", false);
        this.hurtSound = config.getString("sound.hurt", Sound.ENTITY_GENERIC_HURT.name());
        this.hurtSoundPitch = (float) config.getDouble("sound.hurt-pitch", 1.0);
        this.deathSound = config.getString("sound.death", Sound.ENTITY_GENERIC_DEATH.name());
        this.deathSoundPitch = (float) config.getDouble("sound.death-pitch", 0.5);
        this.minlevel = config.getInt("min-level", 1);
        this.maxLevel = config.getInt("max-level", minlevel);
        this.minHealth = config.getInt("min-health", (int) CustomMobUtil.getMaxHealth(getMinlevel()));
        this.maxHealth = config.getInt("max-health", minHealth);
        this.abilities = config.getConfigurationSection("abilities");
        this.equipment = config.getConfigurationSection("equipment");

        LootTableManager tableManager = RaidCraft.getComponent(LootPlugin.class).getLootTableManager();
        if (tableManager != null) {
            RDSTable lootTable = tableManager.getLevelDependantLootTable(
                    config.getString("loot-table", RaidCraft.getComponent(MobsPlugin.class).getConfiguration().defaultLoottable),
                    getMinlevel());
            if (lootTable == null) {
                RaidCraft.LOGGER.warning("Loot-Table " + config.getString("loot-table") + " defined in mob " + ConfigUtil.getFileName(config) + " does not exist!");
            } else {
                this.lootTables.add(lootTable);
            }

            config.getStringList("loot-tables").stream()
                    .map(table -> tableManager.getLevelDependantLootTable(table, getMinlevel()))
                    .filter(Objects::nonNull)
                    .forEach(lootTables::add);
        }
    }

    @Override
    public void addLootTable(RDSTable table) {
        this.lootTables.add(table);
    }

    @Override
    public void removeLootTable(RDSTable table) {
        this.lootTables.remove(table);
    }

    @Override
    public void clearLootTables() {
        this.lootTables.clear();
    }
}
