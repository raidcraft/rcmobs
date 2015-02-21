package de.raidcraft.mobs.tables;

import com.avaje.ebean.validation.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
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
    private String world;
    private double cooldown;
    private Timestamp lastSpawn;
    @OneToMany
    @JoinColumn(name = "spawn_location_source_id")
    private List<TSpawnedMob> spawnedMobs = new ArrayList<>();

    public Location getBukkitLocation() {

        return new Location(Bukkit.getWorld(getWorld()), getX(), getY(), getZ());
    }
}
