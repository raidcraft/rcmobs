package de.raidcraft.mobs;

import com.avaje.ebean.EbeanServer;
import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.mobs.tables.TMobSpawnLocation;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.TimeUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Silthus
 */
@Data
@EqualsAndHashCode(of = "id")
public class MobSpawnLocation implements Spawnable, Listener {

    private final int id;
    private final Spawnable spawnable;
    private final TMobSpawnLocation databaseEntry;
    private int spawnTreshhold = 0;

    protected MobSpawnLocation(TMobSpawnLocation location) throws UnknownMobException {

        this.id = location.getId();
        MobManager manager = RaidCraft.getComponent(MobManager.class);
        this.spawnable = manager.getSpwanableMob(location.getMob());
        this.databaseEntry = location;
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

        return getDatabaseEntry().getSpawnedMobs().size();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {

        Optional<TSpawnedMob> spawnedMob = getDatabaseEntry().getSpawnedMobs().stream()
                .filter(mob -> mob.getUuid().equals(event.getEntity().getUniqueId()))
                .findAny();
        if (spawnedMob.isPresent()) {
            RaidCraft.getDatabase(MobsPlugin.class).delete(spawnedMob.get());
        }
    }

    public void spawn() {

        spawn(true);
    }

    public void spawn(boolean checkCooldown) {

        if (!getLocation().getChunk().isLoaded()) {
            return;
        }
        // dont spawn stuff if it is still on cooldown
        if (getLastSpawn() != null && checkCooldown && System.currentTimeMillis() < getLastSpawn().getTime() + getCooldown()) {
            return;
        }
        if (getSpawnedMobCount() > getSpawnTreshhold()) {
            return;
        }
        // spawn the mob
        List<CharacterTemplate> newSpawnableMobs = spawn(getLocation());
        if (newSpawnableMobs != null) {
            EbeanServer db = RaidCraft.getDatabase(MobsPlugin.class);
            for (CharacterTemplate mob : newSpawnableMobs) {
                TSpawnedMob spawnedMob = db.find(TSpawnedMob.class).where().eq("uuid", mob.getEntity().getUniqueId()).findUnique();
                if (spawnedMob == null) {
                    spawnedMob = new TSpawnedMob();
                    spawnedMob.setMob(getDatabaseEntry().getMob());
                    spawnedMob.setSpawnTime(Timestamp.from(Instant.now()));
                    spawnedMob.setUuid(mob.getEntity().getUniqueId());
                    spawnedMob.setSpawnLocationSource(getDatabaseEntry());
                    db.save(spawnedMob);
                }
            }
            getDatabaseEntry().setLastSpawn(Timestamp.from(Instant.now()));
            db.save(getDatabaseEntry());
        }
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) {

        return getSpawnable().spawn(location);
    }
}
