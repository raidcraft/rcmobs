package de.raidcraft.mobs.api;

import java.util.List;

/**
 * @author Silthus
 */
public interface MobGroup extends Spawnable {

    public String getName();

    public long getSpawnInterval();

    public int getMinSpawnAmount();

    public int getMaxSpawnAmount();

    public int getSpawnRadius();

    public List<Spawnable> getSpawns();
}
