package de.raidcraft.mobs;

import com.sk89q.util.StringUtil;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.Component;
import de.raidcraft.api.config.SimpleConfiguration;
import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.skills.util.StringUtils;
import de.raidcraft.util.CaseInsensitiveMap;
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

    protected MobManager(MobsPlugin plugin) {

        this.plugin = plugin;
        baseDir = new File(plugin.getDataFolder(), "mobs");
        baseDir.mkdirs();
        load(baseDir);
        loadGroups();
        RaidCraft.registerComponent(MobManager.class, this);
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
            SimpleConfiguration<MobsPlugin> config = plugin.configure(new SimpleConfiguration<>(plugin, file));
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
            mobs.put(StringUtils.formatName(mob.getMobName()), mob);
            plugin.getLogger().info("Loaded custom mob: " + mob.getMobName());
        }
    }

    private void loadGroups() {

        for (Map.Entry<String, ConfigurationSection> entry : queuedGroups.entrySet()) {
            ConfigurableMobGroup group = new ConfigurableMobGroup(entry.getKey(), entry.getValue());
            groups.put(group.getName(), group);
            plugin.getLogger().info("Loaded mob group: " + group.getName());
        }
    }

    protected void reload() {

        mobs.clear();
        load(baseDir);
    }

    public SpawnableMob getSpwanableMob(String name) throws UnknownMobException {

        name = StringUtils.formatName(name);
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

    public List<SpawnableMob> getNaturallySpawningMobs() {

        ArrayList<SpawnableMob> spawnableMobs = new ArrayList<>();
        for (SpawnableMob mob : mobs.values()) {
            if (mob.isSpawningNaturally()) {
                spawnableMobs.add(mob);
            }
        }
        return spawnableMobs;
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
}
