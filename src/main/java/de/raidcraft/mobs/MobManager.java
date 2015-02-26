package de.raidcraft.mobs;

import com.sk89q.util.StringUtil;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.Component;
import de.raidcraft.api.config.SimpleConfiguration;
import de.raidcraft.api.mobs.MobProvider;
import de.raidcraft.api.mobs.Mobs;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.mobs.groups.ConfigurableMobGroup;
import de.raidcraft.mobs.groups.VirtualMobGroup;
import de.raidcraft.mobs.tables.TMobGroupSpawnLocation;
import de.raidcraft.mobs.tables.TMobSpawnLocation;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.CaseInsensitiveMap;
import de.raidcraft.util.LocationUtil;
import de.raidcraft.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Silthus
 */
public final class MobManager implements Component, MobProvider {

    private static final String FILE_GROUP_SUFFIX = ".group.yml";

    private final MobsPlugin plugin;
    private final File baseDir;
    private final Map<String, SpawnableMob> mobs = new CaseInsensitiveMap<>();
    private final Map<String, MobGroup> groups = new CaseInsensitiveMap<>();
    private final Map<String, MobGroup> virtualGroups = new CaseInsensitiveMap<>();
    private final Map<String, ConfigurationSection> queuedGroups = new CaseInsensitiveMap<>();
    private final List<MobSpawnLocation> spawnableMobs = new ArrayList<>();
    private final List<MobGroupSpawnLocation> spawnableGroups = new ArrayList<>();
    private int loadedMobs = 0;
    private int loadedMobGroups = 0;
    private int loadedSpawnLocations = 0;

