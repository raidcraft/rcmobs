package de.raidcraft.mobs.actions;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.action.Action;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.SpawnableMob;
import de.raidcraft.mobs.UnknownMobException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * @author mdoering
 */
public class MobSpawnAction implements Action<Player> {

    @Override
    @Information(
            value = "mob.spawn",
            desc = "Spawns the given custom mob at the defined location.",
            conf = {"world", "x", "y", "z"}
    )
    public void accept(Player player, ConfigurationSection config) {

        try {
            SpawnableMob mob = RaidCraft.getComponent(MobManager.class).getSpwanableMob(config.getString("mob"));
            Location location = new Location(
                    config.isSet("world") ? Bukkit.getWorld(config.getString("world")) : player.getWorld(),
                    config.getDouble("x"),
                    config.getDouble("y"),
                    config.getDouble("z")
            );
            if (config.isSet("id")) {
                mob.spawn(config.getString("id"), location);
            } else {
                mob.spawn(location);
            }
        } catch (UnknownMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
            player.sendMessage(ChatColor.RED + "Error spawning mob with action! " + e.getMessage());
        }
    }
}
