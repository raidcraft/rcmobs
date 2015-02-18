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
public class MobGroupSpawnLocation implements Spawnable, Listener {

    private final int id;
    private final MobGroup spawnable;
    private final TMobGroupSpawnLocation databaseEntry;

    protected MobGroupSpawnLocation(TMobGroupSpawnLocation location) throws UnknownMobException {

        this.id = location.getId();
        MobManager manager = RaidCraft.getComponent(MobManager.class);
        this.spawnable = manager.getMobGroup(location.getSpawnGroup());
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

        return getDatabaseEntry().getSpawnedMobGroups().stream().mapToInt(group -> group.getSpawnedMobs().size()).sum();
    }

    public int getSpawnTreshhold() {

        return getDatabaseEntry().getRespawnTreshhold();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {

        getDatabaseEntry().getSpawnedMobGroups().forEach(group -> {
            Optional<TSpawnedMob> spawnedMob = group.getSpawnedMobs().stream()
                    .filter(mob -> mob.getUuid().equals(event.getEntity().getUniqueId()))
                    .findAny();
            if (spawnedMob.isPresent()) {
                RaidCraft.getDatabase(MobsPlugin.class).delete(spawnedMob.get());
            }
        });
    }

    public void spawn() {

        spawn(true);
    }

    public void spawn(boolean checkCooldown) {

        if (!getLocation().getChunk().isLoaded()) {
            return;
        }
        // dont spawn stuff if it is still on cooldown
        if (checkCooldown && System.currentTimeMillis() < getLastSpawn().getTime() + getCooldown()) {
            return;
        }
        if (getSpawnedMobCount() > getSpawnTreshhold()) {
            return;
        }
        // spawn the mob
        List<CharacterTemplate> newSpawnableMobs = spawn(getLocation());
        if (newSpawnableMobs != null) {
            EbeanServer db = RaidCraft.getDatabase(MobsPlugin.class);
            TSpawnedMobGroup spawnedMobGroup = new TSpawnedMobGroup();
            spawnedMobGroup.setSpawnGroupLocationSource(getDatabaseEntry());
            db.save(spawnedMobGroup);
            for (CharacterTemplate mob : newSpawnableMobs) {
                TSpawnedMob spawnedMob = db.find(TSpawnedMob.class, mob.getEntity().getUniqueId());
                spawnedMob.setMobGroupSource(spawnedMobGroup);
                db.save(spawnedMob);
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
