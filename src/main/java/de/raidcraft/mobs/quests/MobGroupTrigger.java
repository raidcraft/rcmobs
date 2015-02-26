package de.raidcraft.mobs.quests;

import de.raidcraft.api.action.trigger.Trigger;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.events.RCMobGroupDeathEvent;
import de.raidcraft.skills.api.hero.Hero;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Collection;

/**
 * @author mdoering
 */
public class MobGroupTrigger extends Trigger implements Listener {

    public MobGroupTrigger() {

        super("group", "kill");
    }
    
    @Information(
            value = "group.kill",
            desc = "Is triggered when the given mob group was killed by the player.",
            conf = {"group"}
    )

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onGroupKill(RCMobGroupDeathEvent event) {

        // may be buggy because it depends on the combat effect to calculate who was involed
        // TODO: track involved entities out of combat
        event.getMobGroup().getSpawnables().stream()
            .filter(spawnable -> spawnable instanceof Mob)
            .map(spawnable -> ((Mob) spawnable).getInvolvedTargets())
                .flatMap(Collection::stream)
                .filter(target -> target instanceof Hero)
                .distinct().forEach(hero ->
                        informListeners("kill", ((Hero) hero).getPlayer(),
                                config -> config.getString("id").equals(event.getGroupId())
                        )
                );
    }
}
