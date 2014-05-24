package de.raidcraft.mobs;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.NestedCommand;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.BasePlugin;
import de.raidcraft.api.config.ConfigurationBase;
import de.raidcraft.api.config.Setting;
import de.raidcraft.api.quests.InvalidTypeException;
import de.raidcraft.api.quests.QuestConfigLoader;
import de.raidcraft.api.quests.QuestException;
import de.raidcraft.api.quests.Quests;
import de.raidcraft.mobs.commands.MobCommands;
import de.raidcraft.mobs.creatures.ConfigurableCreature;
import de.raidcraft.mobs.listener.MobListener;
import de.raidcraft.mobs.quests.MobQuestTrigger;
import de.raidcraft.mobs.tables.TMobGroupSpawnLocation;
import de.raidcraft.mobs.tables.TMobSpawnLocation;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
public class MobsPlugin extends BasePlugin implements Listener {

    private MobManager mobManager;
    private LocalConfiguration configuration;

    @Override
    public void enable() {

        configuration = configure(new LocalConfiguration(this));
        registerCommands(BaseCommands.class);
        this.mobManager = new MobManager(this);
        registerEvents(this);
        Bukkit.getScheduler().runTaskLater(this, () -> new MobListener(this), 5L);

        try {
            Quests.registerTrigger(this, MobQuestTrigger.class);
            // register our quest loader
            Quests.registerQuestLoader(new QuestConfigLoader("mob") {
                @Override
                public void loadConfig(String id, ConfigurationSection config) {

                    getMobManager().registerMob(id, config);
                }
            });
            Quests.registerQuestLoader(new QuestConfigLoader("mobgroup") {
                @Override
                public void loadConfig(String id, ConfigurationSection config) {

                    getMobManager().registerMobGroup(id, config);
                }
            });
        } catch (InvalidTypeException | QuestException e) {
            getLogger().warning(e.getMessage());
        }

        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {

                CharacterManager characterManager = RaidCraft.getComponent(CharacterManager.class);
                if (characterManager == null) return;
                for (World world : Bukkit.getWorlds()) {
                    for (LivingEntity entity : world.getLivingEntities()) {
                        if (entity.hasMetadata("RC_CUSTOM_MOB")) {
                            CharacterTemplate character = characterManager.getCharacter(entity);
                            if (character instanceof ConfigurableCreature) {
                                ((ConfigurableCreature) character).checkSpawnPoint();
                            }
                        }
                    }
                }
            }
        }, 100L, 100L);
    }

    @Override
    public void disable() {

        // on shutdown butcher all of our custom mobs
        for (World world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (entity.hasMetadata("RC_CUSTOM_MOB")) {
                    entity.remove();
                }
            }
        }
    }

    public void reload() {

        this.mobManager.reload();
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {

        ArrayList<Class<?>> tables = new ArrayList<>();
        tables.add(TMobGroupSpawnLocation.class);
        tables.add(TMobSpawnLocation.class);
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
                aliases = {"rcm", "mobs", "mob"},
                desc = "Base command for the mobs plugin."
        )
        @NestedCommand(MobCommands.class)
        public void mobs(CommandContext args, CommandSender sender) {


        }
    }
}