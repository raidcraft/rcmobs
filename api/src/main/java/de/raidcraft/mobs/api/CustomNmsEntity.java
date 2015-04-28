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

    public UUID getUniqueID();

    public void load(ConfigurationSection config);

    public LivingEntity spawn(Location location);

    public void setWrappedEntity(Mob mob);

    public Optional<Mob> getWrappedEntity();
}
