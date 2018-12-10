package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.SpawnMobException;
import de.raidcraft.mobs.api.SpawnReason;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.mobs.tables.TMobGroupSpawnLocation;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.mobs.tables.TSpawnedMobGroup;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.TimeUtil;
import io.ebean.EbeanServer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Location;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Silthus
 */
@Data
@EqualsAndHashCode(of = "id")
public class MobGroupSpawnLocation implements Spawnable {

    private final int id;
    private final MobGroup spawnable;

    protected MobGroupSpawnLocation(TMobGroupSpawnLocation location, MobGroup spawnable) {

        this.id = location.getId();
        this.spawnable = spawnable;
    }

    public TMobGroupSpawnLocation getDatabaseEntry() {

        return RaidCraft.getDatabase(MobsPlugin.class).find(TMobGroupSpawnLocation.class, getId());
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

        try {
            MobsPlugin plugin = RaidCraft.getComponent(MobsPlugin.class);
            Logger logger = plugin.getLogger();

            if (getLocation().getWorld() == null) {
                return;
            }
            if (!getLocation().getWorld().isChunkLoaded(getDatabaseEntry().getChunkX(), getDatabaseEntry().getChunkZ())) {
                return;
            }
            // dont spawn stuff if it is still on cooldown
            if (checkCooldown && getLastSpawn() != null && getLastSpawn().toInstant().plusMillis(getCooldown()).isAfter(Instant.now())) {
                if (plugin.getConfiguration().debugMobSpawning) logger.info(toString() + " not spawning! Group is still on cooldown.");
                return;
            }
            int mobCount = getSpawnedMobCount();
            if (mobCount > getSpawnTreshhold()) {
                if (plugin.getConfiguration().debugMobSpawning) logger.info(toString() + " not spawning! Respawn treshhold (" + mobCount + "/" + getSpawnTreshhold() + ") not met.");
                return;
            }
            List<TSpawnedMob> remainingMobs = new ArrayList<>();
            if (mobCount > 0) {
                for (TSpawnedMobGroup group : getDatabaseEntry().getSpawnedMobGroups()) {
                    remainingMobs.addAll(group.getSpawnedMobs());
                }
            }
            // spawn the mob
            List<CharacterTemplate> newSpawnableMobs = spawn(getLocation(), SpawnReason.GROUP);
            if (newSpawnableMobs != null && !newSpawnableMobs.isEmpty()) {
                EbeanServer db = RaidCraft.getDatabase(MobsPlugin.class);
                MobManager component = RaidCraft.getComponent(MobManager.class);
                TSpawnedMob spawnedMob = component.getSpawnedMob(newSpawnableMobs.get(0).getEntity());
                if (spawnedMob != null) {
                    TSpawnedMobGroup mobGroup = spawnedMob.getMobGroupSource();
                    TMobGroupSpawnLocation entry = getDatabaseEntry();
                    if (mobGroup == null) {
                        if (!remainingMobs.isEmpty()) {
                            mobGroup = remainingMobs.get(0).getMobGroupSource();
                        } else {
                            mobGroup = new TSpawnedMobGroup();
                        }
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
                    entry.setCooldown(getSpawnable().getSpawnInterval());
                    db.update(entry);
                }
                if (plugin.getConfiguration().debugMobSpawning) logger.info(toString() + " spawned " + newSpawnableMobs.size() + " new mobs!");
            }
        } catch (SpawnMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
            RaidCraft.LOGGER.info("deleting mob group spawn location...");
            delete();
        }
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) {

        try {
            return getSpawnable().spawn(location);
        } catch (SpawnMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
            RaidCraft.LOGGER.info("deleting mob group spawn location...");
            delete();
        }
        return new ArrayList<>();
    }

    public void delete() {

        MobsPlugin plugin = RaidCraft.getComponent(MobsPlugin.class);
        plugin.getRcDatabase().delete(getDatabaseEntry());
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
