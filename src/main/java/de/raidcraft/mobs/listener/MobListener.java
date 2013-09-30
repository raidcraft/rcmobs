package de.raidcraft.mobs.listener;

import de.raidcraft.RaidCraft;
import de.raidcraft.loot.table.LootTableEntry;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.character.CharacterType;
import de.raidcraft.util.LocationUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * @author Silthus
 */
public class MobListener implements Listener {

    private final MobsPlugin plugin;

    public MobListener(MobsPlugin plugin) {

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

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        if (event.getEntityType() == EntityType.PLAYER) {
            return;
        }
        CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter(event.getEntity());
        if (character.getCharacterType() == CharacterType.CUSTOM_MOB) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            // add our custom drops
            if (character instanceof Mob && ((Mob) character).getLootTable() != null) {
                for (LootTableEntry loot : ((Mob) character).getLootTable().loot()) {
                    event.getDrops().add(loot.getItem());
                }
            }
        }
    }
}
