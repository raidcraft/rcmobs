package de.raidcraft.mobs.api;

import java.util.List;

/**
 * @author Silthus
 */
public interface MobGroup extends Spawnable {

    String getName();

    double getSpawnInterval();

    int getMinSpawnAmount();

    int getMaxSpawnAmount();

    int getRespawnTreshhold();

    boolean isInGroup(Spawnable spawnable);

    List<Spawnable> getSpawnables();
}
