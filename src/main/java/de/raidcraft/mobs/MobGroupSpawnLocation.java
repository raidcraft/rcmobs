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

    protected MobGroupSpawnLocation(TMobGroupSpawnLocation location) throws UnknownMobException {

        this.id = location.getId();
        MobManager manager = RaidCraft.getComponent(MobManager.class);
        this.spawnable = manager.getMobGroup(location.getSpawnGroup());
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

        return getDatabaseEntry().getSpawnedMobGroups().stream().mapToInt(group -> group.getSpawnedMobs().size()).sum();
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
                if (mobGroup == null) {
                    mobGroup = new TSpawnedMobGroup();
                    mobGroup.setMobGroup(getSpawnable().getName());
                    mobGroup.setSpawnTime(Timestamp.from(Instant.now()));
                    mobGroup.setSpawnGroupLocationSource(getDatabaseEntry());
                    db.save(mobGroup);
                } else {
                    mobGroup.setSpawnGroupLocationSource(getDatabaseEntry());
                    db.update(getDatabaseEntry());
                }
                getDatabaseEntry().setLastSpawn(Timestamp.from(Instant.now()));
                db.update(getDatabaseEntry());
                for (CharacterTemplate mob : newSpawnableMobs) {
                    TSpawnedMob tSpawnedMob = component.getSpawnedMob(mob.getEntity());
                    if (tSpawnedMob != null) {
                        tSpawnedMob.setMobGroupSource(mobGroup);
                        db.update(tSpawnedMob);
                    }
                }
            }
        }
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) {

        return getSpawnable().spawn(location);
    }
}
