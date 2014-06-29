package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.TimeUtil;
import lombok.Getter;
import lombok.Setter;
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

    @Getter
    private final Spawnable spawnable;
    @Getter
    private final Location location;
    @Getter
    @Setter
    private long cooldown;
    @Getter
    @Setter
    private long lastSpawn;
    @Getter
    @Setter
    private int spawnTreshhold = 0;
    private List<CharacterTemplate> spawnedMobs = new ArrayList<>();

    protected FixedSpawnLocation(Spawnable spawnable, Location location, double cooldown) {

        this.spawnable = spawnable;
        this.location = location;
        this.cooldown = TimeUtil.secondsToMillis(cooldown);
    }

    public void addSpawnedMob(CharacterTemplate mob) {

        spawnedMobs.add(mob);
    }

    public void removeSpawnedMob(CharacterTemplate mob) {

        if (spawnedMobs.contains(mob)) {
            spawnedMobs.remove(mob);
        }
    }

    public int getSpawnedMobCount() {

        return spawnedMobs.size();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {

        if (event.getEntity().hasMetadata("RC_CUSTOM_MOB")) {
            CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter(event.getEntity());
            removeSpawnedMob(character);
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
        if (checkCooldown && System.currentTimeMillis() < getLastSpawn() + getCooldown()) {
            return;
        }
        validateSpawnedMobs();
        if (!spawnedMobs.isEmpty() && spawnedMobs.size() > getSpawnTreshhold()) {
            return;
        }
        // spawn the mob
        List<CharacterTemplate> newSpawnableMobs = spawn(getLocation());
        if (newSpawnableMobs != null) {
            if (newSpawnableMobs.size() > 1) {
                spawnedMobs.addAll(newSpawnableMobs);
            } else {
                spawnedMobs.add(newSpawnableMobs.get(0));
            }
        }
        setLastSpawn(System.currentTimeMillis());
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) {

        return getSpawnable().spawn(location);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof FixedSpawnLocation)) return false;

        FixedSpawnLocation location1 = (FixedSpawnLocation) o;

        return getLocation().equals(location1.getLocation()) && getSpawnable().equals(location1.getSpawnable());
    }

    @Override
    public int hashCode() {

        int result = spawnable.hashCode();
        result = 31 * result + location.hashCode();
        return result;
    }
}
