package de.raidcraft.mobs.tables;

import io.ebean.annotation.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
@Entity
@Table(name = "rcmobs_mob_spawn_location")
@Data
@EqualsAndHashCode(of = "id")
public class TMobSpawnLocation {

    @Id
    private int id;
    @NotNull
    private String mob;
    private int x;
    private int y;
    private int z;
    private int chunkX;
    private int chunkZ;
    private String world;
    private double cooldown;
    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp lastSpawn;
    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    @JoinColumn(name = "spawn_location_source_id")
    private List<TSpawnedMob> spawnedMobs = new ArrayList<>();

    public Location getBukkitLocation() {

        return new Location(Bukkit.getWorld(getWorld()), getX(), getY(), getZ());
    }
}
