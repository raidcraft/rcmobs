package de.raidcraft.mobs.events;

import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.skills.api.character.CharacterTemplate;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Silthus
 */
@Getter
public class RCMobDeathEvent extends Event {

    private final Mob mob;
    private final TSpawnedMob spawnedMob;
    private final CharacterTemplate killer;
    private final Set<CharacterTemplate> involvedTargets = new HashSet<>();

    public RCMobDeathEvent(Mob mob, TSpawnedMob spawnedMob, CharacterTemplate killer) {

        this.mob = mob;
        this.spawnedMob = spawnedMob;
        this.killer = killer;

        this.involvedTargets.addAll(mob.getInvolvedTargets());
        if (killer != null) involvedTargets.add(killer);
    }

    public Optional<CharacterTemplate> getKiller() {

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
