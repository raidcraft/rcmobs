package de.raidcraft.mobs.trigger;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.trigger.Trigger;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.events.RCMobGroupDeathEvent;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.mobs.tables.TSpawnedMobGroup;
import de.raidcraft.skills.api.hero.Hero;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onGroupKill(RCMobGroupDeathEvent event) {

        MobsPlugin plugin = RaidCraft.getComponent(MobsPlugin.class);
        List<TSpawnedMobGroup> groups = plugin.getDatabase().find(TSpawnedMobGroup.class).where()
                .eq("mob_group", event.getMobGroup().getName()).findList();
        for (TSpawnedMobGroup group : groups) {
            HashMap<String, Set<Hero>> heroes = new HashMap<>();
            for (TSpawnedMob spawnedMob : group.getSpawnedMobs()) {
                Mob mob = plugin.getMobManager().getMob(spawnedMob.getUuid());
                if (mob != null) {
                    if (!heroes.containsKey(spawnedMob.getSourceId())) {
                        heroes.put(spawnedMob.getSourceId(), new HashSet<>());
                    }
                    heroes.get(spawnedMob.getSourceId())
                            .addAll((mob.getInvolvedTargets().stream()
                                    .filter(target -> target instanceof Hero)
                                    .map(target -> (Hero) target)
                                    .collect(Collectors.toList())));
                }
            }
            for (Map.Entry<String, Set<Hero>> entry : heroes.entrySet()) {
                for (Hero hero : entry.getValue()) {
                    informListeners("kill", hero.getPlayer(), config -> {
                        if (config.isSet("group") && !config.getString("group").equals(group.getMobGroup())) return false;
                        if (config.isSet("id") && !config.getString("id").equals(entry.getKey())) return false;
                        return true;
                    });
                }
            }
        }
    }
}
