package de.raidcraft.mobs;

import com.comphenix.protocol.ProtocolLibrary;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.NestedCommand;
import de.raidcraft.RaidCraft;
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
import de.raidcraft.mobs.api.MobConstants;
import de.raidcraft.mobs.commands.MobCommands;
import de.raidcraft.mobs.listener.MobListener;
import de.raidcraft.mobs.listener.PacketListener;
import de.raidcraft.mobs.requirements.MobKillRequirement;
import de.raidcraft.mobs.skills.Summon;
import de.raidcraft.mobs.tables.TMobGroupSpawnLocation;
import de.raidcraft.mobs.tables.TMobPlayerKillLog;
import de.raidcraft.mobs.tables.TMobSpawnLocation;
import de.raidcraft.mobs.tables.TPlayerMobKillLog;
import de.raidcraft.mobs.tables.TPlayerPlayerKillLog;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.mobs.tables.TSpawnedMobGroup;
import de.raidcraft.mobs.trigger.MobGroupTrigger;
import de.raidcraft.mobs.trigger.MobTrigger;
import de.raidcraft.skills.SkillsPlugin;
import de.raidcraft.skills.api.exceptions.UnknownSkillException;
import de.raidcraft.util.EntityUtil;
import de.raidcraft.util.ReflectionUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        Bukkit.getScheduler().runTaskLater(this, () -> {
            registerEvents(new MobListener(this));
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketListener(this));
        }, 5L);

        registerActionAPI();
        registerQuestConfigLoader();

        try {
            RaidCraft.getComponent(SkillsPlugin.class).getSkillManager().registerClass(Summon.class);
        } catch (UnknownSkillException e) {
            e.printStackTrace();
        }

        // register our custom NMS entity
        Class<?> rcSkeleton = ReflectionUtil.getNmsClass(MobConstants.NMS_PACKAGE, "RCSkeleton");
        Class<?> rcZombie = ReflectionUtil.getNmsClass(MobConstants.NMS_PACKAGE, "RCZombie");
        if (rcSkeleton != null) EntityUtil.registerEntity(EntityType.SKELETON, rcSkeleton);
        if (rcZombie != null) EntityUtil.registerEntity(EntityType.ZOMBIE, rcZombie);
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
        tables.add(TPlayerMobKillLog.class);
        tables.add(TMobPlayerKillLog.class);
        tables.add(TPlayerPlayerKillLog.class);
        return tables;
    }

    public MobManager getMobManager() {

        return mobManager;
    }

    public LocalConfiguration getConfiguration() {

        return configuration;
    }

    public static class LocalConfiguration extends ConfigurationBase<MobsPlugin> {

        @Setting("default.prevent-slime-splitting")
        public boolean preventSlimeSplitting = true;
        @Setting("debug.mob-spawning")
        public boolean debugMobSpawning = false;
        @Setting("debug.vanilla-spawning")
        public boolean debugVanillaSpawning = false;
        @Setting("debug.fixed-spawn-locations")
        public boolean debugFixedSpawnLocations = false;
        @Setting("default.deny-horse-spawning")
        public boolean denyHorseSpawning = false;
        @Setting("default.spawn-deny-radius")
        public int defaultSpawnDenyRadius = 50;
        @Setting("default.reset-range")
        public int resetRange = 50;
        @Setting("default.spawn-similiar-random-mobs")
        public boolean spawnSimiliarRandomMobs = false;
        @Setting("default.replace-hostile-mobs")
        public boolean replaceHostileMobs = false;
        @Setting("default.replace-animals")
        public boolean replaceAnimals = false;
        @Setting("default.loot-table")
        public String defaultLoottable = "mobs.default-loottable";
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

        @Setting("default.ignored-entities")
        public String[] ignoredEntities = {
                "ARMOR_STAND"
        };

        @Setting("default.ignored-spawn-reasons")
        public String[] ignoredSpawnReasonList = {
                "CUSTOM"
        };

        @Setting("default.denied-entity-types")
        public String[] deniedEntitiesList = {
                "IRON_GOLEM"
        };

        @Setting("respawn-task.remove-entity-on-chunk-unload")
        public boolean respawnTaskRemoveEntityOnChunkUnload = false;
        @Setting("respawn-task.interval")
        public double respawnTaskInterval = 5.0;
        @Setting("respawn-task.mob-batch-count")
        public int respawnTaskMobBatchCount = 10;
        @Setting("respawn-task.mob-group-batch-count")
        public int respawnTaskMobGroupBatchCount = 5;
        @Setting("respawn-task.cleanup-interval")
        public double respawnTaskCleanupInterval = 30;
        @Setting("respawn-task.cleanup-removed-characters")
        public boolean respawnTaskCleanupRemovedCharacters = false;

        private final HashSet<String> replacedMobsSet;
        @Getter
        private final Set<EntityType> ignoredEntityTypes = new HashSet<>();
        @Getter
        private final Set<EntityType> deniedEntities = new HashSet<>();
        @Getter
        private final Set<CreatureSpawnEvent.SpawnReason> ignoredSpawnReasons = new HashSet<>();

        public LocalConfiguration(MobsPlugin plugin) {

            super(plugin, "config.yml");
            replacedMobsSet = new HashSet<>(Arrays.asList(replacedMobs));
            for (String spawnReason : ignoredSpawnReasonList) {
                ignoredSpawnReasons.add(CreatureSpawnEvent.SpawnReason.valueOf(spawnReason));
            }
            for (String entityType : ignoredEntities) {
                ignoredEntityTypes.add(EntityType.valueOf(entityType));
            }
            for (String entityType : deniedEntitiesList) {
                deniedEntities.add(EntityType.valueOf(entityType));
            }
        }

        public HashSet<String> getReplacedMobs() {

            return replacedMobsSet;
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