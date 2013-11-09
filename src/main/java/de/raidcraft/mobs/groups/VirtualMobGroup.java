package de.raidcraft.mobs.groups;

import de.raidcraft.mobs.api.MobGroup;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.skills.api.character.CharacterTemplate;
import de.raidcraft.util.MathUtil;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
public class VirtualMobGroup implements MobGroup {

    private final String name;
    private final List<Spawnable> spawnables;

    public VirtualMobGroup(String name, List<Spawnable> spawnables) {

        this.name = name;
        this.spawnables = spawnables;
    }

    @Override
    public String getName() {

        return name;
    }

    @Override
    public double getSpawnInterval() {

        return -1;
    }

    @Override
    public int getMinSpawnAmount() {

        return 0;
    }

    @Override
    public int getMaxSpawnAmount() {

        return 0;
    }

    @Override
    public int getRespawnTreshhold() {

        return 0;
    }

    @Override
    public boolean isInGroup(Spawnable spawnable) {

        return spawnables.contains(spawnable);
    }

    @Override
    public List<Spawnable> getSpawnables() {

        return spawnables;
    }

    @Override
    public List<CharacterTemplate> spawn(Location location) {

        if (!spawnables.isEmpty()) {
            return spawnables.get(MathUtil.RANDOM.nextInt(spawnables.size())).spawn(location);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof VirtualMobGroup)) return false;

        VirtualMobGroup that = (VirtualMobGroup) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {

        return name.hashCode();
    }
}
