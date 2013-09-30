package de.raidcraft.mobs;

import de.raidcraft.mobs.api.FixedSpawnLocation;
import de.raidcraft.mobs.api.MobGroup;
import org.bukkit.Location;

/**
 * @author Silthus
 */
public class FixedSpawnableMobGroup extends FixedSpawnLocation {

    private final MobGroup mobGroup;

    public FixedSpawnableMobGroup(MobGroup mobGroup, Location location, double cooldown) {

        super(location, cooldown);
        this.mobGroup = mobGroup;
    }

    @Override
    protected int getSpawnRadius() {

        return mobGroup.getSpawnRadius();
    }

    @Override
    public void spawn(Location location) {

        mobGroup.spawn(location);
    }
}
