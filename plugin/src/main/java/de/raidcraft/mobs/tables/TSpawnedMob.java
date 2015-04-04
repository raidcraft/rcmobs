package de.raidcraft.mobs.tables;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.MobsPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * @author mdoering
 */
@Entity
@Table(name = "rcmobs_spawned_mobs")
@Getter
@Setter
public class TSpawnedMob {

    @Id
    private int id;
    @Column(unique = true)
    private UUID uuid;
    private String sourceId;
    private String mob;
    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp spawnTime;
    @ManyToOne(optional = true)
    private TMobSpawnLocation spawnLocationSource;
    @ManyToOne(optional = true)
    private TSpawnedMobGroup mobGroupSource;
    private boolean unloaded;
    private String world;
    private int x;
    private int y;
    private int z;
    private int chunkX;
    private int chunkZ;
    @Version
    public Timestamp lastUpdate;

    public Location getLocation() {

        return new Location(Bukkit.getWorld(getWorld()), (double) getX(), (double) getY(), (double) getZ());
    }

    public void delete() {

        RaidCraft.getDatabase(MobsPlugin.class).delete(this);
        if (getMobGroupSource() != null && getMobGroupSource().getSpawnedMobs().size() <= 1) {
            getMobGroupSource().delete();
        }
    }
}
