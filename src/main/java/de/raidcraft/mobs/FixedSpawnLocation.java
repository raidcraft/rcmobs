package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
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

    public void setLastSpawn(long lastSpawn) {

        this.lastSpawn = lastSpawn;
    }

    public int getSpawnTreshhold() {

        return spawnTreshhold;
    }

    public void setSpawnTreshhold(int spawnTreshhold) {

        this.spawnTreshhold = spawnTreshhold;
    }

    public void addSpawnedMob(CharacterTemplate mob) {

        spawnedMobs.add(mob);
    }

    public int getSpawnedMobCount() {

        return spawnedMobs.size();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {

        if (event.getEntity().hasMetadata("RC_CUSTOM_MOB")) {
            CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter(event.getEntity());
            spawnedMobs.remove(character);
        }
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
