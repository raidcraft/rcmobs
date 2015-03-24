package de.raidcraft.mobs.trigger;

import de.raidcraft.api.action.trigger.Trigger;
import de.raidcraft.skills.api.events.RCEntityDeathEvent;
import de.raidcraft.skills.api.hero.Hero;
import de.raidcraft.mobs.api.Mob;
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
    public void onMobDeath(RCEntityDeathEvent event) {

        if (!(event.getCharacter() instanceof Mob)) return;
        Mob mob = (Mob) event.getCharacter();
        mob.getInvolvedTargets().stream()
                .filter(target -> target instanceof Hero)
                .map(target -> (Hero) target)
                .forEach(hero -> informListeners("kill", hero.getPlayer(),
                        config -> (!config.isSet("mob")
                                || mob.getId() == null
                                || mob.getId().equalsIgnoreCase(config.getString("mob"))
                            )
                        )
                );
    }
}
