package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.creatures.ConfigurableCreature;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.SkillsPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * @author Silthus
 */
public class SpawnableMob {

    private final String mobName;
    private final Class<? extends Mob> mClass = ConfigurableCreature.class;
    private final EntityType type;
    private final double spawnChance;
    private final boolean spawnNaturally;
    private final ConfigurationSection config;

    public SpawnableMob(String mobName, EntityType type, ConfigurationSection config) {

        this.mobName = mobName;
        this.type = type;
        this.spawnChance = config.getDouble("spawn-chance");
        this.spawnNaturally = config.getBoolean("spawn-naturally", true);
        this.config = config;
    }

    public String getMobName() {

        return mobName;
    }

    public Class<? extends Mob> getmClass() {

        return mClass;
    }

    public EntityType getType() {

        return type;
    }

    public ConfigurationSection getConfig() {

        return config;
    }

    public double getSpawnChance() {

        return spawnChance;
    }

    public boolean isSpawningNaturally() {

        return spawnNaturally;
    }

    public boolean spawn(CreatureSpawnEvent event) {

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL && Math.random() < getSpawnChance()) {
            event.setCancelled(true);
            spawn(event.getLocation());
            return true;
        }
        return false;
    }

    public Mob spawn(Location location) {

        CharacterManager manager = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();
        return manager.spawnCharacter(type, location, mClass, config);
    }
}
