package de.raidcraft.mobs;

import com.avaje.ebean.EbeanServer;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.mobs.CustomNmsEntity;
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

/**
 * @author Silthus
 */
public class SpawnableMob extends AbstractSpawnable {

    private final String id;
    private final String mobName;
    private final Class<? extends Mob> mClass = ConfigurableCreature.class;
    private final EntityType type;
    private final String customEntityTypeName;
    private final boolean spawnNaturally;
    private final ConfigurationSection config;
    private double spawnChance = 1.0;

    public SpawnableMob(String id, String mobName, EntityType type, ConfigurationSection config) {

        this.id = id;
        this.mobName = mobName;
        this.type = type;
        this.customEntityTypeName = type == null ? config.getString("custom-type", "RCSkeleton") : null;
        this.spawnNaturally = config.getBoolean("spawn-naturally");
        this.config = config;
    }

    public SpawnableMob(String id, String mobName, ConfigurationSection config) {

        this(id, mobName, null, config);
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

    /**
     * Will respawn the given database mob from its unloaded state.
     * To save performance all respawned entities should be saved to database in one query.
     * If the entity in the database is not {@link de.raidcraft.mobs.tables.TSpawnedMob#isUnloaded()} the mob will not be spawned.
     *
     * @param dbMob          to respawn
     * @param saveToDatabase true if entity should be saved directly
     *
     * @return true if mob was spawned, false if mob was not unloaded
     */
    public boolean respawn(TSpawnedMob dbMob, boolean saveToDatabase) {

        if (!dbMob.isUnloaded()) return false;
        Location location = dbMob.getLocation();
        if (!location.getChunk().isLoaded()) return false;
        CharacterManager manager = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();
        Mob mob = null;
        if (type != null) {
            mob = manager.spawnCharacter(type, location, mClass, config);
        } else if (customEntityTypeName != null) {
            CustomNmsEntity nmsEntity = RaidCraft.getComponent(MobManager.class).getCustonNmsEntity(location.getWorld(), customEntityTypeName);
            mob = manager.wrapCharacter(nmsEntity.spawn(location), mClass, config);
        }
        if (mob == null) return false;
        dbMob.setUuid(mob.getUniqueId());
        dbMob.setSpawnTime(Timestamp.from(Instant.now()));
        dbMob.setUnloaded(false);
        if (saveToDatabase) RaidCraft.getDatabase(MobsPlugin.class).update(dbMob);
        return true;
    }

    @Override
    public List<CharacterTemplate> spawn(Location location, boolean force) {

        CharacterManager manager = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();
        // spawn is not forced so we calculate the spawn chance
        if (!force && getSpawnChance() < 1.0) {
            if (Math.random() > getSpawnChance()) {
                return new ArrayList<>();
            }
        }
        ArrayList<CharacterTemplate> mobs = new ArrayList<>();
        Mob mob = null;
        if (type != null) {
            mob = manager.spawnCharacter(type, location, mClass, config);
        } else if (customEntityTypeName != null) {
            CustomNmsEntity nmsEntity = RaidCraft.getComponent(MobManager.class).getCustonNmsEntity(location.getWorld(), customEntityTypeName);
            mob = manager.wrapCharacter(nmsEntity.spawn(location), mClass, config);
        }
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
    public List<CharacterTemplate> spawn(Location location) {

        return spawn(location, false);
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

    @Override
    public String toString() {

        return "SpawnableMob{" +
                "id='" + id + '\'' +
                ", mobName='" + mobName + '\'' +
                ", mClass=" + mClass +
                ", type=" + type +
                ", spawnNaturally=" + spawnNaturally +
                ", spawnChance=" + spawnChance +
                '}';
    }
}
