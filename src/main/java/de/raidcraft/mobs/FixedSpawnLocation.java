package de.raidcraft.mobs;

import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
public class FixedSpawnLocation implements Spawnable {

    private final Spawnable spawnable;
    private final Location location;
    private long cooldown;
    private long lastSpawn;
    private int spawnTreshhold = 0;
    private List<CharacterTemplate> spawnedMobs = new ArrayList<>();

    protected FixedSpawnLocation(Spawnable spawnable, Location location, double cooldown) {

        this.spawnable = spawnable;
        this.location = location;
        this.cooldown = TimeUtil.secondsToMillis(cooldown);
    }

    public Spawnable getSpawnable() {

        return spawnable;
    }

    public Location getLocation() {

        return location;
    }

    public void setCooldown(long cooldown) {

        this.cooldown = cooldown;
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

    public void validateSpawnedMobs() {

        for (CharacterTemplate characterTemplate : new ArrayList<>(spawnedMobs)) {
            LivingEntity entity = characterTemplate.getEntity();
            if (entity == null || !entity.isValid()) {
                spawnedMobs.remove(characterTemplate);
            }
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
        if (checkCooldown && System.currentTimeMillis() < lastSpawn + cooldown) {
            return;
        }
        validateSpawnedMobs();
        if (!spawnedMobs.isEmpty() && spawnedMobs.size() > getSpawnTreshhold()) {
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
