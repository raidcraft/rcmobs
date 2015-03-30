package de.raidcraft.mobs;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.NestedCommand;
import de.raidcraft.api.BasePlugin;
import de.raidcraft.api.action.ActionAPI;
import de.raidcraft.api.config.ConfigurationBase;
import de.raidcraft.api.config.Setting;
import de.raidcraft.api.mobs.Mobs;
import de.raidcraft.api.quests.QuestConfigLoader;
import de.raidcraft.api.quests.Quests;
import de.raidcraft.mobs.actions.GroupRemoveAction;
import de.raidcraft.mobs.actions.GroupSpawnAction;
import de.raidcraft.mobs.actions.MobRemoveAction;
import de.raidcraft.mobs.actions.MobSpawnAction;
import de.raidcraft.mobs.commands.MobCommands;
import de.raidcraft.mobs.listener.MobListener;
import de.raidcraft.mobs.requirements.MobKillRequirement;
import de.raidcraft.mobs.tables.TMobGroupSpawnLocation;
import de.raidcraft.mobs.tables.TMobSpawnLocation;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.mobs.tables.TSpawnedMobGroup;
import de.raidcraft.mobs.trigger.MobGroupTrigger;
import de.raidcraft.mobs.trigger.MobTrigger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
// TODO: why implements Listener?
public class MobsPlugin extends BasePlugin {

    private MobManager mobManager;
    private LocalConfiguration configuration;

    @Override
    public void enable() {

        configuration = configure(new LocalConfiguration(this));
        registerCommands(BaseCommands.class);
        this.mobManager = new MobManager(this);

        Bukkit.getScheduler().runTaskLater(this, () -> registerEvents(new MobListener(this)), 5L);

        registerActionAPI();
        registerQuestConfigLoader();

        // register our custom NMS entity
        // TODO: enable with 1.8.3
        // EntityUtil.registerEntity(EntityType.SKELETON, ReflectionUtil.getNmsClass("de.raidcraft.mobs.entites.nms", "RCSkeleton"));
    }

    @Override
    public void disable() {

    }

    public void reload() {

        this.mobManager.reload();
    }

    private void registerActionAPI() {

        ActionAPI.register(this)
                .trigger(new MobTrigger())
                .trigger(new MobGroupTrigger())
                .requirement("mob.kill", new MobKillRequirement())
                .action("mob.spawn", new MobSpawnAction())
                .action("mob.remove", new MobRemoveAction())
                .action("group.spawn", new GroupSpawnAction())
                .action("group.remove", new GroupRemoveAction());
    }

    private void registerQuestConfigLoader() {

        // register mob config loader
        Quests.registerQuestLoader(new QuestConfigLoader("mob") {
            @Override
            public void loadConfig(String id, ConfigurationSection config) {

                getMobManager().registerMob(id, config);
            }

            @Override
            public String replaceReference(String key) {

                return Mobs.getFriendlyName(key);
            }
        });
        Quests.registerQuestLoader(new QuestConfigLoader("group") {
            @Override
            public void loadConfig(String id, ConfigurationSection config) {
                getMobManager().registerMobGroup(id, config);
            }
        });
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {

        ArrayList<Class<?>> tables = new ArrayList<>();
        tables.add(TMobGroupSpawnLocation.class);
        tables.add(TMobSpawnLocation.class);
        tables.add(TSpawnedMob.class);
        tables.add(TSpawnedMobGroup.class);
        return tables;
    }

    public MobManager getMobManager() {

        return mobManager;
    }

    public LocalConfiguration getConfiguration() {

        return configuration;
    }

    public static class LocalConfiguration extends ConfigurationBase<MobsPlugin> {

        @Setting("default.deny-horse-spawning")
        public boolean denyHorseSpawning = false;
        @Setting("default.spawn-deny-radius")
        public int defaultSpawnDenyRadius = 50;
        @Setting("default.task-interval")
        public double spawnTaskInterval = 5.0;
        @Setting("default.reset-range")
        public int resetRange = 50;
        @Setting("default.spawn-similiar-random-mobs")
        public boolean spawnSimiliarRandomMobs = false;
        @Setting("default.replace-hostile-mobs")
        public boolean replaceHostileMobs = false;
        @Setting("default.replace-animals")
        public boolean replaceAnimals = false;
        @Setting("default.natural-spawning-adapt-radius")
        public int naturalAdaptRadius = 25;
        @Setting("default.replaced-mobs")
        public String[] replacedMobs = {
                "BLAZE",
                "ZOMBIE",
                "SKELETON",
                "CREEPER",
                "SPIDER",
                "GIANT",
                "SLIME",
                "GHAST",
                "PIG_ZOMBIE",
                "ENDERMAN",
                "CAVE_SPIDER",
                "SILVERFISH",
                "MAGMA_CUBE",
                "WITCH",
                "IRON_GOLEM"
        };

        public LocalConfiguration(MobsPlugin plugin) {

            super(plugin, "config.yml");
        }
    }

    public class BaseCommands {

        @Command(
                aliases = {"rcm", "de/raidcraft/mobs", "mob"},
                desc = "Base command for the mobs plugin."
        )
        @NestedCommand(MobCommands.class)
        public void mobs(CommandContext args, CommandSender sender) {


        }
    }
}