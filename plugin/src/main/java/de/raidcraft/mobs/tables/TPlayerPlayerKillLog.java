package de.raidcraft.mobs.tables;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * @author mdoering
 */
@Entity
@Getter
@Setter
@Table(name = "rcmobs_player_kill_player_log")
public class TPlayerPlayerKillLog {

    @Id
    private int id;
    private UUID killer;
    private UUID victim;
    private String world;
    private Timestamp timestamp;
}
