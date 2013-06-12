package de.raidcraft.mobs;

import com.sk89q.util.StringUtil;
import de.raidcraft.RaidCraft;
import de.raidcraft.api.Component;
import de.raidcraft.api.config.SimpleConfiguration;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.util.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Silthus
 */
public final class MobManager implements Component {

    private final MobsPlugin plugin;
    private final File baseDir;
    private final Map<String, SpawnableMob> mobs = new HashMap<>();

    protected MobManager(MobsPlugin plugin) {

        this.plugin = plugin;
        baseDir = new File(plugin.getDataFolder(), "mobs");
        baseDir.mkdirs();
        load(baseDir);
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

    public Mob spawnMob(String name, Location location) throws UnknownMobException {

        return getSpwanableMob(name).spawn(location);
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
}
