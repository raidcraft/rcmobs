package de.raidcraft.mobs.trigger;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.trigger.Trigger;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.events.RCMobGroupDeathEvent;
import de.raidcraft.mobs.tables.TSpawnedMobGroup;
import de.raidcraft.skills.api.hero.Hero;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

/**
 * @author mdoering
 */
public class MobGroupTrigger extends Trigger implements Listener {

    public MobGroupTrigger() {

        super("group", "kill");
    }

    @Information(
            value = "group.kill",
            desc = "Is triggered when the given mob group was killed. Will inform all involed players",
            conf = {"id", "group"}
    )
    @EventHandler(ignoreCancelled = true)
    public void onGroupKill(RCMobGroupDeathEvent event) {

        if (!(event.getCharacter() instanceof Mob)) return;
        Mob mob = (Mob) event.getCharacter();
        Optional<TSpawnedMobGroup> spawnedMobGroup = RaidCraft.getComponent(MobManager.class).getSpawnedMobGroup(mob.getEntity());
        if (spawnedMobGroup.isPresent()) {
            mob.getInvolvedTargets().stream()
                    .filter(target -> target instanceof Hero)
                    .map(target -> (Hero) target)
                    .forEach(hero -> informListeners("kill", hero.getPlayer(),
                            config -> {
                                if (config.isSet("group") && !config.getString("group").equals(spawnedMobGroup.get().getMobGroup())) return false;
                                if (config.isSet("id") && !config.getString("id").equals(event.getTrackingId())) return false;
                                return true;
                            })
                    );
        }
    }
}
