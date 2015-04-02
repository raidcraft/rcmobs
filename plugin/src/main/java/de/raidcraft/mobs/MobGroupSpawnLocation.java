package de.raidcraft.mobs;

import com.avaje.ebean.EbeanServer;
import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.mobs.tables.TMobGroupSpawnLocation;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.mobs.tables.TSpawnedMobGroup;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.TimeUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Location;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * @author Silthus
 */
@Data
@EqualsAndHashCode(of = "id")
public class MobGroupSpawnLocation implements Spawnable {

    private final int id;
    private final MobGroup spawnable;
    private TMobGroupSpawnLocation dbEntry;

    protected MobGroupSpawnLocation(TMobGroupSpawnLocation location, MobGroup spawnable) {

        this.id = location.getId();
        this.spawnable = spawnable;
    }

    public TMobGroupSpawnLocation getDatabaseEntry() {

        if (dbEntry == null) {
            dbEntry = RaidCraft.getDatabase(MobsPlugin.class).find(TMobGroupSpawnLocation.class, getId());
        }
        return dbEntry;
    }

    public Location getLocation() {

        return getDatabaseEntry().getBukkitLocation();
    }

    public Timestamp getLastSpawn() {

        return getDatabaseEntry().getLastSpawn();
    }

    public long getCooldown() {

        return TimeUtil.secondsToMillis(getDatabaseEntry().getCooldown());
    }

    public int getSpawnedMobCount() {

        int count = 0;
        for (TSpawnedMobGroup mobGroup : getDatabaseEntry().getSpawnedMobGroups()) {
            count += mobGroup.getSpawnedMobs().size();
        }
        return count;
    }

    public int getSpawnTreshhold() {

        return getDatabaseEntry().getRespawnTreshhold();
    }

    public void spawn() {

        spawn(true);
    }

    public void spawn(boolean checkCooldown) {

        if (!getLocation().getChunk().isLoaded()) {
            return;
        }
        // dont spawn stuff if it is still on cooldown
        if (checkCooldown && getLastSpawn() != null && System.currentTimeMillis() < getLastSpawn().getTime() + getCooldown()) {
            return;
        }
        if (getSpawnedMobCount() > getSpawnTreshhold()) {
            return;
        }
        // spawn the mob
        List<CharacterTemplate> newSpawnableMobs = spawn(getLocation(), !checkCooldown);
        if (newSpawnableMobs != null && !newSpawnableMobs.isEmpty()) {
            EbeanServer db = RaidCraft.getDatabase(MobsPlugin.class);
            MobManager component = RaidCraft.getComponent(MobManager.class);
            TSpawnedMob spawnedMob = component.getSpawnedMob(newSpawnableMobs.get(0).getEntity());
            if (spawnedMob != null) {
                TSpawnedMobGroup mobGroup = spawnedMob.getMobGroupSource();
                TMobGroupSpawnLocation entry = getDatabaseEntry();
                if (mobGroup == null) {
                    mobGroup = new TSpawnedMobGroup();
                    mobGroup.setMobGroup(getSpawnable().getName());
                    mobGroup.setSpawnTime(Timestamp.from(Instant.now()));
                    mobGroup.setSpawnGroupLocationSource(entry);
                    db.save(mobGroup);
                    for (CharacterTemplate mob : newSpawnableMobs) {
                        TSpawnedMob tSpawnedMob = component.getSpawnedMob(mob.getEntity());
                        if (tSpawnedMob != null) {
                            tSpawnedMob.setMobGroupSource(mobGroup);
                            db.update(tSpawnedMob);
                        }
                    }
                } else {
                    mobGroup.setSpawnGroupLocationSource(entry);
                    db.update(mobGroup);
                }
                entry.setLastSpawn(Timestamp.from(Instant.now()));
                db.update(entry);
            }
        }
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) {

        return getSpawnable().spawn(location);
    }

    public void delete() {

        MobsPlugin plugin = RaidCraft.getComponent(MobsPlugin.class);
        plugin.getDatabase().delete(getDatabaseEntry());
        plugin.getMobManager().removeSpawnLocation(this);
    }

    @Override
    public String toString() {

        return "MobGroupSpawnLocation{" +
                "spawnable=" + spawnable +
                "cooldown=" + getCooldown() +
                "treshhold=" + getSpawnTreshhold() +
                "spawnedMobs=" + getSpawnedMobCount() +
                "location=" + getLocation() +
                "lastSpawn=" + getLastSpawn() +
                '}';
    }
}
