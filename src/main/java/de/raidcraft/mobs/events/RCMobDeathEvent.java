package de.raidcraft.mobs.events;

import de.raidcraft.mobs.api.Mob;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author Silthus
 */
public class RCMobDeathEvent extends Event {

    private final Mob mob;

    public RCMobDeathEvent(Mob mob) {

        this.mob = mob;
    }

    public Mob getMob() {

        return mob;
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
