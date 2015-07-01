package de.raidcraft.mobs.actions;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.action.Action;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.UnknownMobException;
import de.raidcraft.mobs.api.MobGroup;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * @author mdoering
 */
public class GroupSpawnAction implements Action<Player> {

    @Override
    @Information(
            value = "group.spawn",
            desc = "Spawns the given custom mob group at the defined location.",
            conf = {"group", "x", "y", "z", "world", "id"}
    )
    public void accept(Player player, ConfigurationSection config) {

        try {
            MobGroup group = RaidCraft.getComponent(MobManager.class).getMobGroup(config.getString("group"));
            Location location = new Location(
                    config.isSet("world") ? Bukkit.getWorld(config.getString("world")) : player.getWorld(),
                    config.getDouble("x"),
                    config.getDouble("y"),
                    config.getDouble("z")
            );
            if (config.isSet("id")) {
                group.spawn(config.getString("id"), location);
            } else {
                group.spawn(location);
            }
        } catch (UnknownMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
            player.sendMessage(ChatColor.RED + "Error spawning mob group with action! " + e.getMessage());
        }
    }
}
