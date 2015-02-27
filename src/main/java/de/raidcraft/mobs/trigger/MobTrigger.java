package de.raidcraft.mobs.trigger;

import de.raidcraft.api.action.trigger.Trigger;
import de.raidcraft.mobs.events.RCMobDeathEvent;
import de.raidcraft.skills.api.hero.Hero;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * @author Silthus
 */
public class MobTrigger extends Trigger implements Listener {

    public MobTrigger() {

        super("mob", "kill");
    }

    @Information(
            value = "mob.kill",
            desc = "Is triggered when the given mob or any mob was killed by the player.",
            conf = {"mob"}
    )
    @EventHandler(ignoreCancelled = true)
    public void onMobDeath(RCMobDeathEvent event) {

        event.getMob().getInvolvedTargets().stream()
                .filter(target -> target instanceof Hero)
                .map(target -> (Hero) target)
                .forEach(hero -> informListeners("kill", hero.getPlayer(),
                        config -> (!config.isSet("mob")
                                || event.getMob().getId().equalsIgnoreCase(config.getString("mob"))
                            )
                        )
                );
    }
}
