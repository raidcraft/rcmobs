package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.SpawnMobException;
import de.raidcraft.mobs.api.SpawnReason;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.mobs.tables.TMobSpawnLocation;
import de.raidcraft.mobs.tables.TSpawnedMob;
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

/**
 * @author Silthus
 */
@Data
@EqualsAndHashCode(of = "id")
public class MobSpawnLocation implements Spawnable {

    private final int id;
    private final Spawnable spawnable;

    protected MobSpawnLocation(TMobSpawnLocation location, Spawnable spawnable) {

        this.id = location.getId();
        this.spawnable = spawnable;
    }

    public TMobSpawnLocation getDatabaseEntry() {

        return RaidCraft.getDatabase(MobsPlugin.class).find(TMobSpawnLocation.class, getId());
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

    public boolean isSpawned() {

        return !getDatabaseEntry().getSpawnedMobs().isEmpty();
    }

    public void spawn() {

        spawn(true);
    }

    public void spawn(boolean checkCooldown) {

        try {
            if (!getLocation().getWorld().isChunkLoaded(getDatabaseEntry().getChunkX(), getDatabaseEntry().getChunkZ())) {
                return;
            }
            if (isSpawned()) {
                return;
            }
            // dont spawn stuff if it is still on cooldown
            if (getLastSpawn() != null && checkCooldown && System.currentTimeMillis() < getLastSpawn().getTime() + getCooldown()) {
                return;
            }
            // spawn the mob
            List<CharacterTemplate> newSpawnableMobs = spawn(getLocation(), SpawnReason.SPAWN_LOCATION);
            if (newSpawnableMobs != null) {
                EbeanServer db = RaidCraft.getDatabase(MobsPlugin.class);
                TMobSpawnLocation mobSpawnLocation = getDatabaseEntry();
                mobSpawnLocation.setLastSpawn(Timestamp.from(Instant.now()));
                db.update(mobSpawnLocation);
                for (CharacterTemplate mob : newSpawnableMobs) {
                    TSpawnedMob spawnedMob = RaidCraft.getComponent(MobManager.class).getSpawnedMob(mob.getEntity());
                    if (spawnedMob == null) {
                        spawnedMob = new TSpawnedMob();
                        spawnedMob.setMob(mobSpawnLocation.getMob());
                        spawnedMob.setSpawnTime(Timestamp.from(Instant.now()));
                        spawnedMob.setUuid(mob.getEntity().getUniqueId());
                        spawnedMob.setSpawnLocationSource(mobSpawnLocation);
                        db.save(spawnedMob);
                    } else {
                        spawnedMob.setSpawnLocationSource(mobSpawnLocation);
                        db.update(spawnedMob);
                    }
                }
            }
        } catch (SpawnMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
            RaidCraft.LOGGER.info("deleting mob spawn location...");
            delete();
        }
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) {

        try {
            return getSpawnable().spawn(location);
        } catch (SpawnMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
            RaidCraft.LOGGER.info("deleting mob spawn location...");
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

        return "MobSpawnLocation{" +
                "spawnable=" + spawnable +
                "cooldown=" + getCooldown() +
                "location=" + getLocation() +
                "lastSpawn=" + getLastSpawn() +
                '}';
    }
}
