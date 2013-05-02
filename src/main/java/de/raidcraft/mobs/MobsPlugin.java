package de.raidcraft.mobs;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.NestedCommand;
import de.raidcraft.api.BasePlugin;
import de.raidcraft.mobs.commands.MobCommands;
import org.bukkit.command.CommandSender;

/**
 * @author Silthus
 */
public class MobsPlugin extends BasePlugin {

    private MobManager mobManager;

    @Override
    public void enable() {

        registerCommands(BaseCommands.class);
        this.mobManager = new MobManager(this);
    }

    @Override
    public void disable() {


    }

    public void reload() {

        this.mobManager.reload();
    }

    public MobManager getMobManager() {

        return mobManager;
    }

    public class BaseCommands {

        @Command(
                aliases = {"rcm", "mobs", "mob"},
                desc = "Base command for the mobs plugin."
        )
        @NestedCommand(MobCommands.class)
        public void mobs(CommandContext args, CommandSender sender) {


        }
    }
}
