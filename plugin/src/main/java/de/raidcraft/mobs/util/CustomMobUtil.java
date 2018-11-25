package de.raidcraft.mobs.util;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.*;
import de.raidcraft.mobs.creatures.YamlMobConfig;
import de.raidcraft.nms.api.EntityRegistry;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.util.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;

/**
 * @author Silthus
 */
public class CustomMobUtil {

    private static NmsEntityManager entityManager;

    public static double getMaxHealth(int level) {

        return 1.5126 * (level ^ 2) + 15.946 * level + 80;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Mob> Optional<T> spawnNMSEntity(String customEntityTypeName, Location location, Class<T> mobClass, MobConfig config) {

        if (customEntityTypeName == null || location == null || location.getWorld() == null || mobClass == null) {
            return Optional.empty();
        }
        if (config == null) config = new YamlMobConfig(new MemoryConfiguration());

        if (entityManager == null) {
            try {
                Class<?> entityManagerClass = ReflectionUtil.getNmsClass(MobConstants.NMS_PACKAGE, "EntityManager");
                entityManager = (NmsEntityManager) entityManagerClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }

        Optional<Entity> entity = RaidCraft.getComponent(EntityRegistry.class).getEntity(customEntityTypeName, location.getWorld());
        if (entity.isPresent() && entity.get() instanceof CustomNmsEntity && entity.get() instanceof LivingEntity) {
            entityManager.loadEntity((CustomNmsEntity) entity.get(), config);
            ((CustomNmsEntity) entity.get()).spawn(location);
            return Optional.ofNullable(RaidCraft.getComponent(CharacterManager.class).wrapCharacter((LivingEntity) entity.get(), mobClass, config));
        }
        return Optional.empty();
    }

    public static ArmorStand createArmorStand(Mob mob) {

        ArmorStand armorStand = mob.getEntity().getLocation().getWorld().spawn(mob.getEntity().getLocation(), ArmorStand.class);
        armorStand.setCustomNameVisible(true);
        armorStand.setVisible(false);
        armorStand.setCanPickupItems(false);
        armorStand.setSmall(true);
        armorStand.setMaxHealth(mob.getMaxHealth());
        return armorStand;
    }
}
