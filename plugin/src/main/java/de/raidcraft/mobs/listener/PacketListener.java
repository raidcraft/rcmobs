package de.raidcraft.mobs.listener;

import com.comphenix.packetwrapper.WrapperPlayServerNamedEntitySpawn;
import com.comphenix.packetwrapper.WrapperPlayServerSpawnEntityLiving;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.EntityUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * @author Silthus
 */
public class PacketListener extends PacketAdapter {

    private static final int CUSTOM_NAME_INDEX = 2;
    private static final int ALWAYS_SHOW_INDEX = 3;

    private final CharacterManager characterManager;

    public PacketListener(MobsPlugin plugin) {

        super(plugin, //PacketType.Play.Server.SPAWN_ENTITY_LIVING,
                //PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.ENTITY_METADATA);
        this.characterManager = RaidCraft.getComponent(CharacterManager.class);
    }

    @Override
    public void onPacketSending(PacketEvent event) {

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
        final Entity armorStand = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).read(0);
        if (!(armorStand instanceof ArmorStand) || !armorStand.isInsideVehicle()) {
            return;
        }
        Entity spawnedMob = armorStand.getVehicle();
        if (!(spawnedMob instanceof LivingEntity) || !RaidCraft.getComponent(MobManager.class).isSpawnedMob((LivingEntity) spawnedMob)) {
            return;
        }
        CharacterTemplate character = characterManager.getCharacter((LivingEntity) spawnedMob);
        ChatColor mobColor = EntityUtil.getConColor(
                characterManager.getHero(event.getPlayer()).getPlayerLevel(),
                character.getAttachedLevel().getLevel());
        String healthBar;
        if (character.isInCombat()) {
            healthBar = EntityUtil.drawHealthBar(
                    character.getHealth(),
                    character.getMaxHealth(),
                    mobColor,
                    spawnedMob.hasMetadata("ELITE"),
                    spawnedMob.hasMetadata("RARE"));
        } else {
            healthBar = EntityUtil.drawMobName(
                    character.getName(),
                    character.getAttachedLevel().getLevel(),
                    mobColor,
                    spawnedMob.hasMetadata("ELITE"),
                    spawnedMob.hasMetadata("RARE"));
        }

        WrappedDataWatcher meta = WrappedDataWatcher.getEntityWatcher(armorStand);
        meta.setObject(CUSTOM_NAME_INDEX, healthBar);
        meta.setObject(ALWAYS_SHOW_INDEX, (byte) 1);

        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            WrapperPlayServerSpawnEntityLiving wrapper = new WrapperPlayServerSpawnEntityLiving(event.getPacket());
            wrapper.setMetadata(meta);
            event.setPacket(wrapper.getHandle());
        } else if (event.getPacketType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            WrapperPlayServerNamedEntitySpawn wrapper = new WrapperPlayServerNamedEntitySpawn(event.getPacket());
            wrapper.setMetadata(meta);
            event.setPacket(wrapper.getHandle());
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            PacketContainer packet = event.getPacket().deepClone();
            WrappedDataWatcher watcher = new WrappedDataWatcher(packet.getWatchableCollectionModifier().read(0));
            processDataWatcher(watcher, healthBar);
            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
            event.setPacket(packet);
        }
    }

    private void processDataWatcher(WrappedDataWatcher watcher, String name) {
        // If it's being updated, change it!
        if (watcher.getObject(CUSTOM_NAME_INDEX) != null) {
            watcher.setObject(CUSTOM_NAME_INDEX, name, true);
        }
        if (watcher.getObject(ALWAYS_SHOW_INDEX) != null) {
            watcher.setObject(ALWAYS_SHOW_INDEX, (byte) 1);
        }
    }
}
