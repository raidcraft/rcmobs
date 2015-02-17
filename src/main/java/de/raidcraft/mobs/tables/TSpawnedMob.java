package de.raidcraft.mobs.tables;

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
@Table(name = "rcmobs_mob_spawn_location")
@Getter
@Setter
public class TSpawnedMob {

    @Id
    private int id;
    private UUID uuid;
    private Timestamp spawnTime;
    @ManyToOne(optional = true)
    private TMobSpawnLocation spawnLocationSource;
    @ManyToOne(optional = true)
    private TSpawnedMobGroup mobGroupSource;
}
