package de.raidcraft.mobs.nms.v1_13_R2;

import de.raidcraft.util.LocationUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_13_R2.EntityInsentient;
import net.minecraft.server.v1_13_R2.NavigationAbstract;
import net.minecraft.server.v1_13_R2.PathEntity;
import net.minecraft.server.v1_13_R2.PathfinderGoal;
import org.bukkit.Location;

/**
 * @author mdoering
 */
@Getter
@Setter
public class PathfinderGoalReturnToSpawn extends PathfinderGoal {

    private final NavigationAbstract navigation;
    private final EntityInsentient entity;
    private double speed;
    private Location spawnLocation;
    private int radius;

    public PathfinderGoalReturnToSpawn(EntityInsentient entity, Location spawnLocation, double speed, int radius) {

        this.entity = entity;
        this.navigation = entity.getNavigation();
        this.spawnLocation = spawnLocation;
        this.speed = speed;
        this.radius = radius;
    }

    // shouldStart()
    public boolean a() {

        Location currentLocation = new Location(entity.world.getWorld(), entity.locX, entity.locY, entity.locZ);
        return !LocationUtil.isWithinRadius(currentLocation, spawnLocation, radius);
    }

    // onStart()
    @Override
    public void c() {

        PathEntity pathEntity = navigation.a(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
        if (pathEntity != null) {
            navigation.a(pathEntity, speed);
        }
    }
}
