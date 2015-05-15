package de.raidcraft.mobs.api;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Silthus
 */
public interface CustomNmsEntity {

    UUID getUniqueID();

    void setDeathSound(String sound);

    void setHurtSound(String sound);

    default void load(ConfigurationSection config) {}

    LivingEntity spawn(Location location);

    void setWrappedEntity(Mob mob);

    Optional<Mob> getWrappedEntity();
}
