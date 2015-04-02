package de.raidcraft.mobs.api.events;

import de.raidcraft.mobs.api.SpawnReason;
import de.raidcraft.mobs.api.Spawnable;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author mdoering
 */
@Getter
@Setter
public class RCEntitySpawnEvent extends Event implements Cancellable {

    private final Spawnable entity;
    private final SpawnReason reason;
    private boolean cancelled;

    public RCEntitySpawnEvent(Spawnable entity, SpawnReason reason) {

        this.entity = entity;
        this.reason = reason;
    }

    // Bukkit stuff
    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}
