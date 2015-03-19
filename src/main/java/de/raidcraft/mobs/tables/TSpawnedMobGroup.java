package de.raidcraft.mobs.tables;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.MobsPlugin;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mdoering
 */
@Entity
@Table(name = "rcmobs_spawned_mob_groups")
@Getter
@Setter
public class TSpawnedMobGroup {

    @Id
    private int id;
    private String mobGroup;
    private Timestamp spawnTime;
    @ManyToOne(optional = true)
    private TMobGroupSpawnLocation spawnGroupLocationSource;
    @OneToMany(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "mob_group_source_id")
    private List<TSpawnedMob> spawnedMobs = new ArrayList<>();

    public static void delete(TSpawnedMobGroup group) {

        RaidCraft.getDatabase(MobsPlugin.class).delete(group);
        group.getSpawnedMobs().forEach(TSpawnedMob::delete);
    }
}
