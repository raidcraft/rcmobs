package de.raidcraft.mobs;

import com.sk89q.util.StringUtil;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.Component;
import de.raidcraft.api.config.SimpleConfiguration;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.mobs.tables.MobGroupSpawnLocation;
import de.raidcraft.mobs.tables.MobSpawnLocation;
import de.raidcraft.util.CaseInsensitiveMap;
import de.raidcraft.util.LocationUtil;
import de.raidcraft.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Silthus
 */
public final class MobManager implements Component {

    private static final String FILE_GROUP_SUFFIX = ".group.yml";

    private final MobsPlugin plugin;
    private final File baseDir;
    private final Map<String, SpawnableMob> mobs = new CaseInsensitiveMap<>();
    private final Map<String, MobGroup> groups = new CaseInsensitiveMap<>();
    private final Map<String, ConfigurationSection> queuedGroups = new CaseInsensitiveMap<>();
    private final List<FixedSpawnLocation> spawnableMobs = new ArrayList<>();

    protected MobManager(MobsPlugin plugin) {

        this.plugin = plugin;
        RaidCraft.registerComponent(MobManager.class, this);

        baseDir = new File(plugin.getDataFolder(), "mobs");
        baseDir.mkdirs();
        load();

        // start the spawn task for the fixed spawn locations
        long time = TimeUtil.secondsToTicks(plugin.getConfiguration().spawnTaskInterval);
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {

                for (FixedSpawnLocation location : spawnableMobs) {
                    location.spawn();
                }
            }
        }, 100L, time);
    }

    private void load(File directory) {

        if (directory == null || directory.list() == null) {
            return;
        }
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                load(file);
            }
            if (!file.getName().endsWith(".yml")) {
                continue;
            }
            SimpleConfiguration<MobsPlugin> config = plugin.configure(new SimpleConfiguration<>(plugin, file), false);
            if (file.getName().endsWith(FILE_GROUP_SUFFIX)) {
                queuedGroups.put(file.getName().replace(FILE_GROUP_SUFFIX, ""), config);
                continue;
            }
            EntityType type = EntityType.fromName(config.getString("type"));
            if (type == null) {
                plugin.getLogger().warning("Unknown entity type " + config.getString("type") + " in mob config: " + file.getName());
                continue;
            }
            SpawnableMob mob = new SpawnableMob(config.getString("name", file.getName()), type, config);
            mobs.put(file.getName().replace(".yml", ""), mob);
            plugin.getLogger().info("Loaded custom mob: " + mob.getMobName());
        }
    }

    private void load() {

        load(baseDir);
        loadGroups();
        loadSpawnLocations();
    }

    protected void reload() {

        mobs.clear();
        groups.clear();
        spawnableMobs.clear();
        queuedGroups.clear();
        load();
    }

    private void loadGroups() {

        for (Map.Entry<String, ConfigurationSection> entry : queuedGroups.entrySet()) {
            ConfigurableMobGroup group = new ConfigurableMobGroup(entry.getKey(), entry.getValue());
            groups.put(group.getName(), group);
            plugin.getLogger().info("Loaded mob group: " + group.getName());
        }
    }

    private void loadSpawnLocations() {

        // lets load single spawn locations first
        for (MobSpawnLocation location : plugin.getDatabase().find(MobSpawnLocation.class).findList()) {
            try {
                SpawnableMob spwanableMob = getSpwanableMob(location.getMob());
                FixedSpawnLocation spawnLocation = addSpawnLocation(
                        spwanableMob,
                        new Location(Bukkit.getWorld(location.getWorld()), location.getX(), location.getY(), location.getZ()),
                        location.getCooldown(),
                        spwanableMob.getConfig().getInt("spawn-radius", 1));
            } catch (UnknownMobException e) {
                plugin.getLogger().warning(e.getMessage());
            }
        }
        // and now load the group spawn locations
        for (MobGroupSpawnLocation location : plugin.getDatabase().find(MobGroupSpawnLocation.class).findList()) {
            try {
                MobGroup mobGroup = getMobGroup(location.getSpawnGroup());
                FixedSpawnLocation spawnLocation = addSpawnLocation(
                        mobGroup,
                        new Location(Bukkit.getWorld(location.getWorld()), location.getX(), location.getY(), location.getZ()),
                        location.getCooldown(),
                        mobGroup.getSpawnRadius()
                );
                spawnLocation.setSpawnTreshhold(mobGroup.getRespawnTreshhold());
            } catch (UnknownMobException e) {
                plugin.getLogger().warning(e.getMessage());
            }
        }
    }

    public FixedSpawnLocation addSpawnLocation(Spawnable spawnable, Location location, double cooldown, int radius) {

        FixedSpawnLocation mob = new FixedSpawnLocation(spawnable, location, cooldown, radius);
        spawnableMobs.add(mob);
        plugin.registerEvents(mob);
        return mob;
    }

    public SpawnableMob getSpwanableMob(String name) throws UnknownMobException {

        if (mobs.containsKey(name)) {
            return mobs.get(name);
        }
        List<SpawnableMob> spawnableMobs = new ArrayList<>();
        for (String mobName : mobs.keySet()) {
            if (mobName.contains(name)) {
                spawnableMobs.add(mobs.get(mobName));
            }
        }
        if (spawnableMobs.isEmpty()) {
            throw new UnknownMobException("Keine Kreatur mit dem Namen " + name + " gefunden!");
        }
        if (spawnableMobs.size() > 1) {
            throw new UnknownMobException("Mehrere Kreaturen mit dem Namen " + name + " gefunden: " +
                    StringUtil.joinString(spawnableMobs, ", ", 0));
        }
        return spawnableMobs.get(0);
    }

    public List<SpawnableMob> getSpawnableMobs() {

        return new ArrayList<>(mobs.values());
    }

    public MobGroup getMobGroup(String name) throws UnknownMobException {

        MobGroup group = groups.get(name);
        if (group == null) {
            throw new UnknownMobException("The group " + name + " does not exist!");
        }
        return group;
    }

    public void spawnMob(String name, Location location) throws UnknownMobException {

        getSpwanableMob(name).spawn(location);
    }

    public List<FixedSpawnLocation> getSpawnLocations() {

        return spawnableMobs;
    }

    public void addSpawnLocation(FixedSpawnLocation location) {

        spawnableMobs.add(location);
    }

    public boolean removeSpawnLocation(FixedSpawnLocation location) {

        return spawnableMobs.remove(location);
    }

    public FixedSpawnLocation getClosestSpawnLocation(Location location, int distance) {

        FixedSpawnLocation closest = null;
        for (FixedSpawnLocation spawnLocation : getSpawnLocations()) {
            int blockDistance = LocationUtil.getBlockDistance(location, spawnLocation.getLocation());
            if (blockDistance < distance) {
                closest = spawnLocation;
                distance = blockDistance;
            }
        }
        return closest;
    }
}
