package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
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
public class ConfigurableMobGroup implements MobGroup {

    private final String name;
    private final int minInterval;
    private final int maxInterval;
    private final int minSpawnAmount;
    private final int maxSpawnAmount;
    private final int respawnTreshhold;
    private final List<SpawnableMob> mobs = new ArrayList<>();

    protected ConfigurableMobGroup(String name, ConfigurationSection config) {

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

        return MathUtil.RANDOM.nextInt(maxInterval) + minInterval;
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
    public List<Spawnable> getSpawns() {

        return new ArrayList<Spawnable>(mobs);
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
            SpawnableMob mob = mobs.get(i);
            // spawn with a slightly random offset
            Location newLocation = getRandomLocation(location, amount);
            while (newLocation.getBlock().getType() != Material.AIR
                    || newLocation.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR) {
                newLocation = getRandomLocation(location, amount);
            }
            CharacterTemplate character = mob.spawn(newLocation, false);
            if (character != null) {
                spawnedMobs.add(character);
            }
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
}
