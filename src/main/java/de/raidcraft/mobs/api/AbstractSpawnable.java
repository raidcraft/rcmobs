package de.raidcraft.mobs.api;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.UnknownMobException;
import de.raidcraft.mobs.events.RCMobGroupDeathEvent;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.mobs.tables.TSpawnedMobGroup;
import de.raidcraft.skills.api.character.CharacterTemplate;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.List;

/**
 * @author mdoering
 */
public abstract class AbstractSpawnable implements Spawnable, Listener {

    @Override
    public List<CharacterTemplate> spawn(String source, Location location) {

        remove(source);
        List<CharacterTemplate> entities = spawn(location);
        MobsPlugin plugin = RaidCraft.getComponent(MobsPlugin.class);
        for (CharacterTemplate entity : entities) {
            TSpawnedMob spawnedMob = plugin.getMobManager().getSpawnedMob(entity.getEntity());
            if (spawnedMob != null) {
                spawnedMob.setSourceId(source);
                plugin.getDatabase().update(spawnedMob);
            }
        }
        return entities;
    }

    @Override
    public void remove(String source) {

        MobsPlugin plugin = RaidCraft.getComponent(MobsPlugin.class);
        List<TSpawnedMob> spawnedMobs = plugin.getDatabase().find(TSpawnedMob.class).where().eq("source_id", source).findList();
        for (TSpawnedMob spawnedMob : spawnedMobs) {
            Mob mob = plugin.getMobManager().getMob(spawnedMob.getUuid());
            if (mob != null) {
                mob.getEntity().remove();
                plugin.getDatabase().delete(spawnedMob);
            }
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {

        MobManager component = RaidCraft.getComponent(MobManager.class);
        TSpawnedMob spawnedMob = component.getSpawnedMob(event.getEntity());
        if (spawnedMob != null) {
            TSpawnedMobGroup group = spawnedMob.getMobGroupSource();
            if (group != null) {
                try {
                    MobGroup mobGroup = component.getMobGroup(group.getMobGroup());
                    RaidCraft.callEvent(new RCMobGroupDeathEvent(spawnedMob.getSourceId(), mobGroup));
                } catch (UnknownMobException ignored) {
                }
            }
        }
    }
}
