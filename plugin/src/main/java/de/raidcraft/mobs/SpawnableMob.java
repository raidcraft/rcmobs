package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.creatures.AbstractSpawnable;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.MobConfig;
import de.raidcraft.mobs.api.SpawnReason;
import de.raidcraft.mobs.api.events.RCEntitySpawnEvent;
import de.raidcraft.mobs.creatures.ConfigurableCreature;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.mobs.util.CustomMobUtil;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.SkillsPlugin;
import de.raidcraft.skills.api.character.CharacterTemplate;
import io.ebean.EbeanServer;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Silthus
 */
@Data
public class SpawnableMob extends AbstractSpawnable {

    private final String id;
    private final String mobName;
    private final Class<? extends Mob> mClass = ConfigurableCreature.class;
    private final EntityType type;
    private final String customEntityTypeName;
    private final MobConfig config;
    private double spawnChance = 1.0;

    public SpawnableMob(String id, String mobName, EntityType type, MobConfig config) {

        this.id = id;
        this.mobName = mobName;
        this.type = type;
        this.customEntityTypeName = config.getCustomEntityType();
        this.config = config;
    }

    public SpawnableMob(String id, String mobName, MobConfig config) {

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

        return getConfig().isSpawningNaturally();
    }

    public double getSpawnChance() {

        return spawnChance;
    }

    public void setSpawnChance(double spawnChance) {

        this.spawnChance = spawnChance;
    }

    /**
     * Wraps the given living entity into a managed {@link de.raidcraft.skills.api.character.CharacterTemplate}
     * allowing it to use skills, custom health, etc.
     *
     * @param entity to respawn/wrap
     * @return wrapped entity
     */
    public boolean respawn(TSpawnedMob dbEntry, LivingEntity entity, boolean saveToDatabase) {

        if (dbEntry.getWorld() == null) return false;
        if (!dbEntry.getLocation().getWorld().isChunkLoaded(dbEntry.getChunkX(), dbEntry.getChunkZ())) return false;

        RCEntitySpawnEvent event = new RCEntitySpawnEvent(this, SpawnReason.RESPAWN);
        RaidCraft.callEvent(event);
        if (event.isCancelled()) return false;

        Mob mob = RaidCraft.getComponent(CharacterManager.class).wrapCharacter(entity, mClass, config);
        if (mob == null) return false;
        mob.setId(dbEntry.getMob());
        dbEntry.setUnloaded(false);
        if (saveToDatabase) {
            RaidCraft.getDatabase(MobsPlugin.class).save(dbEntry);
        }
        return true;
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
        if (dbMob.getLocation().getWorld() == null) return false;
        Location location = dbMob.getLocation();
        if (!dbMob.getLocation().getWorld().isChunkLoaded(dbMob.getChunkX(), dbMob.getChunkZ())) return false;

        RCEntitySpawnEvent event = new RCEntitySpawnEvent(this, SpawnReason.RESPAWN);
        RaidCraft.callEvent(event);
        if (event.isCancelled()) return false;

        Optional<Mob> optional = spawnMob(location);
        if (!optional.isPresent()) return false;
        Mob mob = optional.get();
        mob.setId(dbMob.getMob());
        dbMob.setUuid(mob.getUniqueId());
        dbMob.setSpawnTime(Timestamp.from(Instant.now()));
        dbMob.setUnloaded(false);
        if (saveToDatabase) RaidCraft.getDatabase(MobsPlugin.class).update(dbMob);
        return true;
    }

    @Override
    public List<CharacterTemplate> spawn(Location location, SpawnReason reason) {

        ArrayList<CharacterTemplate> mobs = new ArrayList<>();

        if (location.getWorld() == null) return mobs;

        RCEntitySpawnEvent event = new RCEntitySpawnEvent(this, reason);
        RaidCraft.callEvent(event);
        if (event.isCancelled()) return mobs;


        // spawn is not forced so we calculate the spawn chance
        if (!reason.isForcingSpawn() && getSpawnChance() < 1.0) {
            if (Math.random() > getSpawnChance()) {
                return new ArrayList<>();
            }
        }
        Optional<Mob> optional = spawnMob(location);
        if (!optional.isPresent()) {
            return mobs;
        }
        Mob mob = optional.get();
        mob.setId(getId());
        EbeanServer database = RaidCraft.getDatabase(MobsPlugin.class);
        TSpawnedMob spawnedMob = RaidCraft.getComponent(MobManager.class).getSpawnedMob(mob.getEntity());
        if (spawnedMob == null) {
            spawnedMob = new TSpawnedMob();
            spawnedMob.setMob(getId());
            spawnedMob.setSpawnTime(Timestamp.from(Instant.now()));
            spawnedMob.setUuid(mob.getEntity().getUniqueId());
            spawnedMob.setChunkX(location.getChunk().getX());
            spawnedMob.setChunkZ(location.getChunk().getZ());
            spawnedMob.setWorld(location.getWorld().getName());
            spawnedMob.setX(location.getBlockX());
            spawnedMob.setY(location.getBlockY());
            spawnedMob.setZ(location.getBlockZ());
            database.save(spawnedMob);
        }
        mobs.add(mob);
        return mobs;
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) {

        return spawn(location, SpawnReason.UNKNOWN);
    }

    private Optional<Mob> spawnMob(Location location) {

        CharacterManager manager = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();
        Mob mob = null;
        if (type != null || customEntityTypeName != null) {
            Optional<? extends Mob> optional = Optional.empty();
            if (customEntityTypeName != null) {
                optional = CustomMobUtil.spawnNMSEntity(customEntityTypeName, location, mClass, config);
            }
            if (!optional.isPresent()) {
                mob = manager.spawnCharacter(type, location, mClass, config);
            } else {
                mob = optional.get();
            }
        }

        if (mob != null) mob.updateNameDisplay();

        return Optional.ofNullable(mob);
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
                ", spawnNaturally=" + getConfig().isSpawningNaturally() +
                ", spawnChance=" + spawnChance +
                '}';
    }
}
