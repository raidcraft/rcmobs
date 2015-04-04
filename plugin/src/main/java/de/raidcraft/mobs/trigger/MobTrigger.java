package de.raidcraft.mobs.trigger;

import de.raidcraft.api.action.trigger.Trigger;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.events.RCMobDeathEvent;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.skills.api.hero.Hero;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

        Mob mob = event.getMob();
        Set<Player> involvedTargets = new HashSet<>(mob.getInvolvedTargets()).stream()
                .filter(target -> target instanceof Hero)
                .map(target -> ((Hero) target).getPlayer())
                .collect(Collectors.toSet());
        if (event.getKiller().isPresent()) involvedTargets.add(event.getKiller().get());
        involvedTargets.stream()
                .forEach(player -> informListeners("kill", player, config -> {
                    if (config.isSet("mob")) {
                        return event.getSpawnedMob().getMob().equalsIgnoreCase(config.getString("mob"));
                    }
                    if (config.isSet("group")) {
                        TSpawnedMob spawnedMob = event.getSpawnedMob();
                        if (spawnedMob != null && spawnedMob.getMobGroupSource() != null) {
                            return spawnedMob.getMobGroupSource().getMobGroup().equalsIgnoreCase(config.getString("group"));
                        }
                        return false;
                    }
                    if (config.isSet("mobs")) {
                        return config.getStringList("mobs").contains(event.getSpawnedMob().getMob());
                    }
                    return true;
                }));
    }
}
