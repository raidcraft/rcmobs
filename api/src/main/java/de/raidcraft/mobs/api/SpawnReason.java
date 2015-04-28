package de.raidcraft.mobs.api;

import lombok.Getter;

/**
 * @author mdoering
 */
public enum SpawnReason {

    COMMAND(true),
    RESPAWN(true),
    SPAWN_LOCATION(false),
    GROUP(true),
    UNKNOWN(false);

    @Getter
    private final boolean forcingSpawn;

    SpawnReason(boolean forcingSpawn) {

        this.forcingSpawn = forcingSpawn;
    }
}