    protected MobManager(MobsPlugin plugin) {

        this.plugin = plugin;
        RaidCraft.registerComponent(MobManager.class, this);
        Mobs.enable(this);

        baseDir = new File(plugin.getDataFolder(), "mobs");
        baseDir.mkdirs();
        load();

        // start the spawn task for the fixed spawn locations
        long time = TimeUtil.secondsToTicks(plugin.getConfiguration().spawnTaskInterval);
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {

            private static final int STEP_SIZE = 20;
            private int startIndex = 0;

            @Override
            public void run() {
                if(spawnableMobs.size() == 0) return;
                for(int i = startIndex; i < startIndex+STEP_SIZE; i++) {
                    if(i >= spawnableMobs.size()) { startIndex = 0; return; }

                    spawnableMobs.get(i).spawn();
                }
                startIndex += STEP_SIZE;
            }
        }, 20L, time);
    }

    private void load(File directory, String path) {

        if (directory == null || directory.list() == null) {
            return;
        }
        ArrayList<Spawnable> createdMobs = new ArrayList<>();
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                load(file, path + file.getName() + "-");
            }
            if (!file.getName().endsWith(".yml")) {
                continue;
            }
            SimpleConfiguration<MobsPlugin> config = plugin.configure(new SimpleConfiguration<>(plugin, file), false);
            if (file.getName().endsWith(FILE_GROUP_SUFFIX)) {
                queuedGroups.put(path + file.getName().replace(FILE_GROUP_SUFFIX, ""), config);
                continue;
            }
            String mobId = path + file.getName().replace(".yml", "");
            SpawnableMob spawnable = registerAndReturnMob(mobId, config);
            if (spawnable != null && spawnable.isSpawningNaturally()) createdMobs.add(spawnable);
        }
        // lets create a virtual group from the current path and all created mobs
        virtualGroups.put(path, new VirtualMobGroup(path, createdMobs));
    }

    private void load() {

        loadedMobs = 0;
        loadedMobGroups = 0;
        loadedSpawnLocations = 0;
        load(baseDir, "");
        plugin.getLogger().info("Loaded " + loadedMobs + " custom mobs!");
        loadGroups();
        plugin.getLogger().info("Loaded " + loadedMobGroups + " mob groups!");
        loadSpawnLocations();
        plugin.getLogger().info("Loaded " + loadedSpawnLocations + " spawn locations");
    }

    protected void reload() {

        mobs.clear();
        groups.clear();
        spawnableMobs.clear();
        queuedGroups.clear();
        load();
    }

    private SpawnableMob registerAndReturnMob(String mobId, ConfigurationSection config) {

        EntityType type;
        try {
            type = EntityType.valueOf(config.getString("type").toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().warning("Unknown entity type " + config.getString("type") + " in mob config: " + config.getName());
            return null;
        }
        SpawnableMob mob = new SpawnableMob(mobId, config.getString("name", mobId), type, config);
        mobs.put(mobId, mob);
        loadedMobs++;
        return mob;
    }

    @Override
    public void registerMob(String mobId, ConfigurationSection config) {

        registerAndReturnMob(mobId, config);
    }

    @Override
    public void registerMobGroup(String id, ConfigurationSection config) {

        ConfigurableMobGroup group = new ConfigurableMobGroup(id, config);
        groups.put(group.getName(), group);
        loadedMobGroups++;
    }

    private void loadGroups() {

        for (Map.Entry<String, ConfigurationSection> entry : queuedGroups.entrySet()) {
            registerMobGroup(entry.getKey(), entry.getValue());
        }
    }

    private void loadSpawnLocations() {

        // lets load single spawn locations first
        for (TMobSpawnLocation location : plugin.getDatabase().find(TMobSpawnLocation.class).findList()) {
            if (plugin.getServer().getWorld(location.getWorld()) == null) {
                continue;
            }
            addSpawnLocation(location);
            loadedSpawnLocations++;
        }
        // and now load the group spawn locations
        for (TMobGroupSpawnLocation location : plugin.getDatabase().find(TMobGroupSpawnLocation.class).findList()) {
            if (plugin.getServer().getWorld(location.getWorld()) == null) {
                continue;
            }
            addSpawnLocation(location);
            loadedSpawnLocations++;
        }
    }

    public MobSpawnLocation addSpawnLocation(TMobSpawnLocation location) {

        try {
            MobSpawnLocation mob = new MobSpawnLocation(location);
            plugin.registerEvents(mob);
            spawnableMobs.add(mob);
            return mob;
        } catch (UnknownMobException e) {
            e.printStackTrace();
            return null;
        }
    }

    public MobGroupSpawnLocation addSpawnLocation(TMobGroupSpawnLocation location) {

        try {
            MobGroupSpawnLocation group = new MobGroupSpawnLocation(location);
            plugin.registerEvents(group);
            spawnableGroups.add(group);
            return group;
        } catch (UnknownMobException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isSpawnedMob(LivingEntity entity) {

        return plugin.getDatabase().find(TSpawnedMob.class).where().eq("uuid", entity.getUniqueId()).findUnique() != null;
    }

    @Nullable
    public TSpawnedMob getSpawnedMob(LivingEntity entity) {

        return plugin.getDatabase().find(TSpawnedMob.class).where().eq("uuid", entity.getUniqueId()).findUnique();
    }

    public SpawnableMob getSpawnableMob(TSpawnedMob spawnedMob) throws UnknownMobException {

        return getSpwanableMob(spawnedMob.getMob());
    }

    public SpawnableMob getSpwanableMob(String name) throws UnknownMobException {

        if (mobs.containsKey(name)) {
            return mobs.get(name);
        }
        List<SpawnableMob> spawnableMobs = mobs.keySet().stream()
                .filter(mobName -> mobName.contains(name))
                .map(mobs::get)
                .collect(Collectors.toList());
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

    public List<MobGroup> getVirtualGroups() {

        return new ArrayList<>(virtualGroups.values());
    }

    public MobGroup getMobGroup(String name) throws UnknownMobException {

        MobGroup group = groups.get(name);
        if (group == null) {
            // try to loop and find a more unprecise match
            List<String> mobGroups = groups.keySet().stream()
                    .filter(g -> g.toLowerCase().endsWith(name.toLowerCase()))
                    .collect(Collectors.toList());
            if (mobGroups.size() < 1) {
                throw new UnknownMobException("No mob group with the name " + name + " found!");
            } else if (mobGroups.size() > 1) {
                throw new UnknownMobException("Multiple mob groups with the name " + name + " found: " + String.join(",", mobGroups));
            }
            group = groups.get(mobGroups.get(0));
        }
        return group;
    }

    public CharacterTemplate spawnMob(String name, Location location) throws UnknownMobException {

        return getSpwanableMob(name).spawn(location).get(0);
    }

    public List<MobSpawnLocation> getSpawnLocations() {

        return spawnableMobs;
    }

    public void addSpawnLocation(MobSpawnLocation location) {

        spawnableMobs.add(location);
    }

    public boolean removeSpawnLocation(MobSpawnLocation location) {

        return spawnableMobs.remove(location);
    }

    public MobSpawnLocation getClosestSpawnLocation(Location location, int distance) {

        MobSpawnLocation closest = null;
        for (MobSpawnLocation spawnLocation : getSpawnLocations()) {
            int blockDistance = LocationUtil.getBlockDistance(location, spawnLocation.getLocation());
            if (blockDistance < distance) {
                closest = spawnLocation;
                distance = blockDistance;
            }
        }
        return closest;
    }

    @Override
    public String getFriendlyName(String id) {

        if (mobs.containsKey(id)) {
            return mobs.get(id).getMobName();
        }
        return "";
    }
}