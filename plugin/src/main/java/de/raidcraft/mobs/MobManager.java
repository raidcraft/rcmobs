package de.raidcraft.mobs;

import com.sk89q.util.StringUtil;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.Component;
import de.raidcraft.api.config.SimpleConfiguration;
import de.raidcraft.api.mobs.CustomNmsEntity;
import de.raidcraft.api.mobs.MobProvider;
import de.raidcraft.api.mobs.Mobs;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.CaseInsensitiveMap;
import de.raidcraft.util.ConfigUtil;
import de.raidcraft.util.EntityUtil;
import de.raidcraft.util.LocationUtil;
import de.raidcraft.util.ReflectionUtil;
import de.raidcraft.util.TimeUtil;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.mobs.groups.ConfigurableMobGroup;
import de.raidcraft.mobs.groups.VirtualMobGroup;
import de.raidcraft.mobs.tables.TMobGroupSpawnLocation;
import de.raidcraft.mobs.tables.TMobSpawnLocation;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.mobs.tables.TSpawnedMobGroup;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
    private MobSpawnLocation[] spawnableMobs = new MobSpawnLocation[0];
    private MobGroupSpawnLocation[] spawnableGroups = new MobGroupSpawnLocation[0];
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
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if (spawnableMobs.length > 0) {
                for (MobSpawnLocation mob : spawnableMobs) {
                    mob.spawn(true);
                }
            }
        }, 10L, time);
        // and the mob group task
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if (spawnableGroups.length > 0) {
                for (MobGroupSpawnLocation group : spawnableGroups) {
                    group.spawn(true);
                }
            }
        }, 20L, time);
        // walk mobs to their spawnpoint
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            CharacterManager manager = RaidCraft.getComponent(CharacterManager.class);
            if (manager == null) return;
            for (World world : Bukkit.getWorlds()) {
                for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class)) {
                    if (!isSpawnedMob(entity)) continue;
                    TSpawnedMob spawnedMob = getSpawnedMob(entity);
                    if (spawnedMob == null || spawnedMob.isUnloaded()) continue;
                    Mob mob = getMob(entity.getUniqueId());
                    if (mob == null) continue;
                    if (spawnedMob.getSpawnLocationSource() != null || spawnedMob.getMobGroupSource() != null) {
                        if (LocationUtil.getBlockDistance(entity.getLocation(), mob.getSpawnLocation()) > RaidCraft.getComponent(MobsPlugin.class).getConfiguration().resetRange) {
                            mob.reset();
                        } else if (!mob.isInCombat()) {
                            EntityUtil.walkToLocation(entity, mob.getSpawnLocation(), 1.15F);
                        }
                    }
                }
            }
        }, 100L, 100L);
    }

    private void load(File directory, String path) {

        if (directory == null || directory.list() == null) {
            return;
        }
        ArrayList<Spawnable> createdMobs = new ArrayList<>();
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                load(file, path + file.getName() + ".");
            }
            if (!file.getName().endsWith(".yml")) {
                continue;
            }
            ConfigurationSection config = plugin.configure(new SimpleConfiguration<>(plugin, file));
            if (file.getName().endsWith(FILE_GROUP_SUFFIX)) {
                config = ConfigUtil.replacePathReferences(config, path);
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
        spawnableMobs = new MobSpawnLocation[0];
        spawnableGroups = new MobGroupSpawnLocation[0];
        queuedGroups.clear();
        load();
    }

    private SpawnableMob registerAndReturnMob(String mobId, ConfigurationSection config) {

        EntityType type = null;
        if (!config.isSet("custom-type")) {
            try {
                type = EntityType.valueOf(config.getString("type").toUpperCase());
            } catch (Exception e) {
                plugin.getLogger().warning("Unknown entity type " + config.getString("type") + " in mob config: " + config.getName());
                return null;
            }
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

        List<MobSpawnLocation> mobSpawnLocations = new ArrayList<>();
        // lets load single spawn locations first
        List<TMobSpawnLocation> mobSpawnLocationList = plugin.getDatabase().find(TMobSpawnLocation.class).findList();
        for (TMobSpawnLocation location : mobSpawnLocationList) {
            if (plugin.getServer().getWorld(location.getWorld()) == null) {
                continue;
            }
            try {
                mobSpawnLocations.add(new MobSpawnLocation(location));
                loadedSpawnLocations++;
            } catch (UnknownMobException e) {
                e.printStackTrace();
            }
        }
        spawnableMobs = mobSpawnLocations.toArray(new MobSpawnLocation[mobSpawnLocations.size()]);

        List<MobGroupSpawnLocation> mobGroupSpawnLocations = new ArrayList<>();
        // and now load the group spawn locations
        List<TMobGroupSpawnLocation> mobGroupSpawnLocationList = plugin.getDatabase().find(TMobGroupSpawnLocation.class).findList();
        for (TMobGroupSpawnLocation location : mobGroupSpawnLocationList) {
            if (plugin.getServer().getWorld(location.getWorld()) == null) {
                continue;
            }
            try {
                mobGroupSpawnLocations.add(new MobGroupSpawnLocation(location));
                loadedSpawnLocations++;
            } catch (UnknownMobException e) {
                e.printStackTrace();
            }
        }
        spawnableGroups = mobGroupSpawnLocations.toArray(new MobGroupSpawnLocation[mobGroupSpawnLocations.size()]);
    }

    public MobSpawnLocation addSpawnLocation(TMobSpawnLocation location) {

        try {
            MobSpawnLocation mob = new MobSpawnLocation(location);
            List<MobSpawnLocation> locations = new ArrayList<>(Arrays.asList(spawnableMobs));
            locations.add(mob);
            spawnableMobs = locations.toArray(new MobSpawnLocation[locations.size()]);
            return mob;
        } catch (UnknownMobException e) {
            e.printStackTrace();
            return null;
        }
    }

    public MobGroupSpawnLocation addSpawnLocation(TMobGroupSpawnLocation location) {

        try {
            MobGroupSpawnLocation group = new MobGroupSpawnLocation(location);
            List<MobGroupSpawnLocation> locations = new ArrayList<>(Arrays.asList(spawnableGroups));
            locations.add(group);
            spawnableGroups = locations.toArray(new MobGroupSpawnLocation[locations.size()]);
            return group;
        } catch (UnknownMobException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isSpawnedMob(LivingEntity entity) {

        return getSpawnedMob(entity) != null;
    }

    public Mob getMob(UUID uuid) {

        CharacterTemplate character = RaidCraft.getComponent(CharacterManager.class).getCharacter(uuid);
        if (character != null && character instanceof Mob) {
            return (Mob) character;
        }
        return null;
    }

    @Nullable
    public TSpawnedMob getSpawnedMob(LivingEntity entity) {

        return plugin.getDatabase().find(TSpawnedMob.class).where().eq("uuid", entity.getUniqueId()).findUnique();
    }

    public Optional<TSpawnedMobGroup> getSpawnedMobGroup(LivingEntity entity) {

        TSpawnedMob spawnedMob = getSpawnedMob(entity);
        if (spawnedMob != null) {
            return Optional.ofNullable(spawnedMob.getMobGroupSource());
        }
        return Optional.empty();
    }

    public SpawnableMob getSpawnableMob(TSpawnedMob spawnedMob) throws UnknownMobException {

        return getSpwanableMob(spawnedMob.getMob());
    }

    public SpawnableMob getSpwanableMob(String name) throws UnknownMobException {

        if (mobs.containsKey(name)) {
            return mobs.get(name);
        }
        List<SpawnableMob> spawnableMobs = mobs.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().endsWith(name)
                        || entry.getValue().getMobName().equalsIgnoreCase(name)
                        || entry.getValue().getMobName().toLowerCase().contains(name))
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
            List<String> mobGroups = groups.entrySet().stream()
                    .filter(entry -> entry.getKey().toLowerCase().endsWith(name)
                            || entry.getValue().getName().equalsIgnoreCase(name)
                            || entry.getValue().getName().toLowerCase().contains(name))
                    .map(Map.Entry::getKey)
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

        SpawnableMob mob = getSpwanableMob(name);
        // spawn mob
        return mob.spawn(location, true).get(0);
    }

    public void removeSpawnLocation(MobSpawnLocation location) {

        List<MobSpawnLocation> newMobs = new ArrayList<>();
        for (MobSpawnLocation spawnableMob : spawnableMobs) {
            if (!spawnableMob.equals(location)) {
                newMobs.add(spawnableMob);
            }
        }
        spawnableMobs = newMobs.toArray(new MobSpawnLocation[newMobs.size()]);
    }

    public void removeSpawnLocation(MobGroupSpawnLocation location) {

        List<MobGroupSpawnLocation> newMobs = new ArrayList<>();
        for (MobGroupSpawnLocation spawnableGroup : spawnableGroups) {
            if (!spawnableGroup.equals(location)) {
                newMobs.add(spawnableGroup);
            }
        }
        spawnableGroups = newMobs.toArray(new MobGroupSpawnLocation[newMobs.size()]);
    }

    public MobSpawnLocation getClosestMobSpawnLocation(Location location, int distance) {

        MobSpawnLocation closest = null;
        for (MobSpawnLocation spawnLocation : spawnableMobs) {
            int blockDistance = LocationUtil.getBlockDistance(location, spawnLocation.getLocation());
            if (blockDistance < distance) {
                closest = spawnLocation;
                distance = blockDistance;
            }
        }
        return closest;
    }

    public MobGroupSpawnLocation getClosestGroupSpawnLocation(Location location, int distance) {

        MobGroupSpawnLocation closest = null;
        for (MobGroupSpawnLocation spawnLocation : spawnableGroups) {
            int blockDistance = LocationUtil.getBlockDistance(location, spawnLocation.getLocation());
            if (blockDistance < distance) {
                closest = spawnLocation;
                distance = blockDistance;
            }
        }
        return closest;
    }

    public List<MobSpawnLocation> getMobSpawnLocations(Location location, int radius) {

        List<MobSpawnLocation> locations = new ArrayList<>();
        for (MobSpawnLocation spawnLocation : spawnableMobs) {
            int blockDistance = LocationUtil.getBlockDistance(location, spawnLocation.getLocation());
            if (blockDistance < radius) {
                locations.add(spawnLocation);
            }
        }
        return locations;
    }

    public List<MobGroupSpawnLocation> getGroupSpawnLocations(Location location, int radius) {

        List<MobGroupSpawnLocation> locations = new ArrayList<>();
        for (MobGroupSpawnLocation spawnLocation : spawnableGroups) {
            int blockDistance = LocationUtil.getBlockDistance(location, spawnLocation.getLocation());
            if (blockDistance < radius) {
                locations.add(spawnLocation);
            }
        }
        return locations;
    }

    public MobSpawnLocation getMobSpawnLocation(int id) {

        for (MobSpawnLocation spawnableMob : spawnableMobs) {
            if (spawnableMob.getId() == id) {
                return spawnableMob;
            }
        }
        return null;
    }

    public MobGroupSpawnLocation getGroupSpawnLocation(int id) {

        for (MobGroupSpawnLocation groupSpawnLocation : spawnableGroups) {
            if (groupSpawnLocation.getId() == id) {
                return groupSpawnLocation;
            }
        }
        return null;
    }

    public List<TSpawnedMob> getSpawnedMobs(Chunk chunk) {

        return plugin.getDatabase().find(TSpawnedMob.class).where()
                .eq("world", chunk.getWorld().getName())
                .eq("chunk_x", chunk.getX())
                .eq("chunk_z", chunk.getZ())
                .findList();
    }

    @Override
    public String getFriendlyName(String id) {

        if (mobs.containsKey(id)) {
            return mobs.get(id).getMobName();
        }
        return "";
    }

    public CustomNmsEntity getCustonNmsEntity(World world, String name) {

        try {
            Class<?> clazz = ReflectionUtil.getNmsClass("de.raidcraft.mobs.entities.nms", name);
            Constructor<?> constructor = clazz.getDeclaredConstructor(World.class);
            constructor.setAccessible(true);
            return (CustomNmsEntity) constructor.newInstance(world);
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}