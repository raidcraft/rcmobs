package de.raidcraft.mobs.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.Packets;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.events.RCEntityRemovedEvent;
import de.raidcraft.api.random.Dropable;
import de.raidcraft.api.random.RDSTable;
import de.raidcraft.mobs.MobManager;
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
import de.raidcraft.util.EntityUtil;
import de.raidcraft.util.MathUtil;
import org.bukkit.ChatColor;
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

    private static final int CUSTOM_NAME_INDEX = 10;

    private final MobsPlugin plugin;
    private final CharacterManager characterManager;

    public MobListener(MobsPlugin plugin) {

        this.plugin = plugin;
        this.characterManager = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();

        final CharacterManager characterManager = RaidCraft.getComponent(CharacterManager.class);
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin,
                        PacketType.Play.Server.NAMED_SOUND_EFFECT,
                        PacketType.Play.Server.SPAWN_ENTITY,
                        PacketType.Play.Server.ENTITY_METADATA) {
                    @Override
                    public void onPacketSending(PacketEvent event) {

                        PacketContainer packet = event.getPacket();

                        // handle the custom mob hurt effects
                        /*
                        if (event.getPacketType() == PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                            WrapperPlayServerNamedSoundEffect soundEffect = new WrapperPlayServerNamedSoundEffect(event.getPacket());
                            if (false) {
                                // supress skeleton sounds since they are our custom mobs
                                event.setCancelled(true);
                            }
                        }*/

                        // You may also want to check event.getPacketID()
                        final Entity entity = packet.getEntityModifier(event.getPlayer().getWorld()).read(0);
                        if (!(entity instanceof LivingEntity)
                                || !RaidCraft.getComponent(MobManager.class).isSpawnedMob((LivingEntity) entity)) {
                            return;
                        }
                        CharacterTemplate character = characterManager.getCharacter((LivingEntity) entity);
                        ChatColor mobColor = EntityUtil.getConColor(
                                characterManager.getHero(event.getPlayer()).getPlayerLevel(),
                                character.getAttachedLevel().getLevel());
                        String name;
                        if (character.isInCombat()) {
                            name = EntityUtil.drawHealthBar(
                                    character.getHealth(),
                                    character.getMaxHealth(),
                                    mobColor,
                                    entity.hasMetadata("ELITE"),
                                    entity.hasMetadata("RARE"));
                        } else {
                            name = EntityUtil.drawMobName(
                                    character.getName(),
                                    character.getAttachedLevel().getLevel(),
                                    mobColor,
                                    entity.hasMetadata("ELITE"),
                                    entity.hasMetadata("RARE"));
                        }

                        if (name != null) {
                            // Clone the packet!
                            event.setPacket(packet = packet.deepClone());

                            // This comes down to a difference in what the packets store in memory
                            if (event.getPacketID() == Packets.Server.ENTITY_METADATA) {
                                WrappedDataWatcher watcher = new WrappedDataWatcher(packet.getWatchableCollectionModifier().read(0));

                                processDataWatcher(watcher, name);
                                packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
                            } else {
                                processDataWatcher(packet.getDataWatcherModifier().read(0), name);
                            }
                        }
                    }
                });
    }

    private void processDataWatcher(WrappedDataWatcher watcher, String name) {
        // If it's being updated, change it!
        if (watcher.getObject(CUSTOM_NAME_INDEX) != null) {
            watcher.setObject(CUSTOM_NAME_INDEX, name);
        }
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

    // TODO: finish checkPathfinding
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void checkPathfinding(EntityDamageByEntityEvent event) {

        /*
        if (!event.getEntity().hasMetadata("RC_CUSTOM_MOB")
                || !(event.getEntity() instanceof LivingEntity)
                || !(event.getDamager() instanceof LivingEntity)) {
            return;
        }
        CharacterTemplate mob = characterManager.getCharacter((LivingEntity) event.getEntity());
        CharacterTemplate attacker = characterManager.getCharacter((LivingEntity) event.getDamager());
        // TODO: debug this shit
        // lets check if our entity can reach the attacker
        try {
            AStar aStar = new AStar(mob.getEntity().getLocation(), attacker.getEntity().getLocation(), 50);
            aStar.iterate();
            if (aStar.getPathingResult() == PathingResult.NO_PATH) {
                mob.reset();
            } else {
                // lets make him walk there
            }
        } catch (AStar.InvalidPathException e) {
            mob.reset();
        }*/
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntitySpawn(CreatureSpawnEvent event) {

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        if (event.getEntity().getType() == EntityType.HORSE && plugin.getConfiguration().denyHorseSpawning) {
            event.setCancelled(true);
            return;
        }
        if (!plugin.getConfiguration().getReplacedMobs().contains(event.getEntity().getType().name())) {
            return;
        }
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL
                || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE) {
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
                unloadedMobs.add(despawnMob((LivingEntity) entity));
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

    private TSpawnedMob despawnMob(LivingEntity entity) {

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
            if (plugin.getConfiguration().respawnTaskRemoveEntityOnChunkUnload) entity.remove();
        }
        return spawnedMob;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityRemoved(RCEntityRemovedEvent event) {

        if (event.getEntity() == null) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!plugin.getMobManager().isSpawnedMob((LivingEntity) event.getEntity())) return;
        plugin.getDatabase().save(despawnMob((LivingEntity) event.getEntity()));
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