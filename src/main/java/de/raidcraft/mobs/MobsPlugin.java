package de.raidcraft.mobs;

import com.sk89q.craftbook.bukkit.CircuitCore;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.NestedCommand;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.BasePlugin;
import de.raidcraft.mobs.abilities.Panic;
import de.raidcraft.mobs.abilities.Strike;
import de.raidcraft.mobs.circuits.SpawnCustomCreature;
import de.raidcraft.mobs.circuits.TriggerMobAbility;
import de.raidcraft.mobs.commands.MobCommands;
import de.raidcraft.mobs.tables.TMobSpawn;
import de.raidcraft.mobs.tables.TSpawnGroup;
import de.raidcraft.skills.AbilityManager;
import de.raidcraft.skills.SkillsPlugin;
import de.raidcraft.skills.api.exceptions.UnknownSkillException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
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
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {

                registerAbilities();
                registerCustomICs();
            }
        }, 1L);
    }

    @Override
    public void disable() {


    }

    public void reload() {

        this.mobManager.reload();
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {

        ArrayList<Class<?>> tables = new ArrayList<>();
        tables.add(TSpawnGroup.class);
        tables.add(TMobSpawn.class);
        return tables;
    }

    private void registerAbilities() {

        try {
            AbilityManager abilityManager = RaidCraft.getComponent(SkillsPlugin.class).getAbilityManager();
            abilityManager.registerClass(Panic.class);
            abilityManager.registerClass(Strike.class);
        } catch (UnknownSkillException e) {
            getLogger().warning(e.getMessage());
        }
    }

    private void registerCustomICs() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CraftBook");
        if (plugin != null) {
            CircuitCore craftbook = CircuitCore.inst();
            // lets register all of our ics
            craftbook.registerIC("RCM1200", "cus ent spawner", new SpawnCustomCreature.SpawnCustomCreatureFactory(getServer()), CircuitCore.FAMILY_SISO);
            craftbook.registerIC("RCM0001", "use ability", new TriggerMobAbility.TriggerMobAbilityFactory(getServer()), CircuitCore.FAMILY_SISO);
        }
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
