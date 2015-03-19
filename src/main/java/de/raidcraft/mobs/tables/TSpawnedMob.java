package de.raidcraft.mobs.tables;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.MobsPlugin;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;

import javax.persistence.Column;
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
    private int id;
    @Column(unique = true)
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

    public static void delete(TSpawnedMob mob) {

        for (World world : Bukkit.getWorlds()) {
            world.getEntities().stream().filter(e -> e.getUniqueId().equals(mob.getUuid())).forEach(org.bukkit.entity.Entity::remove);
        }
        RaidCraft.getDatabase(MobsPlugin.class).delete(mob);
        if (mob.getMobGroupSource() != null && mob.getMobGroupSource().getSpawnedMobs().size() <= 1) {
            TSpawnedMobGroup.delete(mob.getMobGroupSource());
        }
    }
}
