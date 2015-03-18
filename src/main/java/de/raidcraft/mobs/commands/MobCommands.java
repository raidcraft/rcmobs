package de.raidcraft.mobs.commands;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import de.raidcraft.mobs.MobGroupSpawnLocation;
import de.raidcraft.mobs.MobSpawnLocation;
import de.raidcraft.mobs.MobsPlugin;
import de.raidcraft.mobs.SpawnableMob;
import de.raidcraft.mobs.UnknownMobException;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.tables.TMobGroupSpawnLocation;
import de.raidcraft.mobs.tables.TMobSpawnLocation;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
    @CommandPermissions("rcmobs.reload")
    public void reload(CommandContext args, CommandSender sender) {

        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "Alle Einstellungen des " + plugin.getName() + " Plugins wurden neu geladen.");
    }

    @Command(
            aliases = {"spawn"},
            desc = "Spawns a custom mob",
            min = 1
    )
    @CommandPermissions("rcmobs.spawn")
    public void spawn(CommandContext args, CommandSender sender) throws CommandException {

        try {
            String mobName = args.getJoinedStrings(0);
            plugin.getMobManager().spawnMob(mobName, ((Player) sender).getLocation());
        } catch (UnknownMobException e) {
            throw new CommandException(e);
        }
    }

    @Command(
            aliases = {"spawngroup", "sg"},
            desc = "Spawn a mob group",
            min = 1
    )
    @CommandPermissions("rcmobs.spawngroup")
    public void spawnGroup(CommandContext args, CommandSender sender) throws CommandException {

        try {
            MobGroup group = plugin.getMobManager().getMobGroup(args.getString(0));
            group.spawn(((Player) sender).getLocation());
        } catch (UnknownMobException e) {
            throw new CommandException(e);
        }
    }

    @Command(
            aliases = {"setmobspawn", "sm", "setmob", "set"},
            desc = "Sets the spawnpoint of a mob",
            min = 2,
            max = 2,
            usage = "<mob> <cooldown>"
    )
    @CommandPermissions("rcmobs.setmobspawn")
    public void setMobSpawn(CommandContext args, CommandSender sender) throws CommandException {

        try {
            SpawnableMob mob = plugin.getMobManager().getSpwanableMob(args.getString(0));
            Location location = ((Player) sender).getLocation();
            TMobSpawnLocation spawn = new TMobSpawnLocation();
            spawn.setMob(mob.getId());
            spawn.setX(location.getBlockX());
            spawn.setY(location.getBlockY());
            spawn.setZ(location.getBlockZ());
            spawn.setWorld(location.getWorld().getName());
            spawn.setCooldown(args.getDouble(1));
            plugin.getDatabase().save(spawn);
            MobSpawnLocation mobSpawnLocation = plugin.getMobManager().addSpawnLocation(spawn);
            mobSpawnLocation.spawn(false);
            sender.sendMessage(ChatColor.GREEN + "Mob Spawn Location von " + mob.getMobName() + " wurde gesetzt.");
        } catch (UnknownMobException e) {
            throw new CommandException(e.getMessage());
        }
    }

    @Command(
            aliases = {"setmobgroup", "smg", "setgroup", "setg"},
            desc = "Sets the spawnpoint of a mob group",
            min = 2,
            max = 2,
            usage = "<mobgroup> <cooldown>"
    )
    @CommandPermissions("rcmobs.setgroupspawn")
    public void setMobSpawnGroup(CommandContext args, CommandSender sender) throws CommandException {

        try {
            MobGroup mobGroup = plugin.getMobManager().getMobGroup(args.getString(0));
            Location location = ((Player) sender).getLocation();
            TMobGroupSpawnLocation spawn = new TMobGroupSpawnLocation();
            spawn.setSpawnGroup(mobGroup.getName());
            spawn.setX(location.getBlockX());
            spawn.setY(location.getBlockY());
            spawn.setZ(location.getBlockZ());
            spawn.setWorld(location.getWorld().getName());
            spawn.setCooldown(args.getDouble(1));
            plugin.getDatabase().save(spawn);
            sender.sendMessage(ChatColor.GREEN + "Mob Spawn Location für die Mob Gruppe " + mobGroup.getName() + " wurde gesetzt.");
            MobGroupSpawnLocation spawnLocation = plugin.getMobManager().addSpawnLocation(spawn);
            spawnLocation.spawn(false);
        } catch (UnknownMobException e) {
            throw new CommandException(e.getMessage());
        }
    }

    @Command(
            aliases = {"deletespawn", "ds", "removespawn", "rs", "delete", "remove"},
            desc = "Removes the nearest spawnpoint",
            flags = "r:"
    )
    @CommandPermissions("rcmobs.deletespawn")
    public void deleteSpawnPoint(CommandContext args, CommandSender sender) throws CommandException {

        int radius = args.getFlagInteger('r', 30);
        MobSpawnLocation spawn = plugin.getMobManager().getClosestMobSpawnLocation(((Player) sender).getLocation(), radius);
        if (spawn == null) {
            throw new CommandException("Keinen Spawnpunkt im Radius von " + radius + " Metern gefunden.");
        }
        TMobSpawnLocation mobSpawn = plugin.getDatabase().find(TMobSpawnLocation.class).where()
                .eq("x", spawn.getLocation().getBlockX())
                .eq("y", spawn.getLocation().getBlockY())
                .eq("z", spawn.getLocation().getBlockZ()).findUnique();
        if (mobSpawn != null) {
            plugin.getDatabase().delete(mobSpawn);
            plugin.getMobManager().removeSpawnLocation(spawn);
            sender.sendMessage(ChatColor.GREEN + "Spawnpunkt wurde gelöscht!");
            return;
        }
        TMobGroupSpawnLocation mobGroup = plugin.getDatabase().find(TMobGroupSpawnLocation.class).where()
                .eq("x", spawn.getLocation().getBlockX())
                .eq("y", spawn.getLocation().getBlockY())
                .eq("z", spawn.getLocation().getBlockZ()).findUnique();
        if (mobGroup != null) {
            plugin.getDatabase().delete(mobGroup);
            plugin.getMobManager().removeSpawnLocation(spawn);
            sender.sendMessage(ChatColor.GREEN + "Spawnpunkt wurde gelöscht!");
            return;
        }
        throw new CommandException("Keine Einträge in der Datenbank gefunden!");
    }

    @Command(
            aliases = {"debug"},
            desc = "show all mob spawn locations",
            flags = "r:"
    )
    @CommandPermissions("rcmobs.deletespawn")
    public void debug(CommandContext args, CommandSender sender) throws CommandException {

        if (!(sender instanceof Player)) {
            return;
        }
        Player player = (Player) sender;
        String world = player.getWorld().getName().toLowerCase();
        plugin.getDatabase().find(TMobGroupSpawnLocation.class).findList()
                .forEach(loc -> {
                    try {
                        if (loc.getWorld().toLowerCase().equals(world)) {
                            fakeBeacon(player, loc.getBukkitLocation());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        plugin.getDatabase().find(TMobSpawnLocation.class).findList()
                .forEach(loc -> {
                    try {
                        if (loc.getWorld().toLowerCase().equals(world)) {
                            fakeBeacon(player, loc.getBukkitLocation());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void fakeBeacon(Player player, Location location) {

        // TODO: to it better
        Location a1 = location.clone().add(-1, -1, 1);
        Location a2 = location.clone().add(-1, -1, 0);
        Location a3 = location.clone().add(-1, -1, -1);
        Location b1 = location.clone().add(0, -1, 1);
        Location b2 = location.clone().add(0, -1, 0);
        Location b3 = location.clone().add(0, -1, -1);
        Location c1 = location.clone().add(1, -1, 1);
        Location c2 = location.clone().add(1, -1, 0);
        Location c3 = location.clone().add(1, -1, -1);
        player.sendBlockChange(a1, Material.DIAMOND_BLOCK, (byte) 0);
        player.sendBlockChange(a2, Material.DIAMOND_BLOCK, (byte) 0);
        player.sendBlockChange(a3, Material.DIAMOND_BLOCK, (byte) 0);
        player.sendBlockChange(b1, Material.DIAMOND_BLOCK, (byte) 0);
        player.sendBlockChange(b2, Material.DIAMOND_BLOCK, (byte) 0);
        player.sendBlockChange(b3, Material.DIAMOND_BLOCK, (byte) 0);
        player.sendBlockChange(c1, Material.DIAMOND_BLOCK, (byte) 0);
        player.sendBlockChange(c2, Material.DIAMOND_BLOCK, (byte) 0);
        player.sendBlockChange(c3, Material.DIAMOND_BLOCK, (byte) 0);

        player.sendBlockChange(location, Material.BEACON, (byte) 3);
    }
}
