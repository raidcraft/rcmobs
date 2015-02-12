package de.raidcraft.mobs.groups;

import com.sk89q.worldedit.blocks.BlockType;
import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.SpawnableMob;
import de.raidcraft.mobs.UnknownMobException;
import de.raidcraft.mobs.api.AbstractSpawnable;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.MathUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
public class ConfigurableMobGroup extends AbstractSpawnable implements MobGroup {

    private final String name;
    private final int minInterval;
    private final int maxInterval;
    private final int minSpawnAmount;
    private final int maxSpawnAmount;
    private final int respawnTreshhold;
    private final List<Spawnable> mobs = new ArrayList<>();

    public ConfigurableMobGroup(String name, ConfigurationSection config) {

        this.name = name;
        minInterval = config.getInt("min-interval", 300);
        maxInterval = config.getInt("max-interval", minInterval);
        minSpawnAmount = config.getInt("min-amount", 1);
        maxSpawnAmount = config.getInt("max-amount", minSpawnAmount);
        respawnTreshhold = config.getInt("respawn-treshhold", minSpawnAmount - 1);
        if (config.getConfigurationSection("mobs") != null) {
            for (String key : config.getConfigurationSection("mobs").getKeys(false)) {
                try {
                    SpawnableMob mob = RaidCraft.getComponent(MobManager.class).getSpwanableMob(key);
                    ConfigurationSection section = config.getConfigurationSection("mobs").getConfigurationSection(key);
                    mob.setSpawnChance(section.getDouble("chance", 1.0));
                    mobs.add(mob);
                } catch (UnknownMobException e) {
                    RaidCraft.LOGGER.warning(e.getMessage());
                }
            }
        }
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public double getSpawnInterval() {

        //get the range, casting to long to avoid overflow problems
        long range = (long)maxInterval - (long)minInterval + 1;
        // compute a fraction of the range, 0 <= frac < range
        long fraction = (long)(range * MathUtil.RANDOM.nextDouble());
        int randomNumber =  (int)(fraction + minInterval);
        return randomNumber;
    }

    @Override
    public int getMinSpawnAmount() {

        return minSpawnAmount;
    }

    @Override
    public int getMaxSpawnAmount() {

        return maxSpawnAmount;
    }

    @Override
    public int getRespawnTreshhold() {

        return respawnTreshhold;
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
    public List<CharacterTemplate> spawn(Location location) {

        List<CharacterTemplate> spawnedMobs = new ArrayList<>();
        if (mobs.isEmpty()) {
            return spawnedMobs;
        }
        int amount = MathUtil.RANDOM.nextInt(getMaxSpawnAmount()) + getMinSpawnAmount();
        int i = 0;
        while (spawnedMobs.size() < amount) {
            Spawnable mob = mobs.get(i);
            // spawn with a slightly random offset
            // some workarounds to prevent endless loops
            Location newLocation = getRandomLocation(location, amount);
            boolean found = false;
            for(int k = 0; k < 100; k++) {
                if(newLocation.getBlock().getType() == Material.AIR
                        || newLocation.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR
                        || BlockType.canPassThrough(newLocation.getBlock().getType().getId())
                        || BlockType.canPassThrough(newLocation.getBlock().getRelative(BlockFace.UP).getType().getId())) {
                    found = true;
                    break;
                }
                newLocation = getRandomLocation(location, amount);
            }
            if(!found) {
                continue;
            }

            List<CharacterTemplate> spawn = mob.spawn(newLocation);
            if (spawn != null) spawnedMobs.addAll(spawn);
            i++;

            if (i >= mobs.size()) {
                i = 0;
            }
        }
        CharacterTemplate characterTemplate = spawnedMobs.get(0);
        for (CharacterTemplate mob : spawnedMobs) {
            mob.joinParty(characterTemplate.getParty());
        }
        return spawnedMobs;
    }

    private Location getRandomLocation(Location location, int amount) {

        return location.clone().add(
                MathUtil.RANDOM.nextInt(amount * 2) - amount,
                0,
                MathUtil.RANDOM.nextInt(amount * 2) - amount);
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
}
