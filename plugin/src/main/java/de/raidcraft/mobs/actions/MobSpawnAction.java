package de.raidcraft.mobs.actions;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.action.Action;
import de.raidcraft.api.locations.Locations;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.SpawnableMob;
import de.raidcraft.mobs.UnknownMobException;
import org.bukkit.ChatColor;
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
            conf = {"mob", "x", "y", "z", "world", "id", "amount"}
    )
    public void accept(Player player, ConfigurationSection config) {

        try {
            SpawnableMob mob = RaidCraft.getComponent(MobManager.class).getSpwanableMob(config.getString("mob"));
            Locations.fromConfig(config).ifPresent(location -> {
                for (int i = 0; i < config.getInt("amount", 1); i++) {
                    if (config.isSet("id")) {
                        mob.spawn(config.getString("id"), location.getLocation());
                    } else {
                        mob.spawn(location.getLocation());
                    }
                }
            });
        } catch (UnknownMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
            player.sendMessage(ChatColor.RED + "Error spawning mob with action! " + e.getMessage());
        }
    }
}
