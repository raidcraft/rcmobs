package de.raidcraft.mobs.api;

import java.util.List;

/**
 * @author Silthus
 */
public interface MobGroup extends Spawnable {

    public String getName();

    public double getSpawnInterval();

    public int getMinSpawnAmount();

    public int getMaxSpawnAmount();

    public int getSpawnRadius();

    public int getRespawnTreshhold();

    public List<Spawnable> getSpawns();
}
