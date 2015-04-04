package de.raidcraft.mobs.trigger;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.trigger.Trigger;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.events.RCMobDeathEvent;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.hero.Hero;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

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

        MobManager mobManager = RaidCraft.getComponent(MobManager.class);
        Mob mob = event.getMob();
        Set<CharacterTemplate> involvedTargets = new HashSet<>(mob.getInvolvedTargets());
        involvedTargets.add(mob.getLastDamageCause().getAttacker());
        involvedTargets.stream()
                .filter(target -> target instanceof Hero)
                .map(target -> (Hero) target)
                .forEach(hero -> informListeners("kill", hero.getPlayer(), config -> {
                    if (config.isSet("mob")) {
                        return mob.getId().equalsIgnoreCase(config.getString("mob"));
                    }
                    if (config.isSet("group")) {
                        TSpawnedMob spawnedMob = event.getSpawnedMob();
                        if (spawnedMob != null && spawnedMob.getMobGroupSource() != null) {
                            return spawnedMob.getMobGroupSource().getMobGroup().equalsIgnoreCase(config.getString("group"));
                        }
                        return false;
                    }
                    if (config.isSet("mobs")) {
                        return config.getStringList("mobs").contains(mob.getId());
                    }
                    return true;
                }));
    }
}
