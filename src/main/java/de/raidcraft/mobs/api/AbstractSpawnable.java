package de.raidcraft.mobs.api;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.SpawnableMob;
import de.raidcraft.mobs.events.RCMobGroupDeathEvent;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mdoering
 */
public abstract class AbstractSpawnable implements Spawnable, Listener {

    private final Map<String, List<CharacterTemplate>> sourcedSpawns = new HashMap<>();

    @Override
    public List<CharacterTemplate> spawn(String source, Location location) {

        remove(source);
        List<CharacterTemplate> entities = spawn(location);
        sourcedSpawns.put(source, entities);
        return entities;
    }

    @Override
    public void remove(String source) {

        if (sourcedSpawns.containsKey(source)) {
            sourcedSpawns.remove(source).forEach(CharacterTemplate::remove);
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {

        if (event.getEntity().hasMetadata("RC_CUSTOM_MOB")) {
            CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter(event.getEntity());
            sourcedSpawns.entrySet().stream().filter(entry -> entry.getValue().contains(character)).forEach(entry -> {
                entry.getValue().remove(character);
                if (entry.getValue().isEmpty() && this instanceof MobGroup) {
                    RaidCraft.callEvent(new RCMobGroupDeathEvent(entry.getKey(), (MobGroup) this));
                }
            });
        }
    }
}
