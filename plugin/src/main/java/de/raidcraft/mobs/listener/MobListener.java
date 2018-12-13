package de.raidcraft.mobs.listener;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.requirement.tables.TTag;
import de.raidcraft.api.events.RCEntityRemovedEvent;
import de.raidcraft.api.random.Dropable;
import de.raidcraft.api.random.RDSTable;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.RespawnTask;
import de.raidcraft.mobs.SpawnableMob;
import de.raidcraft.mobs.UnknownMobException;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.SpawnMobException;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
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
    private CharacterManager characterManager;

    public MobListener(MobsPlugin plugin) {

        this.plugin = plugin;
    }

    private CharacterManager getCharacterManager() {
        if (characterManager == null) {
            this.characterManager = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();
        }
        return this.characterManager;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {

        if (!(event.getEntity() instanceof LivingEntity) || !plugin.getMobManager().isSpawnedMob((LivingEntity) event.getEntity())) {
            return;
        }
        if (getCharacterManager() == null) return;

        CharacterTemplate character = getCharacterManager().getCharacter((LivingEntity) event.getEntity());
        if (!(character instanceof Mob)) {
            return;
        }
        try {
            Mob attacker = (Mob) character;
            if (attacker.isPassive()) {
                if (!attacker.isInCombat()) {
                    event.setCancelled(true);
                    return;
                }
            }
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
        boolean debug = plugin.getConfiguration().debugVanillaSpawning;
        if (plugin.getConfiguration().getIgnoredEntityTypes().contains(event.getEntityType())) {
            if (debug) plugin.getLogger().info("Spawned Vanilla Mob for " + event.getEntity().getName() + " because the entity type " + event.getEntityType() + " is ignored.");
            return;
        }
        if (plugin.getConfiguration().getIgnoredSpawnReasons().contains(event.getSpawnReason())) {
            if (debug) plugin.getLogger().info("Spawned Vanilla Mob for " + event.getEntity().getName() + " because the spawn reason " + event.getSpawnReason() + " is ignored.");
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
        if (!plugin.getConfiguration().getReplacedMobs().contains(event.getEntityType().name())) {
            if (debug) plugin.getLogger().info("Spawned Vanilla Mob for " + event.getEntity().getName() + " because the entity type " + event.getEntityType() + " is not on the replacement list");
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
            try {
                // okay now we have some groups, lets grap a random one and spawn stuff
                MobGroup mobGroup = virtualGroups.get(MathUtil.RANDOM.nextInt(virtualGroups.size()));
                if (debug)
                    plugin.getLogger().info("Spawning mob group " + mobGroup.getName() + " at " + event.getLocation());
                mobGroup.spawn(event.getLocation());
            } catch (SpawnMobException e) {
                plugin.getLogger().warning(e.getMessage());
            }
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
            CharacterTemplate character = getCharacterManager().getCharacter(event.getEntity());
            if (character instanceof Mob) {
                // lets call our custom death event
                List<RDSTable> lootTables = ((Mob) character).getLootTables();
                lootTables.stream().flatMap(rdsTable -> rdsTable.loot(event.getEntity().getKiller()).stream())
                        .filter(rdsObject -> rdsObject instanceof Dropable)
                        .forEach(rdsObject -> event.getDrops().add(((Dropable) rdsObject).getItemStack()));
            }

            Optional<CharacterTemplate> killer = character.getKiller();
            // track the mob kill if it was killed by a player
            killer.ifPresent(characterTemplate -> {
                TPlayerMobKillLog log = plugin.getRcDatabase().find(TPlayerMobKillLog.class).where()
                        .eq("uuid", characterTemplate.getUniqueId())
                        .eq("mob", spawnedMob.getMob()).findOne();
                if (log == null) {
                    log = new TPlayerMobKillLog();
                    log.setUuid(characterTemplate.getUniqueId());
                    log.setMob(spawnedMob.getMob());
                }
                log.setKillCount(log.getKillCount() + 1);
                plugin.getRcDatabase().save(log);
            });
            if (character instanceof Mob) {
                RaidCraft.callEvent(new RCMobDeathEvent((Mob) character, spawnedMob, killer.orElse(null)));
            }
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
                plugin.getRcDatabase().save(log);
            } else if (damager instanceof LivingEntity) {
                TSpawnedMob spawnedMob = plugin.getMobManager().getSpawnedMob((LivingEntity) damager);
                if (spawnedMob == null) return;
                TMobPlayerKillLog log = plugin.getRcDatabase().find(TMobPlayerKillLog.class).where()
                        .eq("uuid", event.getEntity().getUniqueId())
                        .eq("mob", spawnedMob.getMob()).findOne();
                if (log == null) {
                    log = new TMobPlayerKillLog();
                    log.setUuid(event.getEntity().getUniqueId());
                    log.setMob(spawnedMob.getMob());
                }
                log.setKillCount(log.getKillCount() + 1);
                plugin.getRcDatabase().save(log);
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
                plugin.getMobManager().despawnMob((LivingEntity) entity, false).ifPresent(unloadedMobs::add);
            }
        }
        if (unloadedMobs.size() > 0) {
            plugin.getRcDatabase().saveAll(unloadedMobs);
            if (plugin.getConfiguration().debugMobSpawning && unloadedMobs.size() > 0) {
                plugin.getLogger().info("Unloaded " + unloadedMobs.size()
                        + " mobs in Chunk[" + event.getChunk().getX() + "," + event.getChunk().getZ() + "]");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityRemoved(RCEntityRemovedEvent event) {

        if (!plugin.getConfiguration().respawnTaskCleanupRemovedCharacters) return;
        if (event.getEntity() == null) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        TSpawnedMob spawnedMob = plugin.getMobManager().getSpawnedMob((LivingEntity) event.getEntity());
        event.getEntity().remove();
        if (spawnedMob != null && !spawnedMob.isUnloaded()) {
            spawnedMob.delete();
            if (plugin.getConfiguration().debugMobSpawning) {
                plugin.getLogger().info("Deleted " + spawnedMob.getMob() + " (ID:" + spawnedMob.getId() + ") from db because it was removed.");
            }
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
            plugin.getRcDatabase().saveAll(respawnedMobs);
            // lets respawn all entities in the chunk that were removed
            List<TSpawnedMob> spawnedMobs = plugin.getMobManager().getSpawnedMobs(event.getChunk());
            spawnedMobs.removeAll(respawnedMobs);
            respawnRemovedMobs(spawnedMobs);
        }
        if (plugin.getConfiguration().debugMobSpawning && respawnedMobs.size() > 0) {
            plugin.getLogger().info("Respawned " + respawnedMobs.size()
                    + " mobs in Chunk[" + event.getChunk().getX() + "," + event.getChunk().getZ() + "]");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMobDeath(RCMobDeathEvent event) {

        TTag.findOrCreateTag("mob-kill:" + event.getMob().getId(), "Mob KILL: " + event.getMob().getName() + " (" + event.getMob().getId() + ")", true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMobGroupDeath(RCMobGroupDeathEvent event) {
        TTag.findOrCreateTag("mob-group-kill:" + event.getMobGroup().getName(), "Mob KILL: " + event.getMobGroup().getName(), true);
    }

    /**
     * Respawns the collection of mobs and batch saves their spawn state to the database.
     *
     * @param mobs to respawn
     */
    private void respawnRemovedMobs(Collection<TSpawnedMob> mobs) {

        List<TSpawnedMob> respawnedMobs = new ArrayList<>();
        if (mobs.size() > 0) {
            RespawnTask respawnTask = plugin.getMobManager().getRespawnTask();
            mobs.forEach(mob -> {
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
        plugin.getRcDatabase().updateAll(respawnedMobs);
    }
}