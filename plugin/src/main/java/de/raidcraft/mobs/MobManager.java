package de.raidcraft.mobs;

import com.sk89q.util.StringUtil;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.Component;
import de.raidcraft.api.config.SimpleConfiguration;
import de.raidcraft.api.mobs.MobProvider;
import de.raidcraft.api.mobs.Mobs;
import de.raidcraft.mobs.api.*;
import de.raidcraft.mobs.groups.ConfigurableMobGroup;
import de.raidcraft.mobs.groups.VirtualMobGroup;
import de.raidcraft.mobs.tables.TMobGroupSpawnLocation;
import de.raidcraft.mobs.tables.TMobSpawnLocation;
import de.raidcraft.mobs.tables.TSpawnedMob;
import de.raidcraft.mobs.tables.TSpawnedMobGroup;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.*;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Silthus
 */
public final class MobManager implements Component, MobProvider {

    public static final String FILE_GROUP_SUFFIX = ".group.yml";

    private final MobsPlugin plugin;
    private final File baseDir;
    private final Map<String, SpawnableMob> mobs = new CaseInsensitiveMap<>();
    private final Map<String, MobGroup> groups = new CaseInsensitiveMap<>();
    private final Map<String, ConfigurationSection> queuedGroups = new CaseInsensitiveMap<>();
    private RespawnTask respawnTask;
    private MobSpawnLocation[] spawnableMobs = new MobSpawnLocation[0];
    private MobGroupSpawnLocation[] spawnableGroups = new MobGroupSpawnLocation[0];
    private List<MobGroup> virtualGroups = new ArrayList<>();
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
        startRespawnTask();
        // walk mobs to their spawnpoint
        /*
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
        }, 100L, 100L);*/
        if (plugin.getConfiguration().respawnTaskCleanupInterval > 0) {
            // lets run a mob db purge task for all obsolete entries that were missed
            long ticks = TimeUtil.secondsToTicks(plugin.getConfiguration().respawnTaskCleanupInterval);
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                CharacterManager characterManager = RaidCraft.getComponent(CharacterManager.class);
                List<TSpawnedMob> toDelete = new ArrayList<>();
                for (World world : Bukkit.getServer().getWorlds()) {
                    for (TSpawnedMob spawnedMob : plugin.getDatabase().find(TSpawnedMob.class).where().eq("world", world.getName()).findList()) {
                        CharacterTemplate character = characterManager.getCharacter(spawnedMob.getUuid());
                        if (character == null || !(character instanceof Mob)) {
                            if (!spawnedMob.isUnloaded()) {
                                toDelete.add(spawnedMob);
                            }
                        }
                    }
                }
                plugin.getDatabase().delete(toDelete);
                plugin.getDatabase().find(TSpawnedMobGroup.class).findList().stream()
                        .filter(group -> group.getSpawnedMobs().isEmpty())
                        .forEach(TSpawnedMobGroup::delete);
            }, ticks, ticks);
        }
    }

    public RespawnTask getRespawnTask() {

        return respawnTask;
    }

    private void startRespawnTask() {

        if (respawnTask != null) respawnTask.cancel();
        // start the spawn task for the fixed spawn locations
        long time = TimeUtil.secondsToTicks(plugin.getConfiguration().respawnTaskInterval);
        this.respawnTask = new RespawnTask(plugin, spawnableMobs, spawnableGroups);
        respawnTask.runTaskTimer(plugin, time, time);
    }

    private void load(File directory, String path) {

        if (directory == null || directory.list() == null) {
            return;
        }
        ArrayList<Spawnable> createdMobs = new ArrayList<>();
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                // DO NOT USE . here it will fuck up the YML
                load(file, path + file.getName() + "-");
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
        virtualGroups.add(new VirtualMobGroup(path, createdMobs));
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
        plugin.getLogger().info("Loaded " + loadedSpawnLocations + " spawn locations!");
        plugin.getLogger().info("Loaded " + virtualGroups.size() + " virtual random groups!");
    }

    protected void reload() {

        respawnTask.cancel();
        respawnTask = null;
        mobs.clear();
        groups.clear();
        spawnableMobs = new MobSpawnLocation[0];
        spawnableGroups = new MobGroupSpawnLocation[0];
        queuedGroups.clear();
        virtualGroups.clear();
        load();
        startRespawnTask();
    }

    private SpawnableMob registerAndReturnMob(String mobId, ConfigurationSection config) {

        EntityType type = null;
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

        List<MobSpawnLocation> mobSpawnLocations = new ArrayList<>();
        // lets load single spawn locations first
        List<TMobSpawnLocation> mobSpawnLocationList = plugin.getDatabase().find(TMobSpawnLocation.class).findList();
        for (TMobSpawnLocation location : mobSpawnLocationList) {
            if (plugin.getServer().getWorld(location.getWorld()) == null) {
                continue;
            }
            try {
                SpawnableMob mob = getSpwanableMob(location.getMob());
                if (mob != null) {
                    if (location.getChunkX() == 0 || location.getChunkZ() == 0) {
                        Location bukkitLocation = location.getBukkitLocation();
                        location.setChunkX(bukkitLocation.getChunk().getX());
                        location.setChunkZ(bukkitLocation.getChunk().getZ());
                        plugin.getDatabase().update(location);
                    }
                    mobSpawnLocations.add(new MobSpawnLocation(location, mob));
                    loadedSpawnLocations++;
                } else {
                    plugin.getLogger().warning("No mob " + location.getMob()
                            + " for spawn location " + location.getBukkitLocation() + " found!");
                }
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
                MobGroup mobGroup = getMobGroup(location.getSpawnGroup());
                if (mobGroup != null) {
                    if (location.getChunkX() == 0 || location.getChunkZ() == 0) {
                        Location bukkitLocation = location.getBukkitLocation();
                        location.setChunkX(bukkitLocation.getChunk().getX());
                        location.setChunkZ(bukkitLocation.getChunk().getZ());
                        plugin.getDatabase().update(location);
                    }
                    mobGroupSpawnLocations.add(new MobGroupSpawnLocation(location, mobGroup));
                    loadedSpawnLocations++;
                } else {
                    plugin.getLogger().warning("No mob group " + location.getSpawnGroup()
                            + " for spawn location " + location.getBukkitLocation() + " found!");
                }
            } catch (UnknownMobException e) {
                e.printStackTrace();
            }
        }
        spawnableGroups = mobGroupSpawnLocations.toArray(new MobGroupSpawnLocation[mobGroupSpawnLocations.size()]);
    }

    public MobSpawnLocation addSpawnLocation(TMobSpawnLocation location) {

        try {
            SpawnableMob spwanableMob = getSpwanableMob(location.getMob());
            if (spwanableMob != null) {
                MobSpawnLocation mob = new MobSpawnLocation(location, spwanableMob);
                List<MobSpawnLocation> locations = new ArrayList<>(Arrays.asList(spawnableMobs));
                locations.add(mob);
                spawnableMobs = locations.toArray(new MobSpawnLocation[locations.size()]);
                return mob;
            } else {
                plugin.getLogger().warning("No mob " + location.getMob()
                        + " for spawn location " + location.getBukkitLocation() + " found!");
            }
        } catch (UnknownMobException e) {
            e.printStackTrace();
        }
        return null;
    }

    public MobGroupSpawnLocation addSpawnLocation(TMobGroupSpawnLocation location) {

        try {
            MobGroup mobGroup = getMobGroup(location.getSpawnGroup());
            if (mobGroup != null) {
                MobGroupSpawnLocation group = new MobGroupSpawnLocation(location, mobGroup);
                List<MobGroupSpawnLocation> locations = new ArrayList<>(Arrays.asList(spawnableGroups));
                locations.add(group);
                spawnableGroups = locations.toArray(new MobGroupSpawnLocation[locations.size()]);
                return group;
            } else {
                plugin.getLogger().warning("No mob group " + location.getSpawnGroup()
                        + " for spawn location " + location.getBukkitLocation() + " found!");
            }
        } catch (UnknownMobException e) {
            e.printStackTrace();
        }
        return null;
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
        List<String> spawnableMobs = mobs.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().endsWith(name)
                        || entry.getValue().getMobName().equalsIgnoreCase(name)
                        || entry.getValue().getMobName().toLowerCase().contains(name))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (spawnableMobs.isEmpty()) {
            throw new UnknownMobException("Keine Kreatur mit dem Namen " + name + " gefunden!");
        }
        if (spawnableMobs.size() > 1) {
            throw new UnknownMobException("Mehrere Kreaturen mit dem Namen " + name + " gefunden: " +
                    StringUtil.joinString(spawnableMobs, ", ", 0));
        }
        return mobs.get(spawnableMobs.get(0));
    }

    public List<SpawnableMob> getSpawnableMobs() {

        return new ArrayList<>(mobs.values());
    }

    public List<MobGroup> getVirtualGroups() {

        return virtualGroups;
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
                throw new UnknownMobException("No mob group with the displayName " + name + " found!");
            } else if (mobGroups.size() > 1) {
                throw new UnknownMobException("Multiple mob groups with the displayName " + name + " found: " + String.join(",", mobGroups));
            }
            group = groups.get(mobGroups.get(0));
        }
        return group;
    }

    public void removeSpawnLocation(MobSpawnLocation location) {

        List<MobSpawnLocation> newMobs = new ArrayList<>();
        for (MobSpawnLocation spawnableMob : spawnableMobs) {
            if (!spawnableMob.equals(location)) {
                newMobs.add(spawnableMob);
            }
        }
        spawnableMobs = newMobs.toArray(new MobSpawnLocation[newMobs.size()]);
        startRespawnTask();
    }

    public void removeSpawnLocation(MobGroupSpawnLocation location) {

        List<MobGroupSpawnLocation> newMobs = new ArrayList<>();
        for (MobGroupSpawnLocation spawnableGroup : spawnableGroups) {
            if (!spawnableGroup.equals(location)) {
                newMobs.add(spawnableGroup);
            }
        }
        spawnableGroups = newMobs.toArray(new MobGroupSpawnLocation[newMobs.size()]);
        startRespawnTask();
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

    public Optional<CustomNmsEntity> getCustonNmsEntity(World world, String name) {

        try {
            Class<?> clazz = ReflectionUtil.getNmsClass(MobConstants.NMS_PACKAGE, name);
            if (clazz == null) {
                plugin.getLogger().warning("No Custom NMS entity with Class " + name + " found in " + MobConstants.NMS_PACKAGE);
                return Optional.empty();
            }
            Constructor<?> constructor = clazz.getDeclaredConstructor(World.class);
            constructor.setAccessible(true);
            return Optional.of((CustomNmsEntity) constructor.newInstance(world));
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean isAllowedNaturalSpawn(Location location) {

        return plugin.getConfiguration().defaultSpawnDenyRadius < 0
                || (getMobSpawnLocations(location, plugin.getConfiguration().defaultSpawnDenyRadius).isEmpty()
                && getGroupSpawnLocations(location, plugin.getConfiguration().defaultSpawnDenyRadius).isEmpty());
    }
}