package de.raidcraft.mobs.events;

import de.raidcraft.mobs.api.MobGroup;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author mdoering
 */
public class RCMobGroupDeathEvent extends Event {

    private final String id;
    private final MobGroup mob;

    public RCMobGroupDeathEvent(String id, MobGroup mob) {

        this.id = id;
        this.mob = mob;
    }

    public MobGroup getMobGroup() {

        return mob;
    }

    public String getGroupId() {

        return id;
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
