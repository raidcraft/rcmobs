package de.raidcraft.mobs.quests;

import de.raidcraft.api.action.trigger.Trigger;
import de.raidcraft.mobs.events.RCMobDeathEvent;
import de.raidcraft.skills.api.combat.action.Attack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * @author Silthus
 */
public class MobQuestTrigger extends Trigger implements Listener {

    public MobQuestTrigger() {

        super("mob", "kill");
    }

    @EventHandler(ignoreCancelled = true)
    public void onMobDeath(RCMobDeathEvent event) {

        Attack lastDamageCause = event.getMob().getLastDamageCause();
        if (lastDamageCause == null || !(lastDamageCause.getAttacker().getEntity() instanceof Player)) return;
        Player player = (Player) lastDamageCause.getAttacker().getEntity();
        // TODO: improve this to allow party killing
        informListeners("kill", player, config -> !config.isSet("mob") || event.getMob().getId().equalsIgnoreCase(config.getString("mob")));
    }
}
