package de.raidcraft.mobs.tables;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

/**
 * @author mdoering
 */
@Entity
@Getter
@Setter
@Table(name = "rcmobs_mob_player_kill_log")
public class TMobPlayerKillLog {

    @Id
    private int id;
    private UUID uuid;
    private String mob;
    private int killCount;
}
