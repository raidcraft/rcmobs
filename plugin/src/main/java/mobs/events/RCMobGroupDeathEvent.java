package mobs.events;

import de.raidcraft.skills.api.character.CharacterTemplate;
import mobs.api.MobGroup;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author mdoering
 */
public class RCMobGroupDeathEvent extends Event {

    private final String trackingId;
    private final MobGroup mob;
    private final CharacterTemplate character;

    public RCMobGroupDeathEvent(String trackingId, MobGroup mob, CharacterTemplate character) {

        this.trackingId = trackingId;
        this.mob = mob;
        this.character = character;
    }

    public MobGroup getMobGroup() {

        return mob;
    }

    public String getTrackingId() {

        return trackingId;
    }

    /**
     * Gets the entity that triggered the mob group death event.
     *
     * @return triggering entity
     */
    public CharacterTemplate getCharacter() {

        return character;
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
