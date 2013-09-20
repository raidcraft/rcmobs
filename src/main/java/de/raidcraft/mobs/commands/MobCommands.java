package de.raidcraft.mobs.commands;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.UnknownMobException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Silthus
 */
public class MobCommands {

    private final MobsPlugin plugin;

    public MobCommands(MobsPlugin plugin) {

        this.plugin = plugin;
    }

    @Command(
            aliases = {"reload"},
            desc = "Reloads the Mob Plugin"
    )
    public void reload(CommandContext args, CommandSender sender) {

        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "Alle Einstellungen des " + plugin.getName() + " Plugins wurden neu geladen.");
    }

    @Command(
            aliases = {"spawn"},
            desc = "Spawns a custom mob",
            min = 1
    )
    public void spawn(CommandContext args, CommandSender sender) throws CommandException {

        try {
            String mobName = args.getJoinedStrings(0);
            plugin.getMobManager().spawnMob(mobName, ((Player) sender).getLocation());
        } catch (UnknownMobException e) {
            throw new CommandException(e);
        }
    }
}
