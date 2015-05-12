package de.raidcraft.mobs.listener;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.events.RCEntityRemovedEvent;
import de.raidcraft.api.random.Dropable;
import de.raidcraft.api.random.RDSTable;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.RespawnTask;
import de.raidcraft.mobs.SpawnableMob;
import de.raidcraft.mobs.UnknownMobException;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.events.RCMobDeathEvent;
import de.raidcraft.mobs.events.RCMobGroupDeathEvent;
import de.raidcraft.mobs.tables.TMobPlayerKillLog;
import de.raidcraft.mobs.tables.TPlayerMobKillLog;
import de.raidcraft.mobs.tables.TPlayerPlayerKillLog;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.SkillsPlugin;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.util.BukkitUtil;
import de.raidcraft.util.MathUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Silthus
 */
public class MobListener implements Listener {

    private final MobsPlugin plugin;
    private final CharacterManager characterManager;

    public MobListener(MobsPlugin plugin) {

        this.plugin = plugin;
        this.characterManager = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {

        if (!(event.getEntity() instanceof LivingEntity) || !plugin.getMobManager().isSpawnedMob((LivingEntity) event.getEntity())) {
            return;
        }
        CharacterTemplate character = characterManager.getCharacter((LivingEntity) event.getEntity());
        if (!(character instanceof Mob)) {
            return;
        }
        try {
            Mob attacker = (Mob) character;
            CharacterTemplate target = attacker.getTarget();
            if (target == null) {
                // just let the mob do a normal search
                return;
            }
            event.setTarget(target.getEntity());
        } catch (CombatException e) {
            // this should be never thrown due to performance issues
            // mobs check for targets every tick and throwing exceptions is very costly
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSlimeSplit(SlimeSplitEvent event) {

        if (plugin.getConfiguration().preventSlimeSplitting) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntitySpawn(CreatureSpawnEvent event) {

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        if (plugin.getConfiguration().getIgnoredEntityTypes().contains(event.getEntityType())) {
            return;
        }
        if (plugin.getConfiguration().getIgnoredSpawnReasons().contains(event.getSpawnReason())) {
            return;
        }
        if (plugin.getConfiguration().getDeniedEntities().contains(event.getEntityType())) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity().getType() == EntityType.HORSE && plugin.getConfiguration().denyHorseSpawning) {
            event.setCancelled(true);
            return;
        }
        if (!plugin.getConfiguration().getReplacedMobs().contains(event.getEntity().getType().name())) {
            return;
        }
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
            // check if there are custom mobs around and stop the spawning of the entity
            if (!plugin.getMobManager().isAllowedNaturalSpawn(event.getLocation())) {
                event.setCancelled(true);
                return;
            }
            // lets replace all natural mobs with our own
            List<MobGroup> virtualGroups = plugin.getMobManager().getVirtualGroups();
            if (virtualGroups.isEmpty()) {
                event.setCancelled(true);
                return;
            }
            List<SpawnableMob> nearbyMobs = new ArrayList<>();
            if (plugin.getConfiguration().spawnSimiliarRandomMobs) {
                // first we want to check all nearby entities for custom mobs so we can spawn more of the same type
                for (LivingEntity entity : BukkitUtil.getNearbyEntities(event.getEntity(), plugin.getConfiguration().naturalAdaptRadius)) {
                    TSpawnedMob spawnedMob = plugin.getMobManager().getSpawnedMob(entity);
                    if (spawnedMob != null) {
                        try {
                            SpawnableMob mob = plugin.getMobManager().getSpwanableMob(spawnedMob.getMob());
                            nearbyMobs.add(mob);
                        } catch (UnknownMobException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            // if there are no mobs nearby we grap a random group and spawn some mobs
            if (!nearbyMobs.isEmpty()) {
                // now we need to filter our all of the groups that are not matching nearby mobs
                for (MobGroup group : new ArrayList<>(virtualGroups)) {
                    boolean inGroup = false;
                    for (SpawnableMob mob : nearbyMobs) {
                        if (group.isInGroup(mob)) {
                            inGroup = true;
                            break;
                        }
                    }
                    if (!inGroup) {
                        virtualGroups.remove(group);
                    }
                }
            }
            // filter out all of our null values
            virtualGroups = virtualGroups.stream().filter(g -> g != null).collect(Collectors.toList());
            event.setCancelled(true);
            if (virtualGroups.isEmpty()) {
                return;
            }
            // okay now we have some groups, lets grap a random one and spawn stuff
            MobGroup mobGroup = virtualGroups.get(MathUtil.RANDOM.nextInt(virtualGroups.size()));
            mobGroup.spawn(event.getLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        if (event.getEntityType() == EntityType.PLAYER || event.getEntity().hasMetadata("NPC")) {
            return;
        }
        TSpawnedMob spawnedMob = plugin.getMobManager().getSpawnedMob(event.getEntity());
        if (spawnedMob != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            // add our custom drops
            CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter(event.getEntity());
            if (character instanceof Mob) {
                // lets call our custom death event
                Optional<RDSTable> lootTable = ((Mob) character).getLootTable();
                if (lootTable.isPresent()) {
                    lootTable.get().getResult().stream()
                            .filter(rdsObject -> rdsObject instanceof Dropable)
                            .forEach(rdsObject -> event.getDrops().add(((Dropable) rdsObject).getItemStack()));
                }
            }
            // track the mob kill if it was killed by a player
            Player killer = event.getEntity().getKiller();
            if (killer != null) {
                TPlayerMobKillLog log = plugin.getDatabase().find(TPlayerMobKillLog.class).where()
                        .eq("uuid", killer.getUniqueId())
                        .eq("mob", spawnedMob.getMob()).findUnique();
                if (log == null) {
                    log = new TPlayerMobKillLog();
                    log.setUuid(killer.getUniqueId());
                    log.setMob(spawnedMob.getMob());
                }
                log.setKillCount(log.getKillCount() + 1);
                plugin.getDatabase().save(log);
            }
            if (character instanceof Mob) RaidCraft.callEvent(new RCMobDeathEvent((Mob) character, spawnedMob, killer));
            // delete the mob group if the last mob dies
            if (spawnedMob.getMobGroupSource() != null && spawnedMob.getMobGroupSource().getSpawnedMobs().size() <= 1) {
                try {
                    MobGroup mobGroup = plugin.getMobManager().getMobGroup(spawnedMob.getMobGroupSource().getMobGroup());
                    RaidCraft.callEvent(new RCMobGroupDeathEvent(spawnedMob.getSourceId(), mobGroup, character));
                    spawnedMob.delete();
                } catch (UnknownMobException e) {
                    e.printStackTrace();
                }
            } else {
                spawnedMob.delete();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {

        EntityDamageEvent lastDamage = event.getEntity().getLastDamageCause();
        if (lastDamage instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) lastDamage).getDamager();
            if (damager instanceof Player) {
                TPlayerPlayerKillLog log = new TPlayerPlayerKillLog();
                log.setKiller(damager.getUniqueId());
                log.setVictim(event.getEntity().getUniqueId());
                log.setTimestamp(Timestamp.from(Instant.now()));
                log.setWorld(event.getEntity().getWorld().getName());
                plugin.getDatabase().save(log);
            } else if (damager instanceof LivingEntity) {
                TSpawnedMob spawnedMob = plugin.getMobManager().getSpawnedMob((LivingEntity) damager);
                if (spawnedMob == null) return;
                TMobPlayerKillLog log = plugin.getDatabase().find(TMobPlayerKillLog.class).where()
                        .eq("uuid", event.getEntity().getUniqueId())
                        .eq("mob", spawnedMob.getMob()).findUnique();
                if (log == null) {
                    log = new TMobPlayerKillLog();
                    log.setUuid(event.getEntity().getUniqueId());
                    log.setMob(spawnedMob.getMob());
                }
                log.setKillCount(log.getKillCount() + 1);
                plugin.getDatabase().save(log);
            }
        }
    }

    // disable fire damage
    //TODO maybe detect damage cause and cancel if it isn't sunlight...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityCombustion(EntityCombustEvent event) {

        if (event.getEntityType() == EntityType.PLAYER || event.getEntity().hasMetadata("NPC")) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity && plugin.getMobManager().isSpawnedMob((LivingEntity) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHorseDamage(EntityDamageEvent event) {

        if (event.getEntityType() != EntityType.HORSE) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkUnloadEvent event) {

        List<TSpawnedMob> unloadedMobs = new ArrayList<>();
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity && plugin.getMobManager().isSpawnedMob((LivingEntity) entity)) {
                unloadedMobs.add(despawnMob((LivingEntity) entity, false));
            }
        }
        if (unloadedMobs.size() > 0) {
            plugin.getDatabase().save(unloadedMobs);
            if (plugin.getConfiguration().debugMobSpawning) {
                plugin.getLogger().info("Unloaded " + unloadedMobs.size()
                        + " mobs in Chunk[" + event.getChunk().getX() + "," + event.getChunk().getZ() + "]");
            }
        }
    }

    private TSpawnedMob despawnMob(LivingEntity entity, boolean saveToDb) {

        TSpawnedMob spawnedMob = plugin.getMobManager().getSpawnedMob(entity);
        if (spawnedMob != null) {
            spawnedMob.setUnloaded(true);
            Location location = entity.getLocation();
            spawnedMob.setChunkX(location.getChunk().getX());
            spawnedMob.setChunkZ(location.getChunk().getZ());
            spawnedMob.setWorld(location.getWorld().getName());
            spawnedMob.setX(location.getBlockX());
            spawnedMob.setY(location.getBlockY());
            spawnedMob.setZ(location.getBlockZ());
            if (saveToDb) plugin.getDatabase().update(spawnedMob);
            if (plugin.getConfiguration().respawnTaskRemoveEntityOnChunkUnload) entity.remove();
        }
        return spawnedMob;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityRemoved(RCEntityRemovedEvent event) {

        if (!plugin.getConfiguration().respawnTaskCleanupRemovedCharacters) return;
        if (event.getEntity() == null) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        TSpawnedMob spawnedMob = plugin.getMobManager().getSpawnedMob((LivingEntity) event.getEntity());
        if (spawnedMob != null) {
            spawnedMob.delete();
            event.getEntity().remove();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {

        List<TSpawnedMob> respawnedMobs = new ArrayList<>();
        if (plugin.getConfiguration().respawnTaskRemoveEntityOnChunkUnload) {
            respawnRemovedMobs(plugin.getMobManager().getSpawnedMobs(event.getChunk()));
        } else {
            for (Entity entity : event.getChunk().getEntities()) {
                if (entity instanceof LivingEntity) {
                    TSpawnedMob spawnedMob = plugin.getMobManager().getSpawnedMob((LivingEntity) entity);
                    if (spawnedMob != null) {
                        try {
                            SpawnableMob spawnableMob = plugin.getMobManager().getSpawnableMob(spawnedMob);
                            if (spawnableMob.respawn(spawnedMob, (LivingEntity) entity, false)) {
                                respawnedMobs.add(spawnedMob);
                            }
                        } catch (UnknownMobException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            plugin.getDatabase().save(respawnedMobs);
            // lets respawn all entities in the chunk that were removed
            List<TSpawnedMob> spawnedMobs = plugin.getMobManager().getSpawnedMobs(event.getChunk());
            spawnedMobs.removeAll(respawnedMobs);
            plugin.getDatabase().save(respawnRemovedMobs(spawnedMobs));
        }
        if (plugin.getConfiguration().debugMobSpawning) {
            plugin.getLogger().info("Respawned " + respawnedMobs.size()
                    + " mobs in Chunk[" + event.getChunk().getX() + "," + event.getChunk().getZ() + "]");
        }
    }

    private Collection<TSpawnedMob> respawnRemovedMobs(Collection<TSpawnedMob> mobs) {

        List<TSpawnedMob> respawnedMobs = new ArrayList<>();
        if (mobs.size() > 0) {
            RespawnTask respawnTask = plugin.getMobManager().getRespawnTask();
            mobs.stream().forEach(mob -> {
                try {
                    // only respawn fixed spawn location mobs and not the random ones...
                    if (mob.getMobGroupSource() == null && mob.getSpawnLocationSource() == null) {
                        mob.delete();
                    } else {
                        SpawnableMob spawnableMob = plugin.getMobManager().getSpawnableMob(mob);
                        if (respawnTask != null) {
                            spawnableMob.respawn(mob, false);
                            respawnedMobs.add(mob);
                        }
                    }
                } catch (UnknownMobException e) {
                    e.printStackTrace();
                }
            });
        }
        return respawnedMobs;
    }
}