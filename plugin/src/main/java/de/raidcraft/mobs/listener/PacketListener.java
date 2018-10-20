package de.raidcraft.mobs.listener;

import com.comphenix.packetwrapper.AbstractPacket;
import com.comphenix.packetwrapper.WrapperPlayServerEntityMetadata;
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
import de.raidcraft.skills.api.hero.Hero;
import de.raidcraft.util.EntityMetaData;
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

    public PacketListener(MobsPlugin plugin) {

        super(plugin, PacketType.Play.Server.SPAWN_ENTITY_LIVING, PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.NAMED_ENTITY_SPAWN);
    }

    @Override
    public void onPacketSending(PacketEvent event) {

        AbstractPacket wrapper = null;
        Entity mobEntity = null;
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            wrapper = new WrapperPlayServerEntityMetadata(event.getPacket());
            mobEntity = ((WrapperPlayServerEntityMetadata) wrapper).getEntity(event);
        } else if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY_LIVING) {
            wrapper = new WrapperPlayServerSpawnEntityLiving(event.getPacket());
            mobEntity = ((WrapperPlayServerSpawnEntityLiving) wrapper).getEntity(event);
        } else if (event.getPacketType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            wrapper = new WrapperPlayServerNamedEntitySpawn(event.getPacket());
            mobEntity = ((WrapperPlayServerNamedEntitySpawn) wrapper).getEntity(event);
        }

        if (!(mobEntity instanceof LivingEntity) || !RaidCraft.getComponent(MobManager.class).isSpawnedMob((LivingEntity) mobEntity)) {
            return;
        }

        CharacterManager characterManager = RaidCraft.getComponent(CharacterManager.class);

        final CharacterTemplate character = characterManager.getCharacter((LivingEntity) mobEntity);
        final Hero hero = characterManager.getHero(event.getPlayer());

        if (hero == null || character == null) return;

        WrappedDataWatcher meta = WrappedDataWatcher.getEntityWatcher(mobEntity);
        meta.setObject(ALWAYS_SHOW_INDEX, true);
        String mobName;
        if (character.isInCombat()) {
            mobName = EntityUtil.drawHealthBar(character.getHealth(), character.getMaxHealth(),
                    EntityUtil.getConColor(hero.getPlayerLevel(), character.getAttachedLevel().getLevel()),
                    mobEntity.hasMetadata(EntityMetaData.RCMOBS_ELITE),
                    mobEntity.hasMetadata(EntityMetaData.RCMOBS_RARE));
        } else {
            mobName = EntityUtil.drawMobName(character.getName(),
                    character.getAttachedLevel().getLevel(),
                    hero.getPlayerLevel(),
                    mobEntity.hasMetadata(EntityMetaData.RCMOBS_ELITE),
                    mobEntity.hasMetadata(EntityMetaData.RCMOBS_RARE));
        }
        meta.setObject(CUSTOM_NAME_INDEX, mobName);

        if (wrapper instanceof WrapperPlayServerSpawnEntityLiving) {
            ((WrapperPlayServerSpawnEntityLiving) wrapper).setMetadata(meta);
        } else if (wrapper instanceof WrapperPlayServerEntityMetadata) {
            PacketContainer packet = event.getPacket().deepClone();
            WrappedDataWatcher watcher = new WrappedDataWatcher(packet.getWatchableCollectionModifier().read(0));
            processDataWatcher(watcher, mobName);
            packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
        } else {
            ((WrapperPlayServerNamedEntitySpawn) wrapper).setMetadata(meta);
        }
        event.setPacket(wrapper.getHandle());
    }

    private void processDataWatcher(WrappedDataWatcher watcher, String name) {
        // If it's being updated, change it!
        if (watcher.getObject(CUSTOM_NAME_INDEX) != null) {
            watcher.setObject(CUSTOM_NAME_INDEX, name, true);
        }
        if (watcher.getObject(ALWAYS_SHOW_INDEX) != null) {
            watcher.setObject(ALWAYS_SHOW_INDEX, true);
        }
    }
}
