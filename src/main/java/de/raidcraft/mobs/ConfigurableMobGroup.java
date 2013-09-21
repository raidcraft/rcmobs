package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.util.MathUtil;
import de.raidcraft.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
public class ConfigurableMobGroup implements MobGroup {

    private final String name;
    private final long spawnInterval;
    private final int minSpawnAmount;
    private final int maxSpawnAmount;
    private final List<SpawnableMob> mobs = new ArrayList<>();

    protected ConfigurableMobGroup(String name, ConfigurationSection config) {

        this.name = name;
        spawnInterval = TimeUtil.secondsToTicks(config.getDouble("interval", 300.0));
        minSpawnAmount = config.getInt("min-amount", 1);
        maxSpawnAmount = config.getInt("max-amount", minSpawnAmount);
        if (config.getConfigurationSection("mobs") != null) {
            for (String key : config.getConfigurationSection("mobs").getKeys(false)) {
                try {
                    SpawnableMob mob = RaidCraft.getComponent(MobManager.class).getSpwanableMob(key);
                    ConfigurationSection section = config.getConfigurationSection("mobs").getConfigurationSection(key);
                    mob.setSpawnChance(section.getDouble("chance", 1.0));
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
    public long getSpawnInterval() {

        return spawnInterval;
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
    public List<Spawnable> getSpawns() {

        return new ArrayList<Spawnable>(mobs);
    }

    @Override
    public void spawn(Location location) {

        int amount = MathUtil.RANDOM.nextInt(getMaxSpawnAmount()) + getMinSpawnAmount();
        int spawnedAmount = 0;
        while (spawnedAmount < amount) {
            SpawnableMob mob = mobs.get(MathUtil.RANDOM.nextInt(mobs.size()));
            if (mob.spawn(location, false)) {
                spawnedAmount++;
            }
        }
    }
}
