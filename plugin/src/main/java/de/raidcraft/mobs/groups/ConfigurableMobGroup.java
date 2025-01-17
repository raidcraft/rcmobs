package de.raidcraft.mobs.groups;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.random.RDSRandom;
import de.raidcraft.mobs.*;
import de.raidcraft.mobs.api.SpawnMobException;
import de.raidcraft.mobs.creatures.AbstractSpawnable;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.mobs.tables.TSpawnedMobGroup;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.BlockUtil;
import de.raidcraft.util.ConfigUtil;
import de.raidcraft.util.MathUtil;
import io.ebean.EbeanServer;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
@Data
public class ConfigurableMobGroup extends AbstractSpawnable implements MobGroup {

    private final String name;
    private final int minInterval;
    private final int maxInterval;
    private final int minSpawnAmount;
    private final int maxSpawnAmount;
    private final int respawnTreshhold;
    private final int spawnRadius;
    private final List<Spawnable> mobs = new ArrayList<>();

    public ConfigurableMobGroup(String name, ConfigurationSection config) {

        this.name = name;
        minInterval = config.getInt("min-interval", 300);
        maxInterval = config.getInt("max-interval", minInterval);
        minSpawnAmount = config.getInt("min-amount", 1);
        maxSpawnAmount = config.getInt("max-amount", minSpawnAmount);
        spawnRadius = config.getInt("spawn-radius", 5);
        respawnTreshhold = config.getInt("respawn-treshhold", minSpawnAmount - 1);
        if (config.getConfigurationSection("mobs") != null) {
            for (String key : config.getConfigurationSection("mobs").getKeys(false)) {
                createMob(config.getConfigurationSection("mobs").getConfigurationSection(key));
            }
        }
    }

    private void createMob(ConfigurationSection config) {
        try {
            if (config == null) return;
            if (!config.isSet("mob")) {
                RaidCraft.LOGGER.warning("Invalid OLD mob group format in file: " + ConfigUtil.getFileName(config) + "! Please set the mob as a own key 'mob: name'.");
            }
            SpawnableMob mob = RaidCraft.getComponent(MobManager.class).getSpwanableMob(config.getString("mob"));
            mob.setSpawnChance(config.getDouble("chance", 1.0));
            mobs.add(mob);
        } catch (UnknownMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage() + " in: " + ConfigUtil.getFileName(config));
        }
    }

    @Override
    public double getSpawnInterval() {
        return RDSRandom.getIntValue(minInterval, maxInterval);
    }

    @Override
    public boolean isInGroup(Spawnable spawnable) {

        return mobs.contains(spawnable);
    }

    @Override
    public List<Spawnable> getSpawnables() {

        return new ArrayList<>(mobs);
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) throws SpawnMobException {

        List<CharacterTemplate> spawnedMobs = new ArrayList<>();
        if (mobs.isEmpty()) {
            return spawnedMobs;
        }
        int amount = MathUtil.RANDOM.nextInt(getMaxSpawnAmount()) + getMinSpawnAmount();
        int i = 0;
        int maxRadius = getSpawnRadius();
        int radius = 0;
        while (spawnedMobs.size() < amount) {
            Spawnable mob = mobs.get(i);
            // spawn with a slightly random offset
            // some workarounds to prevent endless loops
            Location newLocation = getRandomLocation(location, radius);
            boolean found = false;
            for (int k = 0; k < 100; k++) {
                if (radius > maxRadius) radius = 0;
                if (!newLocation.getBlock().getType().isSolid()
                        && !newLocation.getBlock().getRelative(BlockFace.UP).getType().isSolid()
                        && !newLocation.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType().isSolid()) {
                    newLocation = newLocation.getBlock().getRelative(BlockFace.UP).getLocation();
                    radius++;
                    found = true;
                    break;
                }
                newLocation = getRandomLocation(location, radius++);
            }
            if (!found) {
                throw new SpawnMobException("cannot spawn " + getName() + " at " + location);
            }

            List<CharacterTemplate> spawn = mob.spawn(newLocation);
            if (spawn != null) spawnedMobs.addAll(spawn);
            i++;

            if (i >= mobs.size()) {
                i = 0;
            }
        }
        CharacterTemplate characterTemplate = spawnedMobs.get(0);
        EbeanServer db = RaidCraft.getDatabase(MobsPlugin.class);
        TSpawnedMobGroup spawnedMobGroup = new TSpawnedMobGroup();
        spawnedMobGroup.setMobGroup(getName());
        spawnedMobGroup.setSpawnTime(Timestamp.from(Instant.now()));
        db.save(spawnedMobGroup);
        for (CharacterTemplate mob : spawnedMobs) {
            mob.joinParty(characterTemplate.getParty());
            TSpawnedMob spawnedMob = RaidCraft.getComponent(MobManager.class).getSpawnedMob(mob.getEntity());
            if (spawnedMob != null) {
                spawnedMob.setMobGroupSource(spawnedMobGroup);
                db.update(spawnedMob);
            }
        }
        return spawnedMobs;
    }

    private Location getRandomLocation(Location location, int radius) {

        if (radius <= 0) {
            return location.clone();
        }

        Location newLoc = location.clone().add(
                RDSRandom.getIntNegativePositiveValue(-radius, radius),
                RDSRandom.getIntNegativePositiveValue(0, radius),
                RDSRandom.getIntNegativePositiveValue(-radius, radius));
        if (newLoc.getBlockY() > location.getWorld().getMaxHeight() - 4) newLoc.setY(location.getWorld().getMaxHeight() - 4);
        if (newLoc.getBlockY() < 4) newLoc.setY(4);
        return newLoc;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof ConfigurableMobGroup)) return false;

        ConfigurableMobGroup that = (ConfigurableMobGroup) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {

        return name.hashCode();
    }

    @Override
    public String toString() {

        return "ConfigurableMobGroup{" +
                "displayName='" + name + '\'' +
                ", minInterval=" + minInterval +
                ", maxInterval=" + maxInterval +
                ", minSpawnAmount=" + minSpawnAmount +
                ", maxSpawnAmount=" + maxSpawnAmount +
                ", respawnTreshhold=" + respawnTreshhold +
                ", mobs=" + mobs +
                '}';
    }
}
