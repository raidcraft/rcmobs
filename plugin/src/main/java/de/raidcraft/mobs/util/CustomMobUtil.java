package de.raidcraft.mobs.util;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.api.CustomNmsEntity;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.MobConstants;
import de.raidcraft.mobs.api.NmsEntityManager;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.util.ReflectionUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

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
    public static <T extends Mob> Optional<T> spawnNMSEntity(String customEntityTypeName, Location location, Class<T> mobClass, ConfigurationSection config) {

        if (customEntityTypeName == null || location == null || location.getWorld() == null || mobClass == null) {
            return Optional.empty();
        }
        if (config == null) config = new MemoryConfiguration();

        if (entityManager == null) {
            try {
                Class<?> entityManagerClass = ReflectionUtil.getNmsClass(MobConstants.NMS_PACKAGE, "EntityManager");
                entityManager = (NmsEntityManager) entityManagerClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }

        CustomNmsEntity nmsEntity = RaidCraft.getComponent(MobManager.class).getCustonNmsEntity(location.getWorld(), customEntityTypeName);
        if (nmsEntity != null) {
            entityManager.loadEntity(nmsEntity, config);
            return Optional.ofNullable(RaidCraft.getComponent(CharacterManager.class).wrapCharacter(nmsEntity.spawn(location), mobClass, config));
        }
        return Optional.empty();
    }
}
