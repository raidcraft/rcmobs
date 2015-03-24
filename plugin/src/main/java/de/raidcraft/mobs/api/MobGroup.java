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

    public int getRespawnTreshhold();

    public boolean isInGroup(Spawnable spawnable);

    public List<Spawnable> getSpawnables();
}
