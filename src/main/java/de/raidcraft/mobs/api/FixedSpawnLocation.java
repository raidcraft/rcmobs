package de.raidcraft.mobs.api;

import de.raidcraft.RaidCraft;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.skills.api.character.CharacterType;
import de.raidcraft.util.LocationUtil;
import de.raidcraft.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * @author Silthus
 */
public abstract class FixedSpawnLocation implements Spawnable {

    private final Location location;
    private final long cooldown;
    private long lastSpawn;

    protected FixedSpawnLocation(Location location, double cooldown) {

        this.location = location;
        this.cooldown = TimeUtil.secondsToMillis(cooldown);
    }

    protected abstract int getSpawnRadius();

    public void spawn() {

        // dont spawn stuff if it is still on cooldown
        if (System.currentTimeMillis() < lastSpawn + cooldown) {
            return;
        }
        // dont spawn entities if there are other entities around in the radius
        Entity[] nearbyEntities = LocationUtil.getNearbyEntities(location, getSpawnRadius());
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity) {
                CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter((LivingEntity) entity);
                if (character.getCharacterType() == CharacterType.CUSTOM_MOB) {
                    return;
                }
            }
        }
        // spawn the mob
        spawn(location);
        lastSpawn = System.currentTimeMillis();
    }
}
