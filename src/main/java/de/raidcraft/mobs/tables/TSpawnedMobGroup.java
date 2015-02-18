package de.raidcraft.mobs.tables;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
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
@Table(name = "rcmobs_mob_group_spawn_location")
@Getter
@Setter
public class TSpawnedMobGroup {

    @Id
    private int id;
    private String mobGroup;
    private Timestamp spawnTime;
    @ManyToOne(optional = true)
    private TMobGroupSpawnLocation spawnGroupLocationSource;
    @OneToMany
    private List<TSpawnedMob> spawnedMobs = new ArrayList<>();
}
