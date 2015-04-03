package de.raidcraft.mobs.trigger;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.trigger.Trigger;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.skills.api.events.RCEntityDeathEvent;
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
    public void onMobDeath(RCEntityDeathEvent event) {

        if (!(event.getCharacter() instanceof Mob)) return;
        MobManager mobManager = RaidCraft.getComponent(MobManager.class);
        Mob mob = (Mob) event.getCharacter();
        mob.getInvolvedTargets().stream()
                .filter(target -> target instanceof Hero)
                .map(target -> (Hero) target)
                .forEach(hero -> informListeners("kill", hero.getPlayer(), config -> {
                    if (config.isSet("mob")) {
                        return mob.getId().equalsIgnoreCase(config.getString("mob"));
                    }
                    if (config.isSet("group")) {
                        TSpawnedMob spawnedMob = mobManager.getSpawnedMob(mob.getEntity());
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
