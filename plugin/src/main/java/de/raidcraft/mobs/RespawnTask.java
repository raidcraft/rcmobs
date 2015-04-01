package de.raidcraft.mobs;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author mdoering
 */
public class RespawnTask extends BukkitRunnable {

    private final MobSpawnLocation[] mobSpawnLocations;
    private final MobGroupSpawnLocation[] mobGroupSpawnLocations;
    // how many mobs and groups should be updated per run
    private final int mobBatchCount;
    private final int mobGroupBatchCount;
    // tracks the current index where the task is at
    // to save performance the task will only update a certain amount of groups and mobs per run
    private int mobIndex = 0;
    private int mobGroupIndex = 0;

    public RespawnTask(MobsPlugin plugin, MobSpawnLocation[] mobSpawnLocations, MobGroupSpawnLocation[] mobGroupSpawnLocations) {

        this.mobSpawnLocations = mobSpawnLocations;
        this.mobGroupSpawnLocations = mobGroupSpawnLocations;
        this.mobBatchCount = plugin.getConfiguration().respawnTaskMobBatchCount;
        this.mobGroupBatchCount = plugin.getConfiguration().respawnTaskMobGroupBatchCount;
    }

    @Override
    public void run() {

        int startIndex = mobIndex;
        if (mobSpawnLocations.length > 0 && mobBatchCount > 0) {
            for (int i = 0; i < mobBatchCount; i++) {
                mobSpawnLocations[mobIndex].spawn(true);
                mobIndex++;
                if (mobIndex >= mobSpawnLocations.length) mobIndex = 0;
                // do not try to respawn the same mob twice in one run
                if (mobIndex == startIndex) break;
            }
        }
        startIndex = mobGroupIndex;
        if (mobGroupSpawnLocations.length > 0 && mobGroupBatchCount > 0) {
            for (int i = 0; i < mobGroupBatchCount; i++) {
                mobGroupSpawnLocations[mobGroupIndex].spawn(true);
                mobGroupIndex++;
                if (mobGroupIndex >= mobGroupSpawnLocations.length) mobGroupIndex = 0;
                // do not try to respawn the same mob twice in one run
                if (mobGroupIndex == startIndex) break;
            }
        }
    }
}
