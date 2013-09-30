package de.raidcraft.mobs;

import de.raidcraft.mobs.api.FixedSpawnLocation;
import org.bukkit.Location;

/**
 * @author Silthus
 */
public class FixedSpawnableMob extends FixedSpawnLocation {

    private final SpawnableMob mob;

    public FixedSpawnableMob(SpawnableMob mob, Location location, double cooldown) {

        super(location, cooldown);
        this.mob = mob;
    }

    @Override
    protected int getSpawnRadius() {

        return mob.getConfig().getInt("spawn-radius", 10);
    }

    @Override
    public void spawn(Location location) {

        mob.spawn(location, true);
    }
}
