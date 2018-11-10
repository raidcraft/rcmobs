package de.raidcraft.mobs;

import com.sk89q.util.StringUtil;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.Component;
import de.raidcraft.api.config.SimpleConfiguration;
import de.raidcraft.api.mobs.MobProvider;
import de.raidcraft.api.mobs.Mobs;
import de.raidcraft.mobs.api.*;
import de.raidcraft.mobs.creatures.YamlMobConfig;
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
    private final Map<String, TMobSpawnLocation> delayedMobs = new HashMap<>();
    private final Map<String, TMobGroupSpawnLocation> delayedMobGroups = new HashMap<>();

    protected MobManager(MobsPlugin plugin) {

        this.plugin = plugin;
        RaidCraft.registerComponent(MobManager.class, this);
        Mobs.enable(this);

        baseDir = new File(plugin.getDataFolder(), "mobs");
        baseDir.mkdirs();
        load();
        startRespawnTask();
        // walk mobs to their spawnpoint
        // causes lag?
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
                    for (TSpawnedMob spawnedMob : plugin.getRcDatabase().find(TSpawnedMob.class).where().eq("world", world.getName()).findList()) {
                        CharacterTemplate character = characterManager.getCharacter(spawnedMob.getUuid());
                        if (character == null || !(character instanceof Mob)) {
                            if (!spawnedMob.isUnloaded()) {
                                toDelete.add(spawnedMob);
                            }
                        }
                    }
                }
                plugin.getRcDatabase().deleteAll(toDelete);
                plugin.getRcDatabase().find(TSpawnedMobGroup.class).findList().stream()
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
        long delay = TimeUtil.secondsToTicks(plugin.getConfiguration().respawnTaskDelay);
        this.respawnTask = new RespawnTask(plugin, spawnableMobs, spawnableGroups);
        respawnTask.runTaskTimer(plugin, delay, time);
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

    protected void unload() {
        respawnTask.cancel();
        respawnTask = null;

        for (World world : plugin.getServer().getWorlds()) {
            plugin.getLogger().info("Despawning all mobs in " + world.getName() + "...");
            despawnMobs(world.getLoadedChunks());
        }

        mobs.clear();
        groups.clear();
        spawnableMobs = new MobSpawnLocation[0];
        spawnableGroups = new MobGroupSpawnLocation[0];
        queuedGroups.clear();
        virtualGroups.clear();
        delayedMobs.clear();
        delayedMobGroups.clear();
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

        unload();
        load();
        startRespawnTask();
    }

    private SpawnableMob registerAndReturnMob(String mobId, ConfigurationSection config) {

        EntityType type;
        try {
            type = EntityType.valueOf(config.getString("type").toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().warning("Unknown entity type " + config.getString("type") + " in mob config: " + config.getName());
            return null;
        }
        SpawnableMob mob = new SpawnableMob(mobId, config.getString("name", mobId), type, new YamlMobConfig(config));
        mobs.put(mobId, mob);
        loadedMobs++;
        TMobSpawnLocation spawnLocation = delayedMobs.remove(mobId);
        if (spawnLocation != null) {
            createMobSpawnLocation(spawnLocation).ifPresent(mobSpawnLocation -> {
                spawnableMobs = Arrays.copyOf(spawnableMobs, spawnableMobs.length + 1);
                spawnableMobs[spawnableMobs.length - 1] = mobSpawnLocation;
                respawnTask.updateMobSpawnLocation(spawnableMobs);
                plugin.getLogger().info("Loaded queued mob: " + mobId);
            });
        }
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
        TMobGroupSpawnLocation spawnLocation = delayedMobGroups.remove(id);
        if (spawnLocation != null) {
            createMobGroupSpawnLocation(spawnLocation).ifPresent(mobSpawnLocation -> {
                spawnableGroups = Arrays.copyOf(spawnableGroups, spawnableGroups.length + 1);
                spawnableGroups[spawnableGroups.length - 1] = mobSpawnLocation;
                respawnTask.updateMobGroupSpawnLocation(spawnableGroups);
                plugin.getLogger().info("Loaded queued mob group: " + id);
            });
        }
    }

    private void loadGroups() {

        for (Map.Entry<String, ConfigurationSection> entry : queuedGroups.entrySet()) {
            registerMobGroup(entry.getKey(), entry.getValue());
        }
    }

    private void loadSpawnLocations() {

        List<MobSpawnLocation> mobSpawnLocations = new ArrayList<>();
        // lets load single spawn locations first
        List<TMobSpawnLocation> mobSpawnLocationList = plugin.getRcDatabase().find(TMobSpawnLocation.class).findList();
        for (TMobSpawnLocation location : mobSpawnLocationList) {
            if (plugin.getServer().getWorld(location.getWorld()) == null) {
                continue;
            }
            createMobSpawnLocation(location).ifPresent(mobSpawnLocations::add);
        }
        spawnableMobs = mobSpawnLocations.toArray(new MobSpawnLocation[0]);
        plugin.getLogger().info("Loaded " + spawnableMobs.length);

        List<MobGroupSpawnLocation> mobGroupSpawnLocations = new ArrayList<>();
        // and now load the group spawn locations
        List<TMobGroupSpawnLocation> mobGroupSpawnLocationList = plugin.getRcDatabase().find(TMobGroupSpawnLocation.class).findList();
        for (TMobGroupSpawnLocation location : mobGroupSpawnLocationList) {
            if (plugin.getServer().getWorld(location.getWorld()) == null) {
                continue;
            }
            createMobGroupSpawnLocation(location).ifPresent(mobGroupSpawnLocations::add);
        }
        spawnableGroups = mobGroupSpawnLocations.toArray(new MobGroupSpawnLocation[0]);
    }

    private Optional<MobSpawnLocation> createMobSpawnLocation(TMobSpawnLocation mobLocation) {
        try {
            SpawnableMob mob = getSpwanableMob(mobLocation.getMob());
            if (mob != null) {
                if (mobLocation.getChunkX() == 0 || mobLocation.getChunkZ() == 0) {
                    Location bukkitLocation = mobLocation.getBukkitLocation();
                    mobLocation.setChunkX(bukkitLocation.getChunk().getX());
                    mobLocation.setChunkZ(bukkitLocation.getChunk().getZ());
                    plugin.getRcDatabase().update(mobLocation);
                }
                return Optional.of(new MobSpawnLocation(mobLocation, mob));
            } else {
                throw new UnknownMobException("No mob " + mobLocation.getMob() + " for spawn location " + mobLocation.getBukkitLocation() + " found!");
            }
        } catch (UnknownMobException e) {
            plugin.getLogger().warning(e.getMessage() + " Queueing mob for delayed loading...");
            delayedMobs.put(mobLocation.getMob(), mobLocation);
        }
        return Optional.empty();
    }

    private Optional<MobGroupSpawnLocation> createMobGroupSpawnLocation(TMobGroupSpawnLocation location) {
        try {
            MobGroup mobGroup = getMobGroup(location.getSpawnGroup());
            if (mobGroup != null) {
                if (location.getChunkX() == 0 || location.getChunkZ() == 0) {
                    Location bukkitLocation = location.getBukkitLocation();
                    location.setChunkX(bukkitLocation.getChunk().getX());
                    location.setChunkZ(bukkitLocation.getChunk().getZ());
                    plugin.getRcDatabase().update(location);
                }
                return Optional.of(new MobGroupSpawnLocation(location, mobGroup));
            } else {
                throw new UnknownMobException("No mob group " + location.getSpawnGroup() + " for spawn location " + location.getBukkitLocation() + " found!");
            }
        } catch (UnknownMobException e) {
            plugin.getLogger().warning(e.getMessage() + " Queueing mob group for delayed loading...");
            delayedMobGroups.put(location.getSpawnGroup(), location);
        }
        return Optional.empty();
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

        return plugin.getRcDatabase().find(TSpawnedMob.class).where().eq("uuid", entity.getUniqueId()).findOne();
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

        return plugin.getRcDatabase().find(TSpawnedMob.class).where()
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

    public void despawnMobs(Chunk[] chunks) {
        int count = 0;
        for (Chunk chunk : chunks) {
            count += despawnMob(chunk);
        }
        plugin.getLogger().info("Despawned " + count + " mobs in " + chunks.length + " chunks.");
    }

    public int despawnMob(Chunk chunk) {
        List<TSpawnedMob> despawnedMobs = Arrays.stream(chunk.getEntities())
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> despawnMob((LivingEntity) entity, false).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        plugin.getRcDatabase().saveAll(despawnedMobs);
        return despawnedMobs.size();
    }

    public Optional<TSpawnedMob> despawnMob(LivingEntity entity) {
        return despawnMob(entity, true);
    }

    public Optional<TSpawnedMob> despawnMob(LivingEntity entity, boolean saveToDb) {
        TSpawnedMob spawnedMob = plugin.getMobManager().getSpawnedMob(entity);
        if (spawnedMob != null) {
            spawnedMob.setUnloaded(true);
            Location location = entity.getLocation();
            spawnedMob.setChunkX(location.getChunk().getX());
            spawnedMob.setChunkZ(location.getChunk().getZ());
            spawnedMob.setWorld(location.getWorld().getName());
            spawnedMob.setX(location.getBlockX());
            spawnedMob.setY(location.getBlockY());
            spawnedMob.setZ(location.getBlockZ());
            if (saveToDb) plugin.getRcDatabase().save(spawnedMob);
            if (plugin.getConfiguration().respawnTaskRemoveEntityOnChunkUnload) entity.remove();
        }
        return Optional.ofNullable(spawnedMob);
    }

    public boolean isAllowedNaturalSpawn(Location location) {

        return plugin.getConfiguration().defaultSpawnDenyRadius < 0
                || (getMobSpawnLocations(location, plugin.getConfiguration().defaultSpawnDenyRadius).isEmpty()
                && getGroupSpawnLocations(location, plugin.getConfiguration().defaultSpawnDenyRadius).isEmpty());
    }
}