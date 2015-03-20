package mobs.api;

import de.raidcraft.RaidCraft;
import de.raidcraft.skills.api.character.CharacterTemplate;
import mobs.MobsPlugin;
import mobs.tables.TSpawnedMob;
import org.bukkit.Location;
import org.bukkit.event.Listener;

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
}
