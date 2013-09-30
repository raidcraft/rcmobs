package de.raidcraft.mobs;

import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
public class FixedSpawnLocation implements Spawnable, Listener {

    private final Spawnable spawnable;
    private final Location location;
    private final long cooldown;
    private final int spawnRadius;
    private long lastSpawn;
    private int spawnTreshhold = 1;
    private List<CharacterTemplate> spawnedMobs;

    protected FixedSpawnLocation(Spawnable spawnable, Location location, double cooldown, int spawnRadius) {

        this.spawnable = spawnable;
        this.location = location;
        this.cooldown = TimeUtil.secondsToMillis(cooldown);
        this.spawnRadius = spawnRadius;
    }

    public Spawnable getSpawnable() {

        return spawnable;
    }

    protected int getSpawnRadius() {

        return spawnRadius;
    }

    public Location getLocation() {

        return location;
    }

    public long getCooldown() {

        return cooldown;
    }

    public long getLastSpawn() {

        return lastSpawn;
    }

    public int getSpawnTreshhold() {

        return spawnTreshhold;
    }

    public void setSpawnTreshhold(int spawnTreshhold) {

        this.spawnTreshhold = spawnTreshhold;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {

        ArrayList<CharacterTemplate> list = new ArrayList<>(spawnedMobs);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(0).getEntity().equals(event.getEntity())) {
                spawnedMobs.remove(i);
            }
        }
    }

    public void spawn() {

        // dont spawn stuff if it is still on cooldown
        if (System.currentTimeMillis() < lastSpawn + cooldown) {
            return;
        }
        if (getSpawnTreshhold() > 0 && spawnedMobs.size() > getSpawnTreshhold()) {
            return;
        }
        // spawn the mob
        spawnedMobs.addAll(spawn(location));
        lastSpawn = System.currentTimeMillis();
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) {

        return getSpawnable().spawn(location);
    }
}
