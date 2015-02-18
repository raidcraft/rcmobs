package de.raidcraft.mobs.quests;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.action.action.Action;
import de.raidcraft.mobs.MobManager;
import de.raidcraft.mobs.UnknownMobException;
import de.raidcraft.mobs.api.MobGroup;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * @author mdoering
 */
public class GroupRemoveAction implements Action<Player> {

    @Override
    public void accept(Player player, ConfigurationSection config) {

        try {
            MobGroup group = RaidCraft.getComponent(MobManager.class).getMobGroup(config.getString("group"));
            if (!config.isSet("id")) {
                RaidCraft.LOGGER.warning("Cannot remove mob group " + group.getName() + " without tracking id!");
                player.sendMessage(ChatColor.RED + "Cannot remove mob group " + group.getName() + " without tracking id!");
                return;
            }
            group.remove(config.getString("id"));
        } catch (UnknownMobException e) {
            RaidCraft.LOGGER.warning(e.getMessage());
            player.sendMessage(ChatColor.RED + "Error spawning mob group with action! " + e.getMessage());
        }
    }
}