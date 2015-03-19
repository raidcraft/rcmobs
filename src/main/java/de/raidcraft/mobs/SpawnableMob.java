package de.raidcraft.mobs;

import com.avaje.ebean.EbeanServer;
import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.AbstractSpawnable;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.creatures.ConfigurableCreature;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.SkillsPlugin;
import de.raidcraft.skills.api.character.CharacterTemplate;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Silthus
 */
public class SpawnableMob extends AbstractSpawnable {

    private final String id;
    private final String mobName;
    private final Class<? extends Mob> mClass = ConfigurableCreature.class;
    private final EntityType type;
    private final boolean spawnNaturally;
    private final ConfigurationSection config;
    private double spawnChance = 1.0;

    public SpawnableMob(String id, String mobName, EntityType type, ConfigurationSection config) {

        this.id = id;
        this.mobName = mobName;
        this.type = type;
        this.spawnNaturally = config.getBoolean("spawn-naturally");
        this.config = config;
    }

    public String getId() {

        return id;
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

    public boolean isSpawningNaturally() {

        return spawnNaturally;
    }

    public ConfigurationSection getConfig() {

        return config;
    }

    public double getSpawnChance() {

        return spawnChance;
    }

    public void setSpawnChance(double spawnChance) {

        this.spawnChance = spawnChance;
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) {

        Logger logger = RaidCraft.getComponent(MobsPlugin.class).getLogger();
        CharacterManager manager = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();
        // spawn is not forced so we calculate the spawn chance
        if (getSpawnChance() < 1.0) {
            if (Math.random() > getSpawnChance()) {
                return new ArrayList<>();
            }
        }
        ArrayList<CharacterTemplate> mobs = new ArrayList<>();
        Mob mob = manager.spawnCharacter(type, location, mClass, config);
        if (mob == null) {
            return mobs;
        }
        mob.setId(getId());
        EbeanServer database = RaidCraft.getDatabase(MobsPlugin.class);
        TSpawnedMob spawnedMob = RaidCraft.getComponent(MobManager.class).getSpawnedMob(mob.getEntity());
        if (spawnedMob == null) {
	        spawnedMob = new TSpawnedMob();
            spawnedMob.setMob(getId());
            spawnedMob.setSpawnTime(Timestamp.from(Instant.now()));
            spawnedMob.setUuid(mob.getEntity().getUniqueId());
            database.save(spawnedMob);
        }
        mobs.add(mob);
        return mobs;
    }

    @Override
    public String toString() {

        return mobName;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof SpawnableMob)) return false;

        SpawnableMob mob = (SpawnableMob) o;

        return id.equals(mob.id);
    }

    @Override
    public int hashCode() {

        return id.hashCode();
    }
}
