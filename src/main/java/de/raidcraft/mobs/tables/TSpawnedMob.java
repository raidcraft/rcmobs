package de.raidcraft.mobs.tables;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.MobsPlugin;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
    private UUID uuid;
    private String sourceId;
    private String mob;
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

    public void delete() {

        RaidCraft.getDatabase(MobsPlugin.class).delete(this);
        if (getMobGroupSource() != null && getMobGroupSource().getSpawnedMobs().size() <= 1) {
            getMobGroupSource().delete();
        }
    }
}
