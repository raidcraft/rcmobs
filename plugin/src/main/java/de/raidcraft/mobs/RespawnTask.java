package de.raidcraft.mobs;

import de.raidcraft.mobs.tables.TSpawnedMob;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mdoering
 */
public class RespawnTask extends BukkitRunnable {

    private final MobsPlugin plugin;
    private MobSpawnLocation[] mobSpawnLocations;
    private MobGroupSpawnLocation[] mobGroupSpawnLocations;
    private final Set<QueuedRespawn> respawnQueue = new HashSet<>();
    // how many mobs and groups should be updated per run
    private final int mobBatchCount;
    private final int mobGroupBatchCount;
    // tracks the current index where the task is at
    // to save performance the task will only update a certain amount of groups and mobs per run
    private int mobIndex = 0;
    private int mobGroupIndex = 0;

    public RespawnTask(MobsPlugin plugin, MobSpawnLocation[] mobSpawnLocations, MobGroupSpawnLocation[] mobGroupSpawnLocations) {

        this.plugin = plugin;
        this.mobSpawnLocations = mobSpawnLocations;
        this.mobGroupSpawnLocations = mobGroupSpawnLocations;
        this.mobBatchCount = plugin.getConfiguration().respawnTaskMobBatchCount;
        this.mobGroupBatchCount = plugin.getConfiguration().respawnTaskMobGroupBatchCount;
    }

    public void addToRespawnQueue(QueuedRespawn respawn) {

        respawnQueue.add(respawn);
    }

    public void updateMobSpawnLocation(MobSpawnLocation[] mobSpawnLocations) {
        this.mobSpawnLocations = mobSpawnLocations;
    }

    public void updateMobGroupSpawnLocation(MobGroupSpawnLocation[] mobGroupSpawnLocations) {
        this.mobGroupSpawnLocations = mobGroupSpawnLocations;
    }

    @Override
    public void run() {

        int startIndex = mobIndex;
        if (mobSpawnLocations.length > 0 && mobBatchCount > 0) {
            long startTime = System.nanoTime();
            for (int i = 0; i < mobBatchCount; i++) {
                mobSpawnLocations[mobIndex].spawn(true);
                mobIndex++;
                if (mobIndex >= mobSpawnLocations.length) mobIndex = 0;
                // do not try to respawn the same mob twice in one run
                if (mobIndex == startIndex) break;
            }
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
            if (plugin.getConfiguration().debugFixedSpawnLocations) {
                plugin.getLogger().info("... checked " + mobBatchCount + " mob spawn locations in " + (duration / 1000000L) + "ms");
            }
        }
        startIndex = mobGroupIndex;
        if (mobGroupSpawnLocations.length > 0 && mobGroupBatchCount > 0) {
            long startTime = System.nanoTime();
            for (int i = 0; i < mobGroupBatchCount; i++) {
                mobGroupSpawnLocations[mobGroupIndex].spawn(true);
                mobGroupIndex++;
                if (mobGroupIndex >= mobGroupSpawnLocations.length) mobGroupIndex = 0;
                // do not try to respawn the same mob twice in one run
                if (mobGroupIndex == startIndex) break;
            }
            long endTime = System.nanoTime();
            long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
            if (plugin.getConfiguration().debugFixedSpawnLocations) {
                plugin.getLogger().info("... checked " + mobGroupBatchCount + " mob group locations in " + (duration / 1000000L) + "ms");
            }
        }
        respawnQueue.forEach(de.raidcraft.mobs.QueuedRespawn::respawn);
        respawnQueue.clear();
        // check all unloaded mobs that are in loaded chunks and add them to the respawn queue
        List<TSpawnedMob> unloaded = plugin.getRcDatabase().find(TSpawnedMob.class).where().eq("unloaded", true).findList();
        unloaded.stream()
                .filter(mob -> mob.getLocation().getWorld() != null)
                .filter(mob -> mob.getLocation().getWorld().isChunkLoaded(mob.getChunkX(), mob.getChunkZ())).forEach(mob -> {
            try {
                SpawnableMob spawnableMob = plugin.getMobManager().getSpawnableMob(mob);
                spawnableMob.respawn(mob, true);
            } catch (UnknownMobException e) {
                e.printStackTrace();
            }
        });
    }
}
