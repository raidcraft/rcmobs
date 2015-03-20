package mobs.actions;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.action.Action;
import mobs.MobManager;
import mobs.SpawnableMob;
import mobs.UnknownMobException;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * @author mdoering
 */
public class MobRemoveAction implements Action<Player> {

    @Override
    public void accept(Player player, ConfigurationSection config) {

        try {
            SpawnableMob mob = RaidCraft.getComponent(MobManager.class).getSpwanableMob(config.getString("mob"));
            if (!config.isSet("id")) {
                RaidCraft.LOGGER.warning("Cannot remove mob " + mob.getId() + " without tracking id!");
                player.sendMessage(ChatColor.RED + "Cannot remove mob " + mob.getId() + " without tracking id!");
                return;
            }
            mob.remove(config.getString("id"));
        } catch (UnknownMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
            player.sendMessage(ChatColor.RED + "Error spawning mob group with action! " + e.getMessage());
        }
    }
}
