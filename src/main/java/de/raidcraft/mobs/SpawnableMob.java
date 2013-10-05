package de.raidcraft.mobs;

import de.raidcraft.RaidCraft;
import de.raidcraft.mobs.api.Mob;
import de.raidcraft.mobs.api.Spawnable;
import de.raidcraft.mobs.creatures.ConfigurableCreature;
import de.raidcraft.skills.CharacterManager;
import de.raidcraft.skills.SkillsPlugin;
import de.raidcraft.skills.api.character.CharacterTemplate;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Silthus
 */
public class SpawnableMob implements Spawnable {

    private final String id;
    private final String mobName;
    private final Class<? extends Mob> mClass = ConfigurableCreature.class;
    private final EntityType type;
    private final ConfigurationSection config;
    private double spawnChance = 1.0;

    public SpawnableMob(String id, String mobName, EntityType type, ConfigurationSection config) {

        this.id = id;
        this.mobName = mobName;
        this.type = type;
        this.config = config;
    }

    public String getId() {

        return id;
    }

    public String getMobName() {

        return mobName;
    }

    public Class<? extends Mob> getmClass() {

        return mClass;
    }

    public EntityType getType() {

        return type;
    }

    public ConfigurationSection getConfig() {

        return config;
    }

    public double getSpawnChance() {

        return spawnChance;
    }

    public void setSpawnChance(double spawnChance) {

        this.spawnChance = spawnChance;
    }

    public List<CharacterTemplate> spawn(Location location) {

        ArrayList<CharacterTemplate> characterTemplates = new ArrayList<>();
        characterTemplates.add(spawn(location, true));
        return characterTemplates;
    }

    public CharacterTemplate spawn(Location location, boolean force) {

        CharacterManager manager = RaidCraft.getComponent(SkillsPlugin.class).getCharacterManager();
        // spawn is not forced so we calculate the spawn chance
        if (!force && getSpawnChance() < 1.0) {
            if (Math.random() > getSpawnChance()) {
                return null;
            }
        }
        Mob mob = manager.spawnCharacter(type, location, mClass, config);
        mob.getEntity().setMetadata("RC_MOB_ID", new FixedMetadataValue(RaidCraft.getComponent(MobsPlugin.class), getId()));
        return mob;
    }

    @Override
    public String toString() {

        return mobName;
    }
}
