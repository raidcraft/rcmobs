package de.raidcraft.mobs.tables;

import com.avaje.ebean.validation.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
@Entity
@Table(name = "rcmobs_mobgroup_spawn_location")
@Getter
@Setter
public class TMobGroupSpawnLocation {

    @Id
    private int id;
    @NotNull
    private String spawnGroup;
    private int x;
    private int y;
    private int z;
    private int chunkX;
    private int chunkZ;
    private String world;
    private double cooldown;
    private int respawnTreshhold;
    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp lastSpawn;
    @OneToMany(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "spawn_group_location_source_id")
    private List<TSpawnedMobGroup> spawnedMobGroups = new ArrayList<>();

    public Location getBukkitLocation() {

        return new Location(Bukkit.getWorld(getWorld()), getX(), getY(), getZ());
    }
}
