package de.raidcraft.mobs.listener;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.character.CharacterType;
import de.raidcraft.util.LocationUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * @author Silthus
 */
public class MobSpawnListener implements Listener {

    private final MobsPlugin plugin;

    public MobSpawnListener(MobsPlugin plugin) {

        this.plugin = plugin;
        plugin.registerEvents(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(CreatureSpawnEvent event) {

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            // check if there are custom mobs around and stop the spawning of the entity
            Entity[] entities = LocationUtil.getNearbyEntities(event.getLocation(), plugin.getConfiguration().defaultSpawnDenyRadius);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity) {
                    CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter((LivingEntity) entity);
                    if (character.getCharacterType() == CharacterType.CUSTOM_MOB) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }
}
