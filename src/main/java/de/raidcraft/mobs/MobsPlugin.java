package de.raidcraft.mobs;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.NestedCommand;
import de.raidcraft.api.BasePlugin;
import de.raidcraft.mobs.commands.MobCommands;
import de.raidcraft.util.MathUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.List;

/**
 * @author Silthus
 */
public class MobsPlugin extends BasePlugin implements Listener {

    private MobManager mobManager;

    @Override
    public void enable() {

        registerCommands(BaseCommands.class);
        this.mobManager = new MobManager(this);
        registerEvents(this);
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onSpawn(CreatureSpawnEvent event) {

        List<SpawnableMob> mobs = getMobManager().getNaturallySpawningMobs();
        SpawnableMob mob = mobs.get(MathUtil.RANDOM.nextInt(mobs.size()));
        if (mob.spawn(event)) {
            event.getLocation().getWorld().strikeLightningEffect(event.getLocation());
            getLogger().info("Spawned custom mob naturally: " + mob.getMobName());
        }
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
