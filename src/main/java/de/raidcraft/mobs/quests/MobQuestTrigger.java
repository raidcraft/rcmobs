package de.raidcraft.mobs.quests;

import de.raidcraft.api.quests.quest.trigger.Trigger;
import de.raidcraft.mobs.events.RCMobDeathEvent;
import de.raidcraft.skills.api.combat.action.Attack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * @author Silthus
 */
@QuestTrigger.Name("mob")
public class MobQuestTrigger extends QuestTrigger implements Listener {

    private String mobId;

    protected MobQuestTrigger(Trigger trigger) {

        super(trigger);
    }

    @Override
    protected void load(ConfigurationSection data) {

        mobId = data.getString("mob");
    }

    @Method("kill")
    @EventHandler(ignoreCancelled = true)
    public void onMobDeath(RCMobDeathEvent event) {

        // TODO: improve this to allow party killing
        Attack lastDamageCause = event.getMob().getLastDamageCause();
        if (event.getMob().getId().equalsIgnoreCase(mobId) && lastDamageCause != null && lastDamageCause.getAttacker().getEntity() instanceof Player) {
            inform("kill", (Player) lastDamageCause.getAttacker().getEntity());
        }
    }
}
