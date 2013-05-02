package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.api.Component;
import de.raidcraft.api.config.SimpleConfiguration;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.skills.util.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.HashMap;
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

        if (directory == null) {
            return;
        }
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                load(file);
            }
            if (!file.getName().endsWith(".yml")) {
                continue;
            }
            SimpleConfiguration<MobsPlugin> config = new SimpleConfiguration<>(plugin, file);
            EntityType type = EntityType.fromName(config.getString("type"));
            if (type == null) {
                plugin.getLogger().warning("Unknown entity type " + config.getString("type") + " in mob config: " + file.getName());
                continue;
            }
            SpawnableMob mob = new SpawnableMob(config.getString("name", file.getName()), type, config);
            mobs.put(StringUtils.formatName(mob.getMobName()), mob);
        }
    }

    protected void reload() {

        mobs.clear();
        load(baseDir);
    }

    public Mob spawnMob(String name, Location location) throws UnknownMobException {

        name = StringUtils.formatName(name);
        if (!mobs.containsKey(name)) {
            throw new UnknownMobException("No mob with the name " + name + " found!");
        }
        return mobs.get(name).spawn(location);
    }
}
