package de.raidcraft.mobs;

import de.raidcraft.mobs.tables.TSpawnedMob;

/**
 * @author Silthus
 */
public class QueuedRespawn {

    private final TSpawnedMob dbEntry;
    private final SpawnableMob mob;

    public QueuedRespawn(TSpawnedMob dbEntry, SpawnableMob mob) {

        this.dbEntry = dbEntry;
        this.mob = mob;
    }

    public void respawn() {

        mob.respawn(dbEntry, true);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof QueuedRespawn)) return false;

        QueuedRespawn that = (QueuedRespawn) o;

        if (!dbEntry.equals(that.dbEntry)) return false;

        return true;
    }

    @Override
    public int hashCode() {

        return dbEntry.hashCode();
    }
}
