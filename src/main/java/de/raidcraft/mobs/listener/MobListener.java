package de.raidcraft.mobs.listener;

import com.comphenix.packetwrapper.Packet3ENamedSoundEffect;
import com.comphenix.protocol.Packets;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import de.raidcraft.RaidCraft;
import de.raidcraft.loot.api.table.LootTableEntry;
import de.raidcraft.mobs.FixedSpawnLocation;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.UnknownMobException;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.SkillsPlugin;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.exceptions.CombatException;
import de.raidcraft.util.EntityUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * @author Silthus
 */
public class MobListener implements Listener {

    private static final int CUSTOM_NAME_INDEX = 10;

    private final MobsPlugin plugin;
    private final CharacterManager characterManager;

    public MobListener(MobsPlugin plugin) {

        this.plugin = plugin;
        plugin.registerEvents(this);
        this.characterManager = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();

        final CharacterManager characterManager = RaidCraft.getComponent(CharacterManager.class);
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, ConnectionSide.SERVER_SIDE,
                        Packets.Server.MOB_SPAWN,
                        Packets.Server.ENTITY_METADATA,
                        Packets.Server.NAMED_SOUND_EFFECT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        PacketContainer packet = event.getPacket();

                        if (event.getPacketID() == Packets.Server.NAMED_SOUND_EFFECT) {
                            // handle the mob hurt effect
                            Packet3ENamedSoundEffect soundEffect = new Packet3ENamedSoundEffect(packet);
                            if (soundEffect.getSoundName().startsWith("mob.skeleton")) {
                                // supress skeleton sounds since they are our custom mobs
                                // TODO: make this much more flexible
                                event.setCancelled(true);
                            }
                        }

                        // You may also want to check event.getPacketID()
                        final Entity entity = packet.getEntityModifier(event.getPlayer().getWorld()).read(0);
                        if (!(entity instanceof LivingEntity) || !entity.hasMetadata("RC_CUSTOM_MOB")) {
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

        if (!(event.getEntity() instanceof LivingEntity)) {
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
    public void onEntitySpawn(CreatureSpawnEvent event) {

        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            // check if there are custom mobs around and stop the spawning of the entity
            if (plugin.getMobManager().getClosestSpawnLocation(event.getLocation(), plugin.getConfiguration().defaultSpawnDenyRadius) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityDeath(EntityDeathEvent event) {

        if (event.getEntityType() == EntityType.PLAYER || event.getEntity().hasMetadata("NPC")) {
            return;
        }
        if (event.getEntity().hasMetadata("RC_CUSTOM_MOB")) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            // add our custom drops
            CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter(event.getEntity());
            if (character instanceof Mob && ((Mob) character).getLootTable() != null) {
                for (LootTableEntry loot : ((Mob) character).getLootTable().loot()) {
                    event.getDrops().add(loot.getItem());
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {

        // kill all out custom mobs in the chunks and reset their spawn timer
        for (Entity entity : event.getChunk().getEntities()) {
            // ignore citizen npcs
            if(entity.hasMetadata("NPC")) {
                return;
            }

            if (entity instanceof LivingEntity && entity.hasMetadata("RC_CUSTOM_MOB")) {
                FixedSpawnLocation spawnLocation = plugin.getMobManager().getClosestSpawnLocation(entity.getLocation(), 10);
                if (spawnLocation.getSpawnedMobCount() > 0) {
                    spawnLocation.setLastSpawn(0);
                }
                entity.remove();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {

        for (Entity entity : event.getChunk().getEntities()) {

            // ignore citizen npcs
            if(entity.hasMetadata("NPC")) {
                return;
            }

            if (entity instanceof LivingEntity) {
                if (entity.hasMetadata("RC_CUSTOM_MOB")) {
                    try {
                        // lets respawn the mob based on its id
                        CharacterTemplate mob = plugin.getMobManager().spawnMob(
                                String.valueOf(entity.getMetadata("RC_MOB_ID").get(0).asString()), entity.getLocation());
                        FixedSpawnLocation spawnLocation = plugin.getMobManager().getClosestSpawnLocation(mob.getEntity().getLocation(), 10);
                        if (spawnLocation != null) {
                            spawnLocation.addSpawnedMob(mob);
                        }
                    } catch (UnknownMobException e) {
                        plugin.getLogger().warning(e.getMessage());
                    }
                }
                entity.remove();
            }
        }
    }
}
