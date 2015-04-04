package de.raidcraft.mobs.events;

import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.tables.TSpawnedMob;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author Silthus
 */
public class RCMobDeathEvent extends Event {


    private final Mob mob;
    private final TSpawnedMob spawnedMob;

    public RCMobDeathEvent(Mob mob, TSpawnedMob spawnedMob) {

        this.mob = mob;
        this.spawnedMob = spawnedMob;
    }

    public Mob getMob() {

        return mob;
    }

    public TSpawnedMob getSpawnedMob() {

        return spawnedMob;
    }

    /*///////////////////////////////////////////////////
    //              Needed Bukkit Stuff
    ///////////////////////////////////////////////////*/

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}
