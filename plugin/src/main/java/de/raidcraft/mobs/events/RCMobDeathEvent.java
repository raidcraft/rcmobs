package de.raidcraft.mobs.events;

import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.tables.TSpawnedMob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Optional;

/**
 * @author Silthus
 */
public class RCMobDeathEvent extends Event {


    private final Mob mob;
    private final TSpawnedMob spawnedMob;
    private final Player killer;

    public RCMobDeathEvent(Mob mob, TSpawnedMob spawnedMob, Player killer) {

        this.mob = mob;
        this.spawnedMob = spawnedMob;
        this.killer = killer;
    }

    public Mob getMob() {

        return mob;
    }

    public TSpawnedMob getSpawnedMob() {

        return spawnedMob;
    }

    public Optional<Player> getKiller() {

        return Optional.ofNullable(killer);
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
