package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.MathUtil;
import de.raidcraft.util.TimeUtil;
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
    private final long spawnInterval;
    private final int minSpawnAmount;
    private final int maxSpawnAmount;
    private final int spawnRadius;
    private final int respawnTreshhold;
    private final List<SpawnableMob> mobs = new ArrayList<>();

    protected ConfigurableMobGroup(String name, ConfigurationSection config) {

        this.name = name;
        spawnInterval = TimeUtil.secondsToTicks(config.getDouble("interval", 300.0));
        minSpawnAmount = config.getInt("min-amount", 1);
        maxSpawnAmount = config.getInt("max-amount", minSpawnAmount);
        respawnTreshhold = config.getInt("respawn-treshhold", minSpawnAmount - 1);
        spawnRadius = config.getInt("spawn-radius", 30);
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
    public int getSpawnRadius() {

        return spawnRadius;
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
    public void spawn(Location location) {

        if (mobs.isEmpty()) {
            return;
        }
        List<CharacterTemplate> spawnedMobs = new ArrayList<>();
        int amount = MathUtil.RANDOM.nextInt(getMaxSpawnAmount()) + getMinSpawnAmount();
        while (spawnedMobs.size() < amount) {
            SpawnableMob mob = mobs.get(MathUtil.RANDOM.nextInt(mobs.size()));
            // spawn with a slightly random offset
            Location newLocation = location.clone().add(
                    MathUtil.RANDOM.nextInt(6) - 3,
                    0,
                    MathUtil.RANDOM.nextInt(6) - 3);
            while (newLocation.getBlock().getType() != Material.AIR
                    || newLocation.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR) {
                newLocation.add(0, 1, 0);
            }
            CharacterTemplate character = mob.spawn(newLocation, false);
            if (character != null) {
                spawnedMobs.add(character);
            }
        }
        CharacterTemplate characterTemplate = spawnedMobs.get(0);
        for (CharacterTemplate mob : spawnedMobs) {
            mob.joinParty(characterTemplate.getParty());
        }
    }
}
