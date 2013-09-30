package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.character.CharacterType;
import de.raidcraft.util.LocationUtil;
import de.raidcraft.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * @author Silthus
 */
public class FixedSpawnLocation implements Spawnable {

    private final Spawnable spawnable;
    private final Location location;
    private final long cooldown;
    private final int spawnRadius;
    private long lastSpawn;
    private int spawnTreshhold = 1;

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

    public void spawn() {

        // dont spawn stuff if it is still on cooldown
        if (System.currentTimeMillis() < lastSpawn + cooldown) {
            return;
        }
        if (getSpawnTreshhold() > 0) {
            int nearby = 0;
            // dont spawn entities if there are other entities around in the radius
            Entity[] nearbyEntities = LocationUtil.getNearbyEntities(location, getSpawnRadius());
            for (Entity entity : nearbyEntities) {
                if (entity instanceof LivingEntity) {
                    CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter((LivingEntity) entity);
                    if (character.getCharacterType() == CharacterType.CUSTOM_MOB) {
                        nearby++;
                    }
                }
                if (getSpawnTreshhold() < nearby) {
                    return;
                }
            }
        }
        // spawn the mob
        spawn(location);
        lastSpawn = System.currentTimeMillis();
    }

    @Override
    public void spawn(Location location) {

        getSpawnable().spawn(location);
    }
}
